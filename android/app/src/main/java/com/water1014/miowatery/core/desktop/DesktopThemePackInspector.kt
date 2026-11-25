package com.water1014.miowatery.core.desktop

import org.json.JSONArray
import org.json.JSONObject

/**
 * Read-only preview model for MioWatery web desktop theme packs.
 *
 * Imported HTML/CSS/JavaScript is intentionally never evaluated by the native app.
 * This inspector only extracts a small, declarative subset for a later native UI.
 */
data class DesktopThemePackPreview(
    val name: String,
    val wallpaper: String?,
    val iconCount: Int,
    val icons: Map<String, String>,
    val widgets: List<DesktopWidgetPreview>,
    val hiddenWidgetIds: Set<String>,
    val placementGroups: Map<String, List<String>>,
    val unsupportedCustomWidgetCount: Int,
    val warnings: List<String>
)

data class DesktopWidgetPreview(
    val id: String,
    val kind: String,
    val title: String,
    val text: String? = null,
    val image: String? = null
)

object DesktopThemePackInspector {
    private const val MAX_JSON_CHARS = 80 * 1024 * 1024
    private const val MAX_IMAGE_CHARS = 12 * 1024 * 1024
    private const val MAX_ICONS = 80
    private const val MAX_WIDGETS = 80
    private val allowedPlacementGroups = setOf("first", "second", "dock", "p3", "p4")

    fun inspect(jsonText: String): DesktopThemePackPreview {
        require(jsonText.length <= MAX_JSON_CHARS) { "美化包文件过大，无法预览" }
        val root = try {
            JSONObject(jsonText)
        } catch (_: Exception) {
            throw IllegalArgumentException("不是有效的 JSON 美化包")
        }
        require(root.optString("app") == "xsj-desktop-theme") { "不支持的美化包格式" }
        require(root.optInt("v", -1) == 1) { "暂不支持此版本的美化包" }
        val desktop = root.optJSONObject("desktop")
            ?: throw IllegalArgumentException("美化包缺少桌面配置")

        val warnings = mutableListOf<String>()
        val wallpaper = safeImage(desktop.optString("wallpaper").takeIf { it.isNotBlank() })
        if (desktop.optString("wallpaper").isNotBlank() && wallpaper == null) {
            warnings += "壁纸地址格式不受支持，导入时会跳过"
        }

        val iconsObject = desktop.optJSONObject("icons")
        val icons = linkedMapOf<String, String>()
        iconsObject?.keys()?.asSequence()?.take(MAX_ICONS)?.forEach { key ->
            val safeKey = safeId(key)
            val image = safeImage(iconsObject.optString(key))
            if (safeKey != null && image != null) icons[safeKey] = image
        }
        val iconCount = icons.size
        if ((iconsObject?.length() ?: 0) > MAX_ICONS) warnings += "图标数量超过限制，超出部分会跳过"

        val widgetsArray = desktop.optJSONArray("widgets") ?: JSONArray()
        val widgets = mutableListOf<DesktopWidgetPreview>()
        val limit = minOf(widgetsArray.length(), MAX_WIDGETS)
        if (widgetsArray.length() > MAX_WIDGETS) warnings += "小组件数量超过限制，超出部分会跳过"
        for (index in 0 until limit) {
            val item = widgetsArray.optJSONObject(index) ?: continue
            val id = safeId(item.optString("id")) ?: continue
            val title = item.optString("title").take(60)
            when (item.optString("kind")) {
                "text" -> widgets += DesktopWidgetPreview(
                    id = id,
                    kind = "text",
                    title = title,
                    text = item.optString("text").take(2_000)
                )
                "clock" -> widgets += DesktopWidgetPreview(
                    id = id,
                    kind = "clock",
                    title = title.ifBlank { "时间" }
                )
                "image" -> {
                    val image = safeImage(item.optString("image"))
                    if (image != null) {
                        widgets += DesktopWidgetPreview(
                            id = id,
                            kind = "image",
                            title = title,
                            image = image
                        )
                    } else {
                        warnings += "有图片组件使用了不受支持的图片地址，已跳过"
                    }
                }
                "css" -> warnings += "发现网页自定义组件引用；原生版不会执行 HTML、CSS 或 JavaScript"
                else -> warnings += "发现暂不支持的组件类型，已跳过"
            }
        }

        val customWidgets = desktop.optJSONArray("wlib")?.length() ?: 0
        if (customWidgets > 0) {
            warnings += "包含 $customWidgets 个网页自定义组件。为保护数据与设备安全，原生版不会运行其中的代码"
        }

        val hidden = linkedSetOf<String>()
        desktop.optJSONArray("hidden")?.let { array ->
            for (index in 0 until array.length()) {
                safeId(array.optString(index))?.let(hidden::add)
            }
        }

        val placement = linkedMapOf<String, List<String>>()
        desktop.optJSONObject("order")?.let { order ->
            allowedPlacementGroups.forEach { group ->
                order.optJSONArray(group)?.let { values ->
                    val ids = buildList {
                        for (index in 0 until minOf(values.length(), 100)) {
                            safeId(values.optString(index))?.let(::add)
                        }
                    }
                    if (ids.isNotEmpty()) placement[group] = ids
                }
            }
        }

        return DesktopThemePackPreview(
            name = root.optString("name").take(40).ifBlank { "未命名美化包" },
            wallpaper = wallpaper,
            iconCount = iconCount,
            icons = icons,
            widgets = widgets,
            hiddenWidgetIds = hidden,
            placementGroups = placement,
            unsupportedCustomWidgetCount = customWidgets,
            warnings = warnings.distinct()
        )
    }

    private fun safeId(value: String): String? =
        value.takeIf { it.matches(Regex("[A-Za-z0-9_:.\\-]{1,80}")) && it !in setOf("__proto__", "constructor", "prototype") }

    private fun safeImage(value: String?): String? {
        val image = value?.trim().orEmpty()
        if (image.isEmpty()) return null
        if (image.length > MAX_IMAGE_CHARS) return null
        if (image.startsWith("https://", ignoreCase = true) &&
            image.matches(Regex("https://[^\\s\\\"'()<>\\\\]{1,2000}", RegexOption.IGNORE_CASE))
        ) return image
        if (image.matches(
                Regex("data:image/(png|jpe?g|gif|webp|avif);base64,[A-Za-z0-9+/=]+", RegexOption.IGNORE_CASE)
            )
        ) return image
        return null
    }
}
