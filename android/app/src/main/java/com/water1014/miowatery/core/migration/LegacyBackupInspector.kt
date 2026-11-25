package com.water1014.miowatery.core.migration

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class LegacyBackupPreview(
    val sourceFormat: String,
    val jsonFileName: String,
    val jsonBytes: Int,
    val topLevelKeys: List<String>,
    val profileCount: Int,
    val personaCount: Int,
    val worldbookCount: Int,
    val chatOwnerCount: Int,
    val conversationCount: Int,
    val messageCount: Int,
    val warnings: List<String>
)

/**
 * Read-only inspector for legacy JSON and ZIP backups.
 * It never writes to the native database or modifies the source stream.
 */
object LegacyBackupInspector {
    private const val MAX_JSON_BYTES = 100 * 1024 * 1024

    fun inspect(input: InputStream, fileName: String): LegacyBackupPreview {
        val lowerName = fileName.lowercase()
        val isZip = lowerName.endsWith(".zip")
        val bytes: ByteArray
        val jsonName: String

        if (isZip) {
            val entries = ZipInputStream(input).use { zip ->
                var selectedName: String? = null
                var selectedBytes: ByteArray? = null
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.endsWith(".json", ignoreCase = true)) {
                        val buffer = ByteArrayOutputStream()
                        val chunk = ByteArray(8192)
                        while (true) {
                            val read = zip.read(chunk)
                            if (read < 0) break
                            if (buffer.size() + read > MAX_JSON_BYTES) {
                                throw IllegalArgumentException("备份 JSON 超过 100 MB 安全读取上限")
                            }
                            buffer.write(chunk, 0, read)
                        }
                        if (selectedBytes == null || entry.name.endsWith("xiaoshouji-backup.json")) {
                            selectedName = entry.name
                            selectedBytes = buffer.toByteArray()
                        }
                    }
                    zip.closeEntry()
                }
                Pair(selectedName, selectedBytes)
            }
            jsonName = entries.first ?: throw IllegalArgumentException("ZIP 备份中没有找到 JSON 文件")
            bytes = entries.second ?: throw IllegalArgumentException("ZIP 备份 JSON 内容为空")
        } else {
            bytes = input.use { stream ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val read = stream.read(chunk)
                    if (read < 0) break
                    if (buffer.size() + read > MAX_JSON_BYTES) {
                        throw IllegalArgumentException("备份 JSON 超过 100 MB 安全读取上限")
                    }
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            }
            jsonName = fileName
        }

        val root = try {
            JsonParser.parseString(bytes.toString(Charsets.UTF_8))
        } catch (error: Exception) {
            throw IllegalArgumentException("无法解析备份 JSON，请确认文件完整", error)
        }
        if (!root.isJsonObject) throw IllegalArgumentException("备份根节点不是 JSON 对象")

        val state = root.asJsonObject
        val chatsByUser = state.objectOrNull("chatsByUser")
        val legacyChats = state.objectOrNull("chats")
        val chatOwners = chatsByUser ?: if (legacyChats != null) JsonObject().apply {
            add(state.get("activeUserProfile")?.takeIf { it.isJsonPrimitive }?.asString?.ifBlank { "default" } ?: "default", legacyChats)
        } else null
        val worlds = state.arrayOrNull("worlds")
        val personas = state.arrayOrNull("personas")
        val profiles = state.arrayOrNull("userProfiles")
        val chatOwnerCount = chatOwners?.size() ?: 0
        val conversations = chatOwners?.entrySet()?.sumOf { (_, value) ->
            if (value.isJsonObject) value.asJsonObject.size() else 0
        } ?: 0
        val messages = chatOwners?.entrySet()?.sumOf { (_, value) ->
            if (!value.isJsonObject) 0 else value.asJsonObject.entrySet().sumOf { (_, chat) ->
                if (chat.isJsonArray) chat.asJsonArray.size() else 0
            }
        } ?: 0

        val warnings = buildList {
            if (state.has("apiPresets") || state.has("apiSessions")) {
                add("备份可能包含 API 配置或凭据；正式导入时会先检查敏感字段。")
            }
            if (legacyChats != null && chatsByUser == null) {
                add("发现旧式 chats 字段，已按当前用户统计其中的会话和消息。")
            }
            if (profiles == null) add("未发现 userProfiles 数组，不能据此判断是否存在其他用户资料结构。")
            if (personas == null) add("未发现 personas 数组，角色数量显示为 0 不代表没有角色数据。")
            if (worlds == null) add("未发现 worlds 数组，世界书数量显示为 0 不代表没有世界书数据。")
            add("这是只读预览；尚未导入任何数据，也不会覆盖现有内容。")
        }

        return LegacyBackupPreview(
            sourceFormat = if (isZip) "ZIP" else "JSON",
            jsonFileName = jsonName,
            jsonBytes = bytes.size,
            topLevelKeys = state.keySet().sorted(),
            profileCount = profiles?.size() ?: if (state.has("me")) 1 else 0,
            personaCount = personas?.size() ?: 0,
            worldbookCount = worlds?.size() ?: 0,
            chatOwnerCount = chatOwnerCount,
            conversationCount = conversations,
            messageCount = messages,
            warnings = warnings
        )
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.arrayOrNull(key: String) =
        get(key)?.takeIf { it.isJsonArray }?.asJsonArray
}
