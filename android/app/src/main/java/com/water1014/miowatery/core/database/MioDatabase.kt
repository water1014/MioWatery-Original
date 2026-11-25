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
        WorldbookEntryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MioDatabase : RoomDatabase() {
    abstract fun mioDao(): MioDao

    companion object {
        @Volatile private var instance: MioDatabase? = null

        fun getInstance(context: Context): MioDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MioDatabase::class.java,
                    "miowatery-native.db"
                ).build().also { instance = it }
            }
    }
}
