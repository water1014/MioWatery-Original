package com.water1014.miowatery.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearMessages(conversationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorldbookEntries(entries: List<WorldbookEntryEntity>)

    @Query("SELECT * FROM worldbook_entries WHERE profileId = :profileId AND (personaId IS NULL OR personaId = :personaId) ORDER BY bookId, title COLLATE NOCASE")
    fun observeWorldbookEntries(profileId: String, personaId: String): Flow<List<WorldbookEntryEntity>>

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun messageCount(): Int
}
