package com.water1014.miowatery.core.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LegacyBackupInspectorTest {
    private val json = """
        {
          "userProfiles":[{"id":"u1"}],
          "personas":[{"id":"p1"},{"id":"p2"}],
          "worlds":[{"id":"w1"}],
          "chatsByUser":{
            "u1":{
              "p1":[{"role":"user","content":"hi"},{"role":"assistant","content":"hello"}],
              "p2":[{"role":"user","content":"image","image":"data:image/png;base64,AA"}]
            }
          }
        }
    """.trimIndent()

    @Test
    fun inspectsLegacyJsonWithoutImportingIt() {
        val preview = LegacyBackupInspector.inspect(
            ByteArrayInputStream(json.toByteArray()),
            "xiaoshouji-backup-2026-11-25.json"
        )

        assertEquals("JSON", preview.sourceFormat)
        assertEquals(1, preview.profileCount)
        assertEquals(2, preview.personaCount)
        assertEquals(1, preview.worldbookCount)
        assertEquals(1, preview.chatOwnerCount)
        assertEquals(2, preview.conversationCount)
        assertEquals(3, preview.messageCount)
        assertTrue(preview.warnings.any { it.contains("只读预览") })
    }

    @Test
    fun inspectsZipBackupAndFindsBackupJson() {
        val zipped = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("xiaoshouji-backup.json"))
                zip.write(json.toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        val preview = LegacyBackupInspector.inspect(
            ByteArrayInputStream(zipped),
            "xiaoshouji-backup.zip"
        )

        assertEquals("ZIP", preview.sourceFormat)
        assertEquals("xiaoshouji-backup.json", preview.jsonFileName)
        assertEquals(3, preview.messageCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonObjectBackupRoot() {
        LegacyBackupInspector.inspect(
            ByteArrayInputStream("[]".toByteArray()),
            "bad.json"
        )
    }
}
