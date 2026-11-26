package com.water1014.miowatery.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        PersonaEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        WorldbookEntryEntity::class,
        DiaryEntryEntity::class,
        MessageAppEntryEntity::class,
        MemoryEntryEntity::class,
        WorldbookCategoryEntity::class,
        StickerGroupEntity::class,
        StickerEntity::class,
        WalletStateEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class MioDatabase : RoomDatabase() {
    abstract fun mioDao(): MioDao

    companion object {
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS diary_entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        entryDate TEXT NOT NULL,
                        mood TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL,
                        isLocked INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_entryDate ON diary_entries(entryDate)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_updatedAt ON diary_entries(updatedAt)")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS message_app_entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        profileId TEXT NOT NULL,
                        personaId TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        text TEXT NOT NULL,
                        reply TEXT NOT NULL,
                        author TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_message_app_entries_profileId ON message_app_entries(profileId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_message_app_entries_personaId ON message_app_entries(personaId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_message_app_entries_kind ON message_app_entries(kind)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_message_app_entries_createdAt ON message_app_entries(createdAt)")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS memory_entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        conversationId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        text TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        rangeStart INTEGER,
                        rangeEnd INTEGER,
                        isMerged INTEGER NOT NULL
                    )""".trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_entries_conversationId ON memory_entries(conversationId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_entries_type ON memory_entries(type)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_memory_entries_createdAt ON memory_entries(createdAt)")
            }
        }


        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS worldbook_categories (
                        id TEXT NOT NULL PRIMARY KEY,
                        profileId TEXT NOT NULL,
                        personaId TEXT,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_worldbook_categories_profileId ON worldbook_categories(profileId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_worldbook_categories_personaId ON worldbook_categories(personaId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_worldbook_categories_name ON worldbook_categories(name)")
                database.execSQL(
                    """INSERT OR IGNORE INTO worldbook_categories (id, profileId, personaId, name, createdAt, updatedAt)
                       SELECT 'legacy-' || profileId || '-' || COALESCE(personaId, 'global') || '-' || bookId,
                              profileId, personaId, bookId, MIN(updatedAt), MAX(updatedAt)
                       FROM worldbook_entries GROUP BY profileId, personaId, bookId""".trimIndent()
                )
            }
        }



        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS sticker_groups (
                        profileId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(profileId, name)
                    )""".trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sticker_groups_profileId ON sticker_groups(profileId)")
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS stickers (
                        id TEXT NOT NULL PRIMARY KEY,
                        profileId TEXT NOT NULL,
                        groupName TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        src TEXT NOT NULL,
                        rawLegacyJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stickers_profileId ON stickers(profileId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stickers_profileId_groupName ON stickers(profileId, groupName)")
            }
        }


        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""CREATE TABLE IF NOT EXISTS wallet_states (
                    profileId TEXT NOT NULL, ownerId TEXT NOT NULL, dataJson TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL, PRIMARY KEY(profileId, ownerId)
                )""".trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_states_profileId ON wallet_states(profileId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_states_ownerId ON wallet_states(ownerId)")
            }
        }
\n        @Volatile private var instance: MioDatabase? = null

        fun getInstance(context: Context): MioDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MioDatabase::class.java,
                    "miowatery-native.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build().also { instance = it }
            }
    }
}
