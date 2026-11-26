package com.water1014.miowatery.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredChatReplyParserTest {
    @Test
    fun preservesLegacyMessageMetadataWithoutLosingMessageText() {
        val parsed = StructuredChatReplyParser.parse(
            """{"messages":[{"text":"你好","quote":"原消息","tr":"hello","recall":"old","sticker":"wave"}],"thought":"thinking","thought_tr":"思考翻译","image_prompt":""}"""
        )
        assertEquals(1, parsed.messages.size)
        assertEquals("你好", parsed.messages.single().text)
        val metadata = parsed.messages.single().metadataJson.orEmpty()
        assertTrue(metadata.contains("\"quote\":\"原消息\""))
        assertTrue(metadata.contains("\"tr\":\"hello\""))
        assertTrue(metadata.contains("\"recall\":\"old\""))
        assertTrue(metadata.contains("\"sticker\":\"wave\""))
        assertTrue(parsed.rawJson.contains("\"thought_tr\":\"思考翻译\""))
        assertEquals("thinking", parsed.thought)
    }

    @Test
    fun supportsNarrationAndReplyFallback() {
        val narration = StructuredChatReplyParser.parse("""{"messages":[{"narr":"她轻轻笑了笑。"}]}""")
        assertTrue(narration.messages.single().isNarration)
        assertEquals("她轻轻笑了笑。", narration.messages.single().text)

        val fallback = StructuredChatReplyParser.parse("""{"reply":"备用回复"}""")
        assertEquals("备用回复", fallback.messages.single().text)
        assertFalse(fallback.messages.single().isNarration)
    }
}
