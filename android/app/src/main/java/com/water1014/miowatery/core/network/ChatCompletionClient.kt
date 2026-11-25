package com.water1014.miowatery.core.network

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ChatMessagePayload(
    val role: String,
    val content: JsonElement
) {
    companion object {
        fun text(role: String, content: String) =
            ChatMessagePayload(role, com.google.gson.JsonPrimitive(content))

        fun image(role: String, content: String, dataUrl: String): ChatMessagePayload {
            val parts = JsonArray()
            parts.add(JsonObject().apply {
                addProperty("type", "text")
                addProperty("text", content.ifBlank { "请查看这张图片并描述其中的内容。" })
            })
            parts.add(JsonObject().apply {
                addProperty("type", "image_url")
                add("image_url", JsonObject().apply {
                    addProperty("url", dataUrl)
                    addProperty("detail", "auto")
                })
            })
            return ChatMessagePayload(role, parts)
        }
    }
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessagePayload>,
    val temperature: Double = 0.8,
    val stream: Boolean = true,
    val extraBodyJson: String = ""
)

data class ChatCompletionResult(
    val content: String,
    val reasoning: String,
    val finishReason: String?,
    val rawResponse: String
)

data class ChatStreamDelta(
    val content: String = "",
    val reasoning: String = "",
    val finishReason: String? = null
)

/**
 * OpenAI-compatible chat completions transport.
 * Supports standard JSON responses and Server-Sent Events, including reasoning deltas.
 */
class ChatCompletionClient(
    private val headerResolver: ApiHeaderResolver,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    suspend fun complete(
        config: ApiConnectionConfig,
        payload: ChatCompletionRequest,
        onDelta: (ChatStreamDelta) -> Unit = {}
    ): ChatCompletionResult {
        require(payload.model.isNotBlank()) { "请先选择模型" }
        require(payload.messages.isNotEmpty()) { "聊天消息不能为空" }

        val headers = headerResolver.resolve(config)
        val requestJson = buildRequestJson(payload)
        val request = Request.Builder()
            .url(ApiEndpointResolver.chatCompletions(config.baseUrl))
            .post(requestJson.toRequestBody(JSON_MEDIA_TYPE))
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, error: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    try {
                        val result = response.use { readResponse(it, payload.stream, onDelta) }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            })
        }
    }

    private fun buildRequestJson(payload: ChatCompletionRequest): String {
        val body = JsonObject().apply {
            addProperty("model", payload.model)
            addProperty("temperature", payload.temperature)
            addProperty("stream", payload.stream)
            add("messages", JsonArray().apply {
                payload.messages.forEach { message ->
                    add(JsonObject().apply {
                        addProperty("role", message.role)
                        add("content", message.content)
                    })
                }
            })
        }

        val extra = payload.extraBodyJson.trim()
        if (extra.isNotEmpty()) {
            val parsed = try {
                JsonParser.parseString(extra)
            } catch (error: Exception) {
                throw IllegalArgumentException("额外请求参数必须是有效的 JSON 对象", error)
            }
            require(parsed.isJsonObject) { "额外请求参数必须是 JSON 对象" }
            parsed.asJsonObject.entrySet().forEach { (key, value) ->
                if (key !in setOf("model", "messages", "stream")) body.add(key, value)
            }
        }
        return GSON.toJson(body)
    }

    private fun readResponse(
        response: Response,
        streaming: Boolean,
        onDelta: (ChatStreamDelta) -> Unit
    ): ChatCompletionResult {
        if (!response.isSuccessful) {
            val body = response.body?.string().orEmpty()
            val message = runCatching {
                JsonParser.parseString(body).asJsonObject
                    .getAsJsonObject("error")?.get("message")?.asString
            }.getOrNull()
            throw IOException(
                "API 请求失败（HTTP ${response.code}）：${message ?: body.take(1200).ifBlank { response.message }}"
            )
        }

        val body = response.body ?: throw IOException("API 返回内容为空")
        return if (streaming) {
            val content = StringBuilder()
            val reasoning = StringBuilder()
            var finishReason: String? = null
            val raw = StringBuilder()
            val source = body.source()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty()) continue
                raw.append(data).append('\n')
                if (data == "[DONE]") break

                val delta = parseStreamDelta(data)
                if (delta.content.isNotEmpty()) content.append(delta.content)
                if (delta.reasoning.isNotEmpty()) reasoning.append(delta.reasoning)
                if (delta.finishReason != null) finishReason = delta.finishReason
                if (delta.content.isNotEmpty() || delta.reasoning.isNotEmpty()) onDelta(delta)
            }
            ChatCompletionResult(
                content = content.toString(),
                reasoning = reasoning.toString(),
                finishReason = finishReason,
                rawResponse = raw.toString()
            )
        } else {
            val raw = body.string()
            val root = try {
                JsonParser.parseString(raw).asJsonObject
            } catch (error: Exception) {
                throw IOException("无法解析 API 响应 JSON", error)
            }
            val choice = root.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
                ?: throw IOException("API 响应中没有 choices[0]")
            val message = choice.getAsJsonObject("message")
                ?: throw IOException("API 响应中没有 message")
            val content = readContent(message.get("content"))
            val reasoning = listOf("reasoning_content", "reasoning", "thinking", "reasoning_text")
                .firstNotNullOfOrNull { key -> message.get(key)?.takeIf { !it.isJsonNull }?.let(::readContent) }
                .orEmpty()
            val (cleanContent, taggedReasoning) = extractThinkTags(content)
            ChatCompletionResult(
                content = cleanContent,
                reasoning = reasoning.ifBlank { taggedReasoning },
                finishReason = choice.get("finish_reason")?.takeIf { !it.isJsonNull }?.asString,
                rawResponse = raw
            )
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val GSON = com.google.gson.Gson()

        internal fun parseStreamDelta(data: String): ChatStreamDelta {
            val root = try {
                JsonParser.parseString(data).asJsonObject
            } catch (error: Exception) {
                throw IOException("无法解析流式响应数据", error)
            }
            val choice = root.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
                ?: return ChatStreamDelta()
            val delta = choice.getAsJsonObject("delta") ?: JsonObject()
            val content = readContent(
                delta.get("content") ?: delta.get("text")
            )
            val reasoning = listOf("reasoning_content", "reasoning", "thinking", "reasoning_text")
                .firstNotNullOfOrNull { key ->
                    delta.get(key)?.takeIf { !it.isJsonNull }?.let(::readContent)
                }.orEmpty()
            return ChatStreamDelta(
                content = content,
                reasoning = reasoning,
                finishReason = choice.get("finish_reason")?.takeIf { !it.isJsonNull }?.asString
            )
        }

        private fun readContent(value: JsonElement?): String {
            if (value == null || value.isJsonNull) return ""
            if (value.isJsonPrimitive) return value.asString
            if (value.isJsonArray) {
                return value.asJsonArray.joinToString("") { part ->
                    if (!part.isJsonObject) "" else {
                        val obj = part.asJsonObject
                        when {
                            obj.has("text") -> obj.get("text").asString
                            obj.has("content") -> readContent(obj.get("content"))
                            else -> ""
                        }
                    }
                }
            }
            return ""
        }

        private fun extractThinkTags(input: String): Pair<String, String> {
            val reasoning = StringBuilder()
            val cleaned = Regex(
                "<(?:think|thinking)>([\\s\\S]*?)</(?:think|thinking)>",
                RegexOption.IGNORE_CASE
            ).replace(input) { match ->
                reasoning.append(match.groupValues[1])
                ""
            }
            return cleaned to reasoning.toString()
        }
    }
}
