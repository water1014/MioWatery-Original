package com.water1014.miowatery.core.network

import android.content.Context
import android.util.Base64
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Native implementation of MioWatery's existing GPT Images and NovelAI image providers.
 * Generated files are kept in app-private storage so chat history remains available.
 */
class ImageGenerationClient(
    private val context: Context,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    fun generate(prompt: String, config: ImageApiConfig): String {
        require(config.enabled) { "请先在「生图 API」中开启生图" }
        require(prompt.isNotBlank()) { "图片描述为空" }
        val bytes = if (config.provider == "novelai") generateNovelAi(prompt, config) else generateGpt(prompt, config)
        if (bytes.isEmpty()) throw IOException("生图接口没有返回图片")
        val directory = File(context.filesDir, "generated_images").apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.png")
        file.writeBytes(bytes)
        return android.net.Uri.fromFile(file).toString()
    }

    private fun generateGpt(prompt: String, c: ImageApiConfig): ByteArray {
        if (c.gptKey.isBlank()) throw IOException("请先在「生图 API」里填写 GPT 生图 Key")
        val base = c.gptBase.trim().trimEnd('/').ifBlank { "https://api.openai.com/v1" }
        val parts = listOf(c.gptPositive, prompt, c.gptDetail).filter { it.isNotBlank() }.toMutableList()
        if (c.gptNegative.isNotBlank()) parts.add("Avoid / negative prompt: " + c.gptNegative)
        val body = JSONObject()
            .put("model", c.gptModel.ifBlank { "gpt-image-1" })
            .put("prompt", parts.joinToString("\n\n"))
            .put("n", c.gptCount.coerceIn(1, 4))
        if (c.gptSize.isNotBlank() && c.gptSize != "auto") body.put("size", c.gptSize)
        if (c.gptQuality.isNotBlank() && c.gptQuality != "auto") body.put("quality", c.gptQuality)
        if (c.gptStyle.isNotBlank()) body.put("style", c.gptStyle)
        val request = Request.Builder()
            .url("$base/images/generations")
            .header("Authorization", "Bearer ${c.gptKey}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()
        val responseText = http.newCall(request).execute().use { response ->
            val responseBody = response.body ?: throw IOException("GPT 生图接口返回为空")
            val raw = responseBody.string()
            if (!response.isSuccessful) throw IOException("GPT 生图接口返回 ${response.code}：${raw.take(800)}")
            raw
        }
        val first = runCatching {
            JsonParser.parseString(responseText).asJsonObject
                .getAsJsonArray("data").first().asJsonObject
        }.getOrElse { throw IOException("无法解析 GPT 生图响应", it) }
        val encoded = first.get("b64_json")?.takeIf { !it.isJsonNull }?.asString
        if (!encoded.isNullOrBlank()) return Base64.decode(encoded, Base64.DEFAULT)
        val imageUrl = first.get("url")?.takeIf { !it.isJsonNull }?.asString
            ?: throw IOException("GPT 生图响应中没有图片数据")
        val imageRequest = Request.Builder().url(imageUrl).get().build()
        return http.newCall(imageRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("下载生成图片失败：HTTP ${response.code}")
            response.body?.bytes() ?: throw IOException("生成图片下载内容为空")
        }
    }

    private fun generateNovelAi(prompt: String, c: ImageApiConfig): ByteArray {
        if (c.naiKey.isBlank()) throw IOException("请先在「生图 API」里填写 NovelAI Token / 中转站 Key")
        val base = c.naiBase.trim().trimEnd('/').ifBlank { "https://image.novelai.net" }
        val positive = listOf(c.naiPositive, prompt, c.naiDetail).filter { it.isNotBlank() }.joinToString(", ")
        var model = c.naiModel.ifBlank { "nai-diffusion-4-full" }
        if (model == "nai-diffusion-4-curated") model = "nai-diffusion-4-curated-preview"
        val isV4 = model.contains("diffusion-4")
        val negative = c.naiNegative
        val parameters = JSONObject()
            .put("width", c.naiWidth.takeIf { it > 0 } ?: 832)
            .put("height", c.naiHeight.takeIf { it > 0 } ?: 1216)
            .put("scale", c.naiScale.takeIf { it > 0f } ?: 5f)
            .put("steps", c.naiSteps.takeIf { it > 0 } ?: 28)
            .put("sampler", c.naiSampler.ifBlank { "k_euler_ancestral" })
            .put("n_samples", 1)
            .put("uc", negative)
            .put("cfg_rescale", c.naiCfgRescale)
            .put("sm", if (isV4) false else c.naiSm)
            .put("sm_dyn", if (isV4) false else c.naiSmDynamic)
            .put("seed", c.naiSeed.toLongOrNull()?.takeIf { it >= 0 } ?: (0L..4294967295L).random())
        if (isV4) {
            parameters.put("params_version", 3)
                .put("legacy", false)
                .put("legacy_v3_extend", false)
                .put("noise_schedule", "karras")
                .put("qualityToggle", false)
                .put("ucPreset", 2)
                .put("autoSmea", false)
                .put("add_original_image", true)
                .put("controlnet_strength", 1)
                .put("dynamic_thresholding", false)
                .put("prefer_brownian", true)
                .put("deliberate_euler_ancestral_bug", false)
                .put("skip_cfg_above_sigma", JSONObject.NULL)
                .put("use_coords", false)
                .put("characterPrompts", org.json.JSONArray())
                .put("v4_prompt", JSONObject().put("caption", JSONObject().put("base_caption", positive).put("char_captions", org.json.JSONArray())).put("use_coords", false).put("use_order", true))
                .put("v4_negative_prompt", JSONObject().put("caption", JSONObject().put("base_caption", negative).put("char_captions", org.json.JSONArray())).put("legacy_uc", false))
        }
        val payload = JSONObject().put("input", positive).put("model", model).put("action", "generate").put("parameters", parameters)
        val request = Request.Builder()
            .url("$base/ai/generate-image")
            .header("Authorization", "Bearer ${c.naiKey}")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON))
            .build()
        return http.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes() ?: throw IOException("NovelAI 生图接口返回为空")
            if (!response.isSuccessful) {
                val detail = bytes.toString(Charsets.UTF_8).replace(Regex("\\s+"), " ").take(800)
                throw IOException("NovelAI / 中转站接口返回 ${response.code}${if (detail.isBlank()) "" else "（$detail）"}")
            }
            val contentType = response.header("Content-Type").orEmpty()
            if (contentType.startsWith("image/")) return@use bytes
            if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte()) {
                var extracted: ByteArray? = null
                ZipInputStream(bytes.inputStream()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory && entry.name.lowercase().let { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".webp") }) {
                            val output = zip.readBytes()
                            if (output.isNotEmpty()) {
                                extracted = output
                                break
                            }
                        }
                    }
                }
                return@use extracted ?: throw IOException("NovelAI ZIP 中没有找到图片文件")
            }
            throw IOException("NovelAI 返回了无法识别的图片格式")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
