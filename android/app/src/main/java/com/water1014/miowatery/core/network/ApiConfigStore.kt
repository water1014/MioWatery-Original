package com.water1014.miowatery.core.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.apiConfigDataStore by preferencesDataStore(name = "api_connection")

data class SavedApiConfig(
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val customHeadersJson: String = "",
    val secondaryBaseUrl: String = "",
    val secondaryModel: String = "",
    val secondaryApiKey: String = ""
)

class ApiConfigStore(context: Context) {
    private val store = context.applicationContext.apiConfigDataStore

    val config: Flow<SavedApiConfig> = store.data.map { prefs ->
        val encrypted = prefs[KEY_SECRET].orEmpty()
        val encryptedSecondary = prefs[KEY_SECONDARY_SECRET].orEmpty()
        SavedApiConfig(
            baseUrl = prefs[KEY_BASE_URL].orEmpty(),
            model = prefs[KEY_MODEL].orEmpty(),
            apiKey = if (encrypted.isBlank()) "" else runCatching { ApiSecretCipher.decrypt(encrypted) }.getOrDefault(""),
            customHeadersJson = prefs[KEY_HEADERS].orEmpty(),
            secondaryBaseUrl = prefs[KEY_SECONDARY_BASE_URL].orEmpty(),
            secondaryModel = prefs[KEY_SECONDARY_MODEL].orEmpty(),
            secondaryApiKey = if (encryptedSecondary.isBlank()) "" else runCatching { ApiSecretCipher.decrypt(encryptedSecondary) }.getOrDefault("")
        )
    }

    suspend fun save(value: SavedApiConfig) {
        val encrypted = if (value.apiKey.isBlank()) "" else ApiSecretCipher.encrypt(value.apiKey)
        val encryptedSecondary = if (value.secondaryApiKey.isBlank()) "" else ApiSecretCipher.encrypt(value.secondaryApiKey)
        store.edit { prefs ->
            prefs[KEY_BASE_URL] = value.baseUrl.trim()
            prefs[KEY_MODEL] = value.model.trim()
            prefs[KEY_SECRET] = encrypted
            prefs[KEY_HEADERS] = value.customHeadersJson.trim()
            prefs[KEY_SECONDARY_BASE_URL] = value.secondaryBaseUrl.trim()
            prefs[KEY_SECONDARY_MODEL] = value.secondaryModel.trim()
            prefs[KEY_SECONDARY_SECRET] = encryptedSecondary
        }
    }

    private companion object {
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_MODEL = stringPreferencesKey("model")
        val KEY_SECRET = stringPreferencesKey("api_key_encrypted")
        val KEY_HEADERS = stringPreferencesKey("custom_headers_json")
        val KEY_SECONDARY_BASE_URL = stringPreferencesKey("secondary_base_url")
        val KEY_SECONDARY_MODEL = stringPreferencesKey("secondary_model")
        val KEY_SECONDARY_SECRET = stringPreferencesKey("secondary_api_key_encrypted")
    }
}

/** AES/GCM encryption backed by a non-exportable Android Keystore key. */
private object ApiSecretCipher {
    private const val KEY_ALIAS = "miowatery_api_secret_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > 12) { "加密密钥数据无效" }
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
