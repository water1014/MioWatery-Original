package com.water1014.miowatery.ui

import java.io.File
import java.net.URI
import java.util.zip.ZipFile

internal fun parseCoreadEpub(file: File): List<Pair<String, String>> {
    ZipFile(file).use { zip ->
        fun read(path: String): String {
            val entry = zip.getEntry(path) ?: return ""
            return zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        val container = read("META-INF/container.xml")
        val opfPath = Regex("""full-path=["']([^"']+)["']""").find(container)?.groupValues?.get(1)
            ?: error("EPUB 缺少书籍目录")
        val opf = read(opfPath)
        val manifest = Regex("""<item\b([^>]*)/?>""", RegexOption.IGNORE_CASE).findAll(opf).associate { m ->
            val attrs = Regex("""([\w:-]+)=["']([^"']*)["']""").findAll(m.groupValues[1])
                .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
            (attrs["id"] ?: "") to (attrs["href"] ?: "")
        }
        val spine = Regex("""<itemref\b([^>]*)/?>""", RegexOption.IGNORE_CASE).findAll(opf).mapNotNull { m ->
            Regex("""idref=["']([^"']+)["']""").find(m.groupValues[1])?.groupValues?.get(1)
        }.toList()
        val result = mutableListOf<Pair<String, String>>()
        spine.forEachIndexed { index, id ->
            val href = manifest[id]?.takeIf { it.isNotBlank() } ?: return@forEachIndexed
            val path = URI(opfPath).resolve(href.substringBefore('#')).normalize().path.removePrefix("/")
            val html = read(path)
            if (html.isBlank()) return@forEachIndexed
            val headingSource = Regex("""<h[1-3][^>]*>(.*?)</h[1-3]>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(html)?.groupValues?.get(1)
            val title = headingSource?.let { epubPlainText(it) }?.take(70)?.takeIf { it.isNotBlank() }
                ?: "第 ${index + 1} 章"
            val body = epubPlainText(html)
            if (body.isNotBlank()) result += title to body
        }
        if (result.isEmpty()) error("没有找到 EPUB 正文")
        return result
    }
}

private fun epubPlainText(html: String): String = html
    .replace(Regex("""<(script|style|head)[^>]*>.*?</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
    .replace(Regex("""<br\s*/?>|</(p|div|h[1-6]|li|section|article)>""", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("""<[^>]+>"""), " ")
    .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
    .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
    .replace(Regex("""[ \t]+"""), " ").replace(Regex(""" *\n *"""), "\n").trim()
