package com.water1014.miowatery.core.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import java.util.UUID

private val Context.apiSessionDataStore by preferencesDataStore(name = "api_sessions")

/** Stable session IDs scoped to one user profile and one conversation. */
interface ApiSessionStore {
    suspend fun getOrCreate(profileId: String, conversationId: String): String
}

class DataStoreApiSessionStore(context: Context) : ApiSessionStore {
    private val store = context.applicationContext.apiSessionDataStore

    override suspend fun getOrCreate(profileId: String, conversationId: String): String {
        val identity = "$profileId:$conversationId"
        val key = stringPreferencesKey("session_${identity.sha256()}")
        val preferences = store.edit { current ->
            if (current[key].isNullOrBlank()) {
                current[key] = newSessionId()
            }
        }
        return requireNotNull(preferences[key])
    }
}

/** In-memory implementation for unit tests and previews only. */
class InMemoryApiSessionStore : ApiSessionStore {
    private val sessions = mutableMapOf<String, String>()

    override suspend fun getOrCreate(profileId: String, conversationId: String): String =
        synchronized(sessions) {
            sessions.getOrPut("$profileId:$conversationId") { newSessionId() }
        }
}

private fun newSessionId(): String =
    "mio-${UUID.randomUUID().toString().replace("-", "")}-${System.currentTimeMillis().toString(36)}"

private fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
