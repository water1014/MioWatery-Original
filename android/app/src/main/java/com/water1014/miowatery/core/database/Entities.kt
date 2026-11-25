package com.water1014.miowatery.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The raw legacy JSON is retained during migration so fields not yet understood
 * by the native model are never silently discarded.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val avatar: String?,
    val rawLegacyJson: String,
    val updatedAt: Long
)

@Entity(
    tableName = "personas",
    primaryKeys = ["profileId", "personaId"],
    indices = [Index("profileId"), Index("personaId")]
)
data class PersonaEntity(
    val profileId: String,
    val personaId: String,
    val name: String,
    val avatar: String?,
    val description: String?,
    val rawLegacyJson: String,
    val updatedAt: Long
)

@Entity(
    tableName = "conversations",
    indices = [Index("profileId"), Index("personaId"), Index("updatedAt")]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val personaId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val rawLegacyJson: String = "{}"
)

@Entity(
    tableName = "messages",
    indices = [Index("conversationId"), Index(value = ["conversationId", "position"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val position: Long,
    val role: String,
    val content: String,
    val timestamp: Long?,
    val reasoning: String?,
    val attachmentJson: String?,
    val metadataJson: String?,
    val rawLegacyJson: String
)

@Entity(
    tableName = "worldbook_entries",
    indices = [Index("profileId"), Index("personaId"), Index("bookId")]
)
data class WorldbookEntryEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val personaId: String?,
    val bookId: String,
    val title: String,
    val content: String,
    val enabled: Boolean,
    val rawLegacyJson: String,
    val updatedAt: Long
)

@Entity(tableName = "diary_entries", indices = [Index("entryDate"), Index("updatedAt")])
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val entryDate: String,
    val mood: String,
    val isFavorite: Boolean,
    val isLocked: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)


@Entity(
    tableName = "message_app_entries",
    indices = [Index("profileId"), Index("personaId"), Index("kind"), Index("createdAt")]
)
data class MessageAppEntryEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val personaId: String,
    /** sms, qa, or pass */
    val kind: String,
    val text: String,
    val reply: String,
    val author: String,
    val createdAt: Long
)
