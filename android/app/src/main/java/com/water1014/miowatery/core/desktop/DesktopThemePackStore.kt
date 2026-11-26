package com.water1014.miowatery.core.desktop

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object DesktopThemePackStore {
    private const val FILE_NAME = "desktop-theme-pack.json"
    private const val MAX_STORED_BYTES = 32 * 1024 * 1024

    fun load(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.isFile || file.length() > MAX_STORED_BYTES) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    fun save(context: Context, json: String) {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_STORED_BYTES) {
            "美化包超过原生版 32 MB 的安全保存上限"
        }
        val target = File(context.filesDir, FILE_NAME)
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        temp.writeText(json, Charsets.UTF_8)
        try {
            Files.move(
                temp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (failure: Exception) {
            temp.delete()
            throw IllegalStateException("保存桌面美化配置失败", failure)
        }
    }

    /** Update only desktop placement order while preserving every other imported theme-pack field. */
    fun updateOrder(json: String?, groups: Map<String, List<String>>): String {
        val root = runCatching { JSONObject(json ?: "{}") }.getOrElse { JSONObject() }
        val desktop = root.optJSONObject("desktop") ?: JSONObject().also { root.put("desktop", it) }
        val order = desktop.optJSONObject("order") ?: JSONObject().also { desktop.put("order", it) }
        groups.forEach { (group, ids) ->
            order.put(group, JSONArray(ids.distinct().filter { it.matches(Regex("[A-Za-z0-9_:.\\\\-]{1,80}")) }))
        }
        if (!root.has("app")) root.put("app", "xsj-desktop-theme")
        if (!root.has("v")) root.put("v", 1)
        if (!root.has("created")) root.put("created", java.time.LocalDate.now().toString())
        if (!root.has("name")) root.put("name", "我的桌面")
        return root.toString(2)
    }

    fun serialize(preview: DesktopThemePackPreview): String {
        val desktop = JSONObject()
        preview.wallpaper?.let { desktop.put("wallpaper", it) }
        val icons = JSONObject()
        preview.icons.forEach { (key, value) -> icons.put(key, value) }
        desktop.put("icons", icons)

        val widgets = JSONArray()
        preview.widgets.forEach { widget ->
            val item = JSONObject()
                .put("id", widget.id)
                .put("kind", widget.kind)
                .put("title", widget.title)
            widget.text?.let { item.put("text", it) }
            widget.image?.let { item.put("image", it) }
            widgets.put(item)
        }
        desktop.put("widgets", widgets)
        desktop.put("hidden", JSONArray(preview.hiddenWidgetIds.toList()))

        val order = JSONObject()
        preview.placementGroups.forEach { (group, ids) -> order.put(group, JSONArray(ids)) }
        desktop.put("order", order)
        desktop.put("wlib", JSONArray())
        desktop.put("free", JSONObject())

        return JSONObject()
            .put("app", "xsj-desktop-theme")
            .put("v", 1)
            .put("created", java.time.LocalDate.now().toString())
            .put("name", preview.name)
            .put("desktop", desktop)
            .toString(2)
    }
}
