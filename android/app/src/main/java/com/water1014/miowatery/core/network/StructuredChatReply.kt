package com.water1014.miowatery.core.network

import com.google.gson.JsonElement
import com.google.gson.JsonParser

data class StructuredChatMessage(
    val text: String,
    val isNarration: Boolean = false,
    /** Original message object, including quote, translation, recall and sticker fields. */
    val metadataJson: String? = null,
    val isRecalled: Boolean = false
)

data class StructuredChatReply(
    val messages: List<StructuredChatMessage>,
    val thought: String,
    val imagePrompt: String,
    /** Preserve the complete original response so unsupported legacy fields are not discarded. */
    val rawJson: String = "{}",
    val recallLast: Boolean = false
)

/** Reads the JSON response shape used by the original MioWatery chat flow. */
object StructuredChatReplyParser {
    fun parse(raw: String): StructuredChatReply {
        val source = raw.trim()
        val unfenced = source.replace(Regex("^\\s*```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```\\s*$"), "").trim()
        val candidates = listOf(unfenced).plus(
            runCatching {
                val start = unfenced.indexOf('{')
                val end = unfenced.lastIndexOf('}')
                if (start >= 0 && end > start) unfenced.substring(start, end + 1) else ""
            }.getOrDefault("")
        ).filter { it.isNotBlank() }
        val root = candidates.firstNotNullOfOrNull { candidate ->
            runCatching { JsonParser.parseString(candidate).takeIf { it.isJsonObject }?.asJsonObject }.getOrNull()
        } ?: return StructuredChatReply(
            messages = source.takeIf { it.isNotBlank() }?.let { listOf(StructuredChatMessage(it)) }.orEmpty(),
            thought = "",
            imagePrompt = ""
        )

        val messages = mutableListOf<StructuredChatMessage>()
        var recallLast = false
        val array = root.get("messages")
        if (array != null && array.isJsonArray) {
            array.asJsonArray.forEach { item ->
                when {
                    item.isJsonPrimitive -> item.asString.trim().takeIf { it.isNotBlank() }
                        ?.let { messages.add(StructuredChatMessage(it)) }
                    item.isJsonObject -> {
                        val obj = item.asJsonObject
                        if (obj.booleanValue("recall_last") && obj.stringValue("text").isBlank() &&
                            obj.stringValue("content").isBlank() && obj.stringValue("narr").isBlank()
                        ) {
                            recallLast = true
                        }
                        val narration = obj.stringValue("narr")
                        val text = narration.ifBlank {
                            obj.stringValue("text").ifBlank {
                                obj.stringValue("content").ifBlank { obj.stringValue("reply") }
                            }
                        }
                        if (text.isNotBlank()) {
                            messages.add(
                                StructuredChatMessage(
                                    text = text,
                                    isNarration = narration.isNotBlank(),
                                    metadataJson = obj.toString(),
                                    isRecalled = obj.booleanValue("recall") || obj.stringValue("recall").equals("true", true) ||
                                        obj.booleanValue("recalled")
                                )
                            )
                        }
                    }
                }
            }
        }
        if (messages.isEmpty()) {
            val fallback = root.stringValue("reply").ifBlank { source }
            if (fallback.isNotBlank()) messages.add(StructuredChatMessage(fallback))
        }
        return StructuredChatReply(
            messages = messages,
            thought = root.stringValue("thought"),
            imagePrompt = root.stringValue("image_prompt").trim(),
            rawJson = root.toString(),
            recallLast = recallLast
        )
    }

    private fun com.google.gson.JsonObject.booleanValue(key: String): Boolean {
        val value = get(key) ?: return false
        return runCatching { value.asBoolean }.getOrDefault(false)
    }

    private fun com.google.gson.JsonObject.stringValue(key: String): String {
        val value: JsonElement = get(key) ?: return ""
        return if (value.isJsonNull || !value.isJsonPrimitive) "" else runCatching { value.asString }.getOrDefault("")
    }
}
