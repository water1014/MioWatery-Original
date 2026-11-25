package com.water1014.miowatery.core.network

object ApiEndpointResolver {
    fun chatCompletions(baseUrl: String): String =
        baseUrl.trim().trimEnd('/') + "/chat/completions"

    /** Keeps the special OpenCode Go model-list URL rewrites from the legacy app. */
    fun modelList(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        return when {
            base.endsWith("/inference/go/openai/v1", ignoreCase = true) ->
                base.replace(Regex("/openai/v1$", RegexOption.IGNORE_CASE), "/v1") + "/models"
            base.endsWith("/inference/go", ignoreCase = true) ->
                "$base/v1/models"
            else -> "$base/models"
        }
    }
}
