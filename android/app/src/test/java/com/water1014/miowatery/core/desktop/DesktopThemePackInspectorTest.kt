package com.water1014.miowatery.core.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopThemePackInspectorTest {
    @Test
    fun inspectsSupportedDesktopFieldsAndRejectsUnsafeImageUrls() {
        val json = """
            {
              "app": "xsj-desktop-theme",
              "v": 1,
              "name": "Rainy desk",
              "desktop": {
                "wallpaper": "https://example.com/wallpaper.webp",
                "icons": {
                  "app": "https://example.com/chat.png",
                  "unsafe": "javascript:alert(1)"
                },
                "widgets": [
                  {"id": "clock-1", "kind": "clock", "title": "Time"},
                  {"id": "note-1", "kind": "text", "title": "Note", "text": "hello"},
                  {"id": "image-1", "kind": "image", "title": "Photo", "image": "https://example.com/photo.webp"}
                ],
                "hidden": ["weather", "__proto__"],
                "order": {"first": ["app", "worlds"]}
              }
            }
        """.trimIndent()

        val preview = DesktopThemePackInspector.inspect(json)

        assertEquals("Rainy desk", preview.name)
        assertEquals("https://example.com/wallpaper.webp", preview.wallpaper)
        assertEquals(1, preview.iconCount)
        assertTrue(preview.icons.containsKey("app"))
        assertEquals(3, preview.widgets.size)
        assertEquals(setOf("weather"), preview.hiddenWidgetIds)
        assertEquals(listOf("app", "worlds"), preview.placementGroups["first"])
    }

    @Test
    fun neverImportsExecutableCustomWidgetCode() {
        val json = """
            {
              "app": "xsj-desktop-theme",
              "v": 1,
              "name": "Code pack",
              "desktop": {
                "widgets": [{"id":"custom-1","kind":"css","lib":"lib-1","page":1}],
                "wlib": [{"id":"lib-1","name":"Custom","html":"<script>alert(1)</script>","css":"body{}","js":"fetch('https://example.com')"}]
              }
            }
        """.trimIndent()

        val preview = DesktopThemePackInspector.inspect(json)

        assertTrue(preview.widgets.isEmpty())
        assertEquals(1, preview.unsupportedCustomWidgetCount)
        assertTrue(preview.warnings.any { it.contains("不会运行") })
    }

    @Test
    fun updatesDesktopOrderWithoutDiscardingExistingThemeData() {
        val original = """
            {
              "app": "xsj-desktop-theme",
              "v": 1,
              "name": "My desk",
              "desktop": {
                "wallpaper": "https://example.com/wallpaper.webp",
                "icons": {"wallet": "https://example.com/wallet.png"},
                "order": {"p4": ["wallet", "music"]}
              }
            }
        """.trimIndent()

        val updated = DesktopThemePackStore.updateOrder(
            original,
            mapOf("p4" to listOf("music", "wallet", "bad id"))
        )
        val root = org.json.JSONObject(updated)
        val desktop = root.getJSONObject("desktop")

        assertEquals("My desk", root.getString("name"))
        assertEquals("https://example.com/wallpaper.webp", desktop.getString("wallpaper"))
        assertEquals("https://example.com/wallet.png", desktop.getJSONObject("icons").getString("wallet"))
        assertEquals(listOf("music", "wallet"), buildList {
            val ids = desktop.getJSONObject("order").getJSONArray("p4")
            for (index in 0 until ids.length()) add(ids.getString(index))
        })
    }

    @Test
    fun rejectsUnknownPackFormat() {
        val failure = runCatching {
            DesktopThemePackInspector.inspect("""{"app":"other","v":1,"desktop":{}}""")
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertFalse(failure?.message.isNullOrBlank())
    }

    @Test
    fun serializesSupportedFieldsSoAnExportCanBeImportedAgain() {
        val original = """
            {
              "app": "xsj-desktop-theme",
              "v": 1,
              "name": "Window garden",
              "desktop": {
                "wallpaper": "https://example.com/wallpaper.webp",
                "icons": {"music": "https://example.com/music.png"},
                "widgets": [
                  {"id":"clock-main","kind":"clock","title":"Clock"},
                  {"id":"note-main","kind":"text","title":"Note","text":"Remember the little things"},
                  {"id":"photo-main","kind":"image","title":"Photo","image":"https://example.com/photo.webp"}
                ],
                "hidden": ["weather"],
                "order": {
                  "first": ["app","worlds"],
                  "p3": ["couple","msgs"],
                  "dock": ["settings","personas"]
                },
                "wlib": []
              }
            }
        """.trimIndent()

        val imported = DesktopThemePackInspector.inspect(original)
        val exported = DesktopThemePackStore.serialize(imported)
        val restored = DesktopThemePackInspector.inspect(exported)

        assertEquals(imported.name, restored.name)
        assertEquals(imported.wallpaper, restored.wallpaper)
        assertEquals(imported.icons, restored.icons)
        assertEquals(imported.widgets, restored.widgets)
        assertEquals(imported.hiddenWidgetIds, restored.hiddenWidgetIds)
        assertEquals(imported.placementGroups, restored.placementGroups)
    }

    @Test
    fun warnsWhenWebOnlyFreeLayoutAndGlassSettingsCannotBeApplied() {
        val json = """
            {
              "app": "xsj-desktop-theme",
              "v": 1,
              "desktop": {
                "free": {"1": {"app:music": {"x": 0.1, "y": 120, "w": 0.4}}},
                "glass": {"blur": 18, "alpha": 0.45}
              }
            }
        """.trimIndent()

        val preview = DesktopThemePackInspector.inspect(json)

        assertTrue(preview.warnings.any { it.contains("自由摆放坐标") })
        assertTrue(preview.warnings.any { it.contains("磨砂玻璃") })
    }
}
