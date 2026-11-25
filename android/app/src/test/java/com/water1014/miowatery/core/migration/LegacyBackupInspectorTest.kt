package com.water1014.miowatery.core.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class LegacyBackupInspectorTest {
    @Test
    fun legacySingleUserChatsAreIncludedInPreviewCounts() {
        val json = """
            {
              "activeUserProfile": "main",
              "me": {"name": "我"},
              "chats": {
                "songqi": [
                  {"role": "user", "content": "你好", "t": 100},
                  {"role": "assistant", "content": "我在", "t": 101}
                ],
                "empty": []
              },
              "personas": [{"id": "songqi", "name": "宋祁"}],
              "worlds": []
            }
        """.trimIndent()

        val preview = LegacyBackupInspector.inspect(
            ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)),
            "legacy.json"
        )

        assertEquals(1, preview.profileCount)
        assertEquals(1, preview.chatOwnerCount)
        assertEquals(2, preview.conversationCount)
        assertEquals(2, preview.messageCount)
        assertTrue(preview.warnings.any { it.contains("旧式 chats") })
    }

    @Test
    fun modernChatsByUserRemainCounted() {
        val json = """
            {
              "userProfiles": [{"id": "main", "name": "我"}],
              "chatsByUser": {
                "main": {
                  "songqi": [
                    {"role": "user", "content": "在吗"},
                    {"role": "assistant", "content": "在"}
                  ]
                }
              }
            }
        """.trimIndent()

        val preview = LegacyBackupInspector.inspect(
            ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)),
            "modern.json"
        )

        assertEquals(1, preview.profileCount)
        assertEquals(1, preview.chatOwnerCount)
        assertEquals(1, preview.conversationCount)
        assertEquals(2, preview.messageCount)
    }
}
