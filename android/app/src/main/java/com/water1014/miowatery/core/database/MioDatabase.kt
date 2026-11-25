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
        DiaryEntryEntity::class
    ],
    version = 2,
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

        @Volatile private var instance: MioDatabase? = null

        fun getInstance(context: Context): MioDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MioDatabase::class.java,
                    "miowatery-native.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
