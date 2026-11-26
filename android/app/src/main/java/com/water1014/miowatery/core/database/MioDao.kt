package com.water1014.miowatery.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class RecentMessagePreview(
    val conversationTitle: String,
    val role: String,
    val content: String,
    val timestamp: Long?
)

@Dao
interface MioDao {
    @Query("SELECT * FROM profiles ORDER BY updatedAt DESC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPersonas(personas: List<PersonaEntity>)

    @Query("SELECT * FROM personas WHERE profileId = :profileId ORDER BY name COLLATE NOCASE")
    fun observePersonas(profileId: String): Flow<List<PersonaEntity>>

    @Query("SELECT * FROM personas WHERE profileId = :profileId AND personaId = :personaId LIMIT 1")
    suspend fun getPersona(profileId: String, personaId: String): PersonaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversation(conversationId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE profileId = :profileId AND personaId = :personaId ORDER BY updatedAt DESC")
    fun observeConversations(profileId: String, personaId: String): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY position ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY position ASC")
    suspend fun getMessagesForConversation(conversationId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearMessages(conversationId: String)

    @Query("DELETE FROM messages WHERE id = :messageId AND conversationId = :conversationId")
    suspend fun deleteMessage(messageId: String, conversationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorldbookCategory(category: WorldbookCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorldbookCategories(categories: List<WorldbookCategoryEntity>)

    @Query("SELECT * FROM worldbook_categories WHERE profileId = :profileId AND (personaId IS NULL OR personaId = :personaId) ORDER BY name COLLATE NOCASE")
    fun observeWorldbookCategories(profileId: String, personaId: String): Flow<List<WorldbookCategoryEntity>>

    @Query("SELECT * FROM worldbook_categories WHERE profileId = :profileId ORDER BY name COLLATE NOCASE")
    fun observeAllWorldbookCategories(profileId: String): Flow<List<WorldbookCategoryEntity>>

    @Query("SELECT * FROM worldbook_categories WHERE id = :id LIMIT 1")
    suspend fun getWorldbookCategory(id: String): WorldbookCategoryEntity?

    @Query("SELECT * FROM worldbook_entries WHERE id = :id LIMIT 1")
    suspend fun getWorldbookEntry(id: String): WorldbookEntryEntity?

    @Query("UPDATE worldbook_entries SET bookId = '默认分类', updatedAt = :now WHERE profileId = :profileId AND bookId = :name AND ((personaId IS NULL AND :personaId IS NULL) OR personaId = :personaId)")
    suspend fun moveCategoryEntriesToDefault(profileId: String, personaId: String?, name: String, now: Long)

    @Query("DELETE FROM worldbook_categories WHERE id = :id")
    suspend fun deleteWorldbookCategory(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorldbookEntries(entries: List<WorldbookEntryEntity>)

    @Query("DELETE FROM worldbook_entries WHERE id = :entryId")
    suspend fun deleteWorldbookEntry(entryId: String)

    @Query("SELECT * FROM worldbook_entries WHERE profileId = :profileId AND (personaId IS NULL OR personaId = :personaId) ORDER BY bookId, title COLLATE NOCASE")
    fun observeWorldbookEntries(profileId: String, personaId: String): Flow<List<WorldbookEntryEntity>>

    @Query("SELECT * FROM worldbook_entries WHERE profileId = :profileId ORDER BY bookId, title COLLATE NOCASE")
    fun observeAllWorldbookEntries(profileId: String): Flow<List<WorldbookEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemoryEntry(entry: MemoryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemoryEntries(entries: List<MemoryEntryEntity>)

    @Query("SELECT * FROM memory_entries WHERE conversationId = :conversationId ORDER BY createdAt DESC")
    fun observeMemoryEntries(conversationId: String): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE conversationId = :conversationId AND type = :type ORDER BY createdAt DESC")
    suspend fun getMemoryEntries(conversationId: String, type: String): List<MemoryEntryEntity>

    @Query("DELETE FROM memory_entries WHERE id = :id AND conversationId = :conversationId")
    suspend fun deleteMemoryEntry(id: String, conversationId: String): Int

    @Query("DELETE FROM memory_entries WHERE conversationId = :conversationId AND id IN (:ids)")
    suspend fun deleteMemoryEntries(ids: List<String>, conversationId: String)

    @Transaction
    suspend fun replaceCompressedMemoryNotes(entry: MemoryEntryEntity, sourceIds: List<String>, conversationId: String) {
        upsertMemoryEntry(entry)
        if (sourceIds.isNotEmpty()) deleteMemoryEntries(sourceIds, conversationId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiaryEntry(entry: DiaryEntryEntity)

    @Query("SELECT * FROM diary_entries ORDER BY entryDate DESC, updatedAt DESC")
    fun observeDiaryEntries(): Flow<List<DiaryEntryEntity>>

    @Query("DELETE FROM diary_entries WHERE id = :entryId AND isLocked = 0")
    suspend fun deleteDiaryEntry(entryId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessageAppEntry(entry: MessageAppEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessageAppEntries(entries: List<MessageAppEntryEntity>)

    @Query("SELECT * FROM message_app_entries WHERE profileId = :profileId AND personaId = :personaId ORDER BY createdAt DESC")
    fun observeMessageAppEntries(profileId: String, personaId: String): Flow<List<MessageAppEntryEntity>>

    @Query("DELETE FROM message_app_entries WHERE id = :entryId AND profileId = :profileId")
    suspend fun deleteMessageAppEntry(entryId: String, profileId: String): Int

    @Query("SELECT c.title AS conversationTitle, m.role AS role, m.content AS content, m.timestamp AS timestamp FROM messages AS m INNER JOIN conversations AS c ON c.id = m.conversationId ORDER BY COALESCE(m.timestamp, c.updatedAt) DESC, m.position DESC LIMIT 3")
    fun observeRecentMessagePreviews(): Flow<List<RecentMessagePreview>>

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun messageCount(): Int

    @Query("SELECT * FROM sticker_groups WHERE profileId = :profileId ORDER BY position ASC, name COLLATE NOCASE")
    fun observeStickerGroups(profileId: String): Flow<List<StickerGroupEntity>>

    @Query("SELECT * FROM stickers WHERE profileId = :profileId ORDER BY groupName COLLATE NOCASE, name COLLATE NOCASE")
    fun observeStickers(profileId: String): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers WHERE profileId = :profileId AND groupName IN (:groups) ORDER BY name COLLATE NOCASE")
    suspend fun getStickersInGroups(profileId: String, groups: List<String>): List<StickerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStickerGroups(groups: List<StickerGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStickers(stickers: List<StickerEntity>)

    @Query("DELETE FROM stickers WHERE profileId = :profileId AND groupName = :groupName")
    suspend fun deleteStickersInGroup(profileId: String, groupName: String)

    @Query("DELETE FROM stickers WHERE profileId = :profileId AND id = :stickerId")
    suspend fun deleteSticker(profileId: String, stickerId: String): Int

    @Query("DELETE FROM sticker_groups WHERE profileId = :profileId AND name = :groupName")
    suspend fun deleteStickerGroup(profileId: String, groupName: String)

    @Query("SELECT * FROM wallet_states WHERE profileId = :profileId ORDER BY ownerId")
    fun observeWalletStates(profileId: String): Flow<List<WalletStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWalletState(state: WalletStateEntity)
}
