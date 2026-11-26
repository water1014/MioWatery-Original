package com.water1014.miowatery.core.desktop

import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopOrderResolverTest {
    private val defaults = linkedMapOf(
        "second" to listOf("diary", "rolephones", "desktopcustom", "music"),
        "p3" to listOf("couple", "coread", "offline", "msgs"),
        "dock" to listOf("app", "worlds", "personas", "settings")
    )
    private val validIds = defaults.values.flatten().toSet()

    @Test
    fun keepsSavedOrderAndRestoresUnmentionedDefaultShortcuts() {
        val result = DesktopOrderResolver.resolve(
            defaults,
            mapOf("second" to listOf("music", "diary")),
            validIds
        )
        assertEquals(
            listOf("music", "diary", "rolephones", "desktopcustom"),
            result["second"]
        )
    }

    @Test
    fun assignsDuplicateShortcutToOnlyItsFirstGroup() {
        val result = DesktopOrderResolver.resolve(
            defaults,
            mapOf(
                "second" to listOf("music", "app"),
                "dock" to listOf("app", "worlds")
            ),
            validIds
        )
        assertEquals(listOf("music", "app", "diary", "rolephones", "desktopcustom"), result["second"])
        assertEquals(listOf("worlds", "personas", "settings"), result["dock"])
    }

    @Test
    fun ignoresUnknownIdsAndKeepsAllValidDefaults() {
        val result = DesktopOrderResolver.resolve(
            defaults,
            mapOf("p3" to listOf("not-an-app", "msgs", "couple")),
            validIds
        )
        assertEquals(listOf("msgs", "couple", "coread", "offline"), result["p3"])
    }
}
