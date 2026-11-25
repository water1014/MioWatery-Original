package com.water1014.miowatery.core.network

import com.google.gson.JsonParser

data class ApiConnectionConfig(
    val baseUrl: String,
    val apiKey: String,
    val customHeadersJson: String = "",
    val userProfileId: String = "default",
    val conversationId: String = "default"
)

/** Mirrors legacy default headers, custom JSON headers and OpenCode session behavior. */
class ApiHeaderResolver(
    private val sessionStore: ApiSessionStore
) {
    suspend fun resolve(config: ApiConnectionConfig): Map<String, String> {
        val headers = linkedMapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ${config.apiKey}"
        )

        val raw = config.customHeadersJson.trim()
        if (raw.isNotEmpty()) {
            val parsed = try {
                JsonParser.parseString(raw)
            } catch (error: Exception) {
                throw IllegalArgumentException("自定义请求头格式错误，请填写有效的 JSON 对象", error)
            }
            if (!parsed.isJsonObject) {
                throw IllegalArgumentException("自定义请求头必须是 JSON 对象")
            }

            parsed.asJsonObject.entrySet().forEach { entry ->
                val name = entry.key.trim()
                if (name.isEmpty()) return@forEach

                val value = when {
                    entry.value.isJsonNull -> ""
                    entry.value.isJsonPrimitive -> entry.value.asJsonPrimitive.asString
                    else -> throw IllegalArgumentException("请求头「$name」的值必须是字符串、数字或布尔值")
                }
                if (value.isNotBlank()) {
                    val previous = headers.keys.firstOrNull { it.equals(name, ignoreCase = true) }
                    if (previous != null) headers.remove(previous)
                    headers[name] = value
                }
            }
        }

        val sessionHeader = headers.keys.firstOrNull {
            it.equals("x-opencode-session", ignoreCase = true)
        }
        val isOpenCode = config.baseUrl.contains("opencode.ai", ignoreCase = true)

        if (sessionHeader != null) {
            val value = headers.getValue(sessionHeader)
            if (value.contains("\$SESSION_ID") || value.contains("{{SESSION_ID}}")) {
                val sessionId = sessionStore.getOrCreate(
                    config.userProfileId,
                    config.conversationId
                )
                headers[sessionHeader] = value
                    .replace("\$SESSION_ID", sessionId)
                    .replace("{{SESSION_ID}}", sessionId)
            }
        } else if (isOpenCode) {
            headers["x-opencode-session"] = sessionStore.getOrCreate(
                config.userProfileId,
                config.conversationId
            )
        }

        return headers.toMap()
    }
}
