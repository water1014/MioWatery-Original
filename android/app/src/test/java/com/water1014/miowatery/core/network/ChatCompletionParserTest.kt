package com.water1014.miowatery.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatCompletionParserTest {
    @Test
    fun parsesContentAndReasoningFromStreamDelta() {
        val delta = ChatCompletionClient.parseStreamDelta(
            """{"choices":[{"delta":{"content":"hello","reasoning_content":"thinking"},"finish_reason":null}]}"""
        )
        assertEquals("hello", delta.content)
        assertEquals("thinking", delta.reasoning)
        assertNull(delta.finishReason)
    }

    @Test
    fun parsesFinishReasonFromStreamDelta() {
        val delta = ChatCompletionClient.parseStreamDelta(
            """{"choices":[{"delta":{},"finish_reason":"stop"}]}"""
        )
        assertEquals("stop", delta.finishReason)
    }
}
