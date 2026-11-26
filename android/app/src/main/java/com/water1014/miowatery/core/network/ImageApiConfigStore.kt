package com.water1014.miowatery.core.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ImageApiConfig(
    val enabled: Boolean = false,
    val provider: String = "gpt",
    val gptBase: String = "https://api.openai.com/v1",
    val gptKey: String = "",
    val gptModel: String = "gpt-image-1",
    val gptPositive: String = "",
    val gptNegative: String = "",
    val gptDetail: String = "",
    val gptSize: String = "1024x1024",
    val gptQuality: String = "auto",
    val gptStyle: String = "",
    val gptCount: Int = 1,
    val naiBase: String = "https://image.novelai.net",
    val naiKey: String = "",
    val naiModel: String = "nai-diffusion-4-full",
    val naiPositive: String = "",
    val naiNegative: String = "",
    val naiDetail: String = "",
    val naiWidth: Int = 832,
    val naiHeight: Int = 1216,
    val naiScale: Float = 5f,
    val naiSteps: Int = 28,
    val naiSampler: String = "k_euler_ancestral",
    val naiCfgRescale: Float = 0f,
    val naiSeed: String = "",
    val naiSm: Boolean = false,
    val naiSmDynamic: Boolean = false
)

class ImageApiConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("miowatery_image_api", Context.MODE_PRIVATE)

    fun load(): ImageApiConfig = ImageApiConfig(
        enabled = prefs.getBoolean("enabled", false),
        provider = prefs.getString("provider", "gpt") ?: "gpt",
        gptBase = prefs.getString("gpt_base", "https://api.openai.com/v1") ?: "",
        gptKey = decrypt(prefs.getString("gpt_key", "").orEmpty()),
        gptModel = prefs.getString("gpt_model", "gpt-image-1") ?: "gpt-image-1",
        gptPositive = prefs.getString("gpt_positive", "").orEmpty(),
        gptNegative = prefs.getString("gpt_negative", "").orEmpty(),
        gptDetail = prefs.getString("gpt_detail", "").orEmpty(),
        gptSize = prefs.getString("gpt_size", "1024x1024") ?: "1024x1024",
        gptQuality = prefs.getString("gpt_quality", "auto") ?: "auto",
        gptStyle = prefs.getString("gpt_style", "").orEmpty(),
        gptCount = prefs.getInt("gpt_count", 1),
        naiBase = prefs.getString("nai_base", "https://image.novelai.net") ?: "",
        naiKey = decrypt(prefs.getString("nai_key", "").orEmpty()),
        naiModel = prefs.getString("nai_model", "nai-diffusion-4-full") ?: "nai-diffusion-4-full",
        naiPositive = prefs.getString("nai_positive", "").orEmpty(),
        naiNegative = prefs.getString("nai_negative", "").orEmpty(),
        naiDetail = prefs.getString("nai_detail", "").orEmpty(),
        naiWidth = prefs.getInt("nai_width", 832),
        naiHeight = prefs.getInt("nai_height", 1216),
        naiScale = prefs.getFloat("nai_scale", 5f),
        naiSteps = prefs.getInt("nai_steps", 28),
        naiSampler = prefs.getString("nai_sampler", "k_euler_ancestral") ?: "k_euler_ancestral",
        naiCfgRescale = prefs.getFloat("nai_cfg_rescale", 0f),
        naiSeed = prefs.getString("nai_seed", "").orEmpty(),
        naiSm = prefs.getBoolean("nai_sm", false),
        naiSmDynamic = prefs.getBoolean("nai_sm_dynamic", false)
    )

    fun save(c: ImageApiConfig) {
        prefs.edit()
            .putBoolean("enabled", c.enabled).putString("provider", c.provider)
            .putString("gpt_base", c.gptBase.trim()).putString("gpt_key", encrypt(c.gptKey))
            .putString("gpt_model", c.gptModel.trim()).putString("gpt_positive", c.gptPositive)
            .putString("gpt_negative", c.gptNegative).putString("gpt_detail", c.gptDetail)
            .putString("gpt_size", c.gptSize).putString("gpt_quality", c.gptQuality)
            .putString("gpt_style", c.gptStyle).putInt("gpt_count", c.gptCount.coerceIn(1, 4))
            .putString("nai_base", c.naiBase.trim()).putString("nai_key", encrypt(c.naiKey))
            .putString("nai_model", c.naiModel).putString("nai_positive", c.naiPositive)
            .putString("nai_negative", c.naiNegative).putString("nai_detail", c.naiDetail)
            .putInt("nai_width", c.naiWidth.coerceIn(64, 2048)).putInt("nai_height", c.naiHeight.coerceIn(64, 2048))
            .putFloat("nai_scale", c.naiScale.coerceIn(0f, 20f)).putInt("nai_steps", c.naiSteps.coerceIn(1, 50))
            .putString("nai_sampler", c.naiSampler).putFloat("nai_cfg_rescale", c.naiCfgRescale.coerceIn(0f, 1f))
            .putString("nai_seed", c.naiSeed).putBoolean("nai_sm", c.naiSm).putBoolean("nai_sm_dynamic", c.naiSmDynamic)
            .apply()
    }

    private fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String = runCatching {
        if (value.isBlank()) return ""
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }.getOrDefault("")

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("miowatery_image_api_v1", null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(
            "miowatery_image_api_v1",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }
}
