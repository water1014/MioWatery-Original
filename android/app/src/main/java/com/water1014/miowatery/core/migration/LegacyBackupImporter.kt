package com.water1014.miowatery.core.migration

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.water1014.miowatery.core.database.ConversationEntity
import com.water1014.miowatery.core.database.MessageEntity
import com.water1014.miowatery.core.database.MioDatabase
import com.water1014.miowatery.core.database.PersonaEntity
import com.water1014.miowatery.core.database.ProfileEntity
import com.water1014.miowatery.core.database.WorldbookEntryEntity
import java.io.File
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.util.Locale

data class LegacyImportResult(
    val profiles: Int,
    val personas: Int,
    val worldbookEntries: Int,
    val conversations: Int,
    val messages: Int,
    val preservedBackupPath: String
)

object LegacyBackupImporter {
    private const val TARGET_PROFILE_ID = "default"

    fun readJson(input: InputStream, fileName: String): String {
        if (!fileName.lowercase(Locale.ROOT).endsWith(".zip")) {
            val bytes = input.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 100 * 1024 * 1024) {
                        throw IllegalArgumentException("备份超过 100 MB 安全读取上限")
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return bytes.toString(Charsets.UTF_8)
        }
        return ZipInputStream(input).use { zip ->
            var selected: ByteArray? = null
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name.endsWith(".json", ignoreCase = true)) {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > 100 * 1024 * 1024) {
                            throw IllegalArgumentException("备份 JSON 超过 100 MB 安全读取上限")
                        }
                        output.write(buffer, 0, count)
                    }
                    val bytes = output.toByteArray()
                    if (selected == null || entry.name.endsWith("xiaoshouji-backup.json")) selected = bytes
                }
                zip.closeEntry()
            }
            (selected ?: throw IllegalArgumentException("ZIP 备份中没有找到 JSON 文件")).toString(Charsets.UTF_8)
        }
    }

    suspend fun importJson(context: Context, jsonText: String, database: MioDatabase): LegacyImportResult {
        require(jsonText.isNotBlank()) { "备份内容为空" }
        require(jsonText.toByteArray(Charsets.UTF_8).size <= 100 * 1024 * 1024) { "备份超过 100 MB 安全导入上限" }
        val root = try {
            JsonParser.parseString(jsonText).asJsonObject
        } catch (error: Exception) {
            throw IllegalArgumentException("备份 JSON 无法解析", error)
        }

        val backupDir = File(context.filesDir, "legacy-backups").apply { mkdirs() }
        val backupFile = File(backupDir, "miowatery-" + System.currentTimeMillis() + ".json")
        backupFile.writeText(jsonText, Charsets.UTF_8)

        val now = System.currentTimeMillis()
        val userProfiles = root.array("userProfiles")?.map { it.asObjectOrEmpty() }.orEmpty()
        val profileRows = if (userProfiles.isNotEmpty()) userProfiles else {
            listOf(root.obj("me") ?: JsonObject().apply { addProperty("name", "我") })
        }
        val activeSourceProfile = root.string("activeUserProfile").ifBlank {
            profileRows.firstOrNull()?.string("id").orEmpty()
        }

        val profileEntities = profileRows.mapIndexed { index, profile ->
            val sourceId = profile.string("id").ifBlank { "profile-" + index }
            ProfileEntity(
                id = "legacy-profile-" + safeId(sourceId),
                displayName = profile.string("name").ifBlank { "用户 " + (index + 1) },
                avatar = profile.stringOrNull("avatar"),
                rawLegacyJson = profile.toString(),
                updatedAt = now
            )
        }

        val personaMap = linkedMapOf<Pair<String, String>, String>()
        val personaEntities = mutableListOf<PersonaEntity>()
        val legacyPersonas = root.array("personas")?.map { it.asObjectOrEmpty() }.orEmpty()
        legacyPersonas.forEachIndexed { index, persona ->
            val sourcePersonaId = persona.string("id").ifBlank { "persona-" + index }
            val sourceProfileId = persona.string("boundUser").ifBlank { activeSourceProfile.ifBlank { "default" } }
            val nativePersonaId = "legacy-" + safeId(sourceProfileId) + "-" + safeId(sourcePersonaId)
            personaMap[sourceProfileId to sourcePersonaId] = nativePersonaId
            personaEntities += PersonaEntity(
                profileId = TARGET_PROFILE_ID,
                personaId = nativePersonaId,
                name = persona.string("name").ifBlank { "未命名角色" },
                avatar = persona.stringOrNull("avatar"),
                description = persona.stringOrNull("bio"),
                rawLegacyJson = persona.toString(),
                updatedAt = now
            )
        }

        val worlds = root.array("worlds")?.map { it.asObjectOrEmpty() }.orEmpty()
        val worldbookRows = mutableListOf<WorldbookEntryEntity>()
        worlds.forEachIndexed { index, world ->
            val sourceId = world.string("id").ifBlank { "world-" + index }
            val scope = world.string("scope").ifBlank { "global" }
            val contacts = world.array("contacts")?.mapNotNull { it.asStringOrNull() }.orEmpty()
            val targets: List<String?> = if (scope == "contact" && contacts.isNotEmpty()) {
                contacts.mapNotNull { contactId ->
                    personaMap[activeSourceProfile to contactId]
                        ?: personaMap.entries.firstOrNull { it.key.second == contactId }?.value
                }.distinct().map { it as String? }
            } else listOf(null)
            targets.forEachIndexed { targetIndex, personaId ->
                worldbookRows += WorldbookEntryEntity(
                    id = "legacy-world-" + safeId(sourceId) + "-" + targetIndex,
                    profileId = TARGET_PROFILE_ID,
                    personaId = personaId,
                    bookId = world.string("cat").ifBlank { "默认分类" },
                    title = world.string("title").ifBlank { "未命名世界书" },
                    content = world.string("content"),
                    enabled = world.boolean("on", true),
                    rawLegacyJson = world.toString(),
                    updatedAt = now
                )
            }
        }

        val conversationRows = mutableListOf<ConversationEntity>()
        val messageRows = mutableListOf<MessageEntity>()
        val chatsByUser = root.obj("chatsByUser")
        val chatOwners = chatsByUser ?: JsonObject().apply {
            val oldChats = root.obj("chats")
            if (oldChats != null) add(activeSourceProfile.ifBlank { "default" }, oldChats)
        }

        chatOwners.entrySet().forEach ownerLoop@ { ownerEntry ->
            val sourceProfileId = ownerEntry.key
            val chats = ownerEntry.value.takeIf { it.isJsonObject }?.asJsonObject ?: return@ownerLoop
            chats.entrySet().forEach chatLoop@ { chatEntry ->
                val sourcePersonaId = chatEntry.key
                val messages = chatEntry.value.takeIf { it.isJsonArray }?.asJsonArray ?: return@chatLoop
                if (messages.size() == 0) return@chatLoop
                val nativePersonaId = personaMap[sourceProfileId to sourcePersonaId]
                    ?: personaMap.entries.firstOrNull { it.key.second == sourcePersonaId }?.value
                    ?: ("legacy-" + safeId(sourceProfileId) + "-" + safeId(sourcePersonaId))
                if (personaEntities.none { it.personaId == nativePersonaId }) {
                    personaEntities += PersonaEntity(
                        profileId = TARGET_PROFILE_ID,
                        personaId = nativePersonaId,
                        name = "导入的角色",
                        avatar = null,
                        description = "从旧版聊天记录恢复",
                        rawLegacyJson = JsonObject().apply { addProperty("legacyPersonaId", sourcePersonaId) }.toString(),
                        updatedAt = now
                    )
                }
                val conversationId = "legacy-chat-" + safeId(sourceProfileId) + "-" + safeId(sourcePersonaId)
                val firstTime = messages.firstOrNull()?.asObjectOrEmpty()?.long("t")?.takeIf { it > 0 } ?: now
                val lastTime = messages.lastOrNull()?.asObjectOrEmpty()?.long("t")?.takeIf { it > 0 } ?: now
                conversationRows += ConversationEntity(
                    id = conversationId,
                    profileId = TARGET_PROFILE_ID,
                    personaId = nativePersonaId,
                    title = "旧版聊天记录",
                    createdAt = firstTime,
                    updatedAt = lastTime.coerceAtLeast(firstTime),
                    rawLegacyJson = JsonObject().apply {
                        addProperty("legacyProfileId", sourceProfileId)
                        addProperty("legacyPersonaId", sourcePersonaId)
                    }.toString()
                )
                messages.forEachIndexed { position, element ->
                    val message = element.asObjectOrEmpty()
                    val role = message.string("role").ifBlank { "assistant" }
                    val messageContent = message.string("content")
                    if (messageContent.isBlank()) return@forEachIndexed
                    messageRows += MessageEntity(
                        id = conversationId + "-" + position,
                        conversationId = conversationId,
                        position = position.toLong(),
                        role = role,
                        content = messageContent,
                        timestamp = message.longOrNull("t"),
                        reasoning = message.stringOrNull("reasoning"),
                        attachmentJson = message.get("attachments")?.takeUnless { it.isJsonNull }?.toString(),
                        metadataJson = message.get("metadata")?.takeUnless { it.isJsonNull }?.toString(),
                        rawLegacyJson = message.toString()
                    )
                }
            }
        }

        val dao = database.mioDao()
        database.withTransaction {
            profileEntities.forEach { dao.upsertProfile(it) }
            dao.upsertPersonas(personaEntities.distinctBy { it.personaId })
            conversationRows.forEach { conversation ->
                dao.upsertConversation(conversation)
                dao.clearMessages(conversation.id)
            }
            if (messageRows.isNotEmpty()) dao.insertMessages(messageRows)
            if (worldbookRows.isNotEmpty()) dao.upsertWorldbookEntries(worldbookRows)
        }

        return LegacyImportResult(
            profiles = profileEntities.size,
            personas = personaEntities.distinctBy { it.personaId }.size,
            worldbookEntries = worldbookRows.size,
            conversations = conversationRows.size,
            messages = messageRows.size,
            preservedBackupPath = backupFile.relativeTo(context.filesDir).path
        )
    }

    private fun safeId(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9_-]"), "_")
        .take(64)
        .ifBlank { "item" }

    private fun JsonObject.obj(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.array(key: String): JsonArray? =
        get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.string(key: String): String =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)?.takeUnless { it.isJsonNull }?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.boolean(key: String, fallback: Boolean): Boolean =
        get(key)?.takeIf { it.isJsonPrimitive }?.asBoolean ?: fallback

    private fun JsonObject.long(key: String): Long =
        get(key)?.takeIf { it.isJsonPrimitive }?.asLong ?: 0L

    private fun JsonObject.longOrNull(key: String): Long? =
        get(key)?.takeUnless { it.isJsonNull }?.takeIf { it.isJsonPrimitive }?.asLong

    private fun JsonElement.asObjectOrEmpty(): JsonObject =
        takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()

    private fun JsonElement.asStringOrNull(): String? =
        takeIf { it.isJsonPrimitive }?.asString
}
