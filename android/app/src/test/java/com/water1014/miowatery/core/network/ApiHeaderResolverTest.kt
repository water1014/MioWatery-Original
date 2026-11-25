package com.water1014.miowatery.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiHeaderResolverTest {
    @Test
    fun defaultHeadersMatchLegacyBehavior() = runBlocking {
        val headers = ApiHeaderResolver(InMemoryApiSessionStore()).resolve(
            ApiConnectionConfig("https://api.example.com/v1", "test-key")
        )
        assertEquals("application/json", headers["Content-Type"])
        assertEquals("Bearer test-key", headers["Authorization"])
        assertFalse(headers.containsKey("x-opencode-session"))
    }

    @Test
    fun customHeadersOverrideDefaultsCaseInsensitively() = runBlocking {
        val headers = ApiHeaderResolver(InMemoryApiSessionStore()).resolve(
            ApiConnectionConfig(
                baseUrl = "https://api.example.com/v1",
                apiKey = "test-key",
                customHeadersJson = """{"authorization":"Token custom","X-Trace":42}"""
            )
        )
        assertEquals("Token custom", headers["authorization"])
        assertFalse(headers.containsKey("Authorization"))
        assertEquals("42", headers["X-Trace"])
    }

    @Test
    fun openCodeSessionIsStablePerUserAndConversation() = runBlocking {
        val resolver = ApiHeaderResolver(InMemoryApiSessionStore())
        val config = ApiConnectionConfig(
            baseUrl = "https://opencode.ai/zen/go",
            apiKey = "test-key",
            userProfileId = "user-a",
            conversationId = "persona-a"
        )
        val first = resolver.resolve(config).getValue("x-opencode-session")
        val second = resolver.resolve(config).getValue("x-opencode-session")
        val otherChat = resolver.resolve(config.copy(conversationId = "persona-b"))
            .getValue("x-opencode-session")
        assertEquals(first, second)
        assertTrue(first.startsWith("mio-"))
        assertTrue(first != otherChat)
    }

    @Test
    fun sessionPlaceholderIsReplacedInCustomHeader() = runBlocking {
        val resolver = ApiHeaderResolver(InMemoryApiSessionStore())
        val config = ApiConnectionConfig(
            baseUrl = "https://opencode.ai/zen/go",
            apiKey = "test-key",
            customHeadersJson = """{"X-OpenCode-Session":"prefix-${'$'}SESSION_ID"}""",
            userProfileId = "u1",
            conversationId = "c1"
        )
        val value = resolver.resolve(config).getValue("X-OpenCode-Session")
        assertTrue(value.startsWith("prefix-mio-"))
        assertFalse(value.contains("\$SESSION_ID"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidCustomHeaderJsonIsRejected() = runBlocking {
        ApiHeaderResolver(InMemoryApiSessionStore()).resolve(
            ApiConnectionConfig("https://api.example.com/v1", "test-key", "not-json")
        )
    }
}

class ApiEndpointResolverTest {
    @Test
    fun chatEndpointTrimsTrailingSlashes() {
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            ApiEndpointResolver.chatCompletions("https://api.example.com/v1///")
        )
    }

    @Test
    fun modelListSupportsOpenCodeGoInferencePath() {
        assertEquals(
            "https://opencode.ai/inference/go/v1/models",
            ApiEndpointResolver.modelList("https://opencode.ai/inference/go/openai/v1")
        )
    }

    @Test
    fun modelListSupportsOpenCodeGoRootPath() {
        assertEquals(
            "https://opencode.ai/inference/go/v1/models",
            ApiEndpointResolver.modelList("https://opencode.ai/inference/go/")
        )
    }

    @Test
    fun modelListUsesDefaultOpenAiPath() {
        assertEquals(
            "https://api.example.com/v1/models",
            ApiEndpointResolver.modelList("https://api.example.com/v1")
        )
    }
}
