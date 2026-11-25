package com.water1014.miowatery.core.desktop

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object DesktopThemePackStore {
    private const val PREFS = "miowatery_desktop_customization"
    private const val KEY_PACK = "current_native_pack"

    fun load(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PACK, null)

    fun save(context: Context, json: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PACK, json)
            .apply()
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
