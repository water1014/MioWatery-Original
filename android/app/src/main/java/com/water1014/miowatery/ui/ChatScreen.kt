package com.water1014.miowatery.ui

import coil3.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.water1014.miowatery.core.database.ConversationEntity
import com.water1014.miowatery.core.database.MessageEntity
import com.water1014.miowatery.core.database.MemoryEntryEntity
import com.water1014.miowatery.core.database.MioDatabase
import com.water1014.miowatery.core.network.ApiConfigStore
import com.water1014.miowatery.core.network.ApiConnectionConfig
import com.water1014.miowatery.core.network.ApiHeaderResolver
import com.water1014.miowatery.core.network.ApiSessionStore
import com.water1014.miowatery.core.network.ChatCompletionClient
import com.water1014.miowatery.core.network.ChatCompletionRequest
import com.water1014.miowatery.core.network.ChatMessagePayload
import com.water1014.miowatery.core.network.DataStoreApiSessionStore
import com.water1014.miowatery.core.network.ImageApiConfigStore
import com.water1014.miowatery.core.network.ImageGenerationClient
import com.water1014.miowatery.core.network.StructuredChatReplyParser
import com.water1014.miowatery.core.network.SavedApiConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.io.ByteArrayOutputStream
import java.io.IOException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONObject

// Native chat entry point: persona title and configured opening line are shown before the first message.
private const val CHAT_PROFILE_ID = "default"
@Composable
fun ChatScreen(onBack: () -> Unit, onOpenMemory: (String) -> Unit = {}, personaId: String = "default", modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val chatBackground = if (darkTheme) Color(0xFF101112) else Color.White
    val chatSurface = if (darkTheme) Color(0xFF1B1D1F) else Color(0xFFF3F4F5)
    val chatText = if (darkTheme) Color(0xFFF2F2F2) else Color(0xFF0F1419)
    val chatSecondary = if (darkTheme) Color(0xFF9AA1A9) else Color(0xFF7B848E)
    val chatDivider = if (darkTheme) Color(0xFF2F3336) else Color(0xFFEAECEE)
    val chatIncoming = if (darkTheme) Color(0xFF292C2F) else Color(0xFFECEEF0)
    val chatFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = chatText,
        unfocusedTextColor = chatText,
        focusedContainerColor = chatSurface,
        unfocusedContainerColor = chatSurface,
        focusedBorderColor = chatSecondary,
        unfocusedBorderColor = chatDivider,
        focusedLabelColor = chatText,
        unfocusedLabelColor = chatSecondary,
        cursorColor = chatText,
        focusedPlaceholderColor = chatSecondary,
        unfocusedPlaceholderColor = chatSecondary
    )
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val personas by dao.observePersonas(CHAT_PROFILE_ID).collectAsState(initial = emptyList())
    val persona = personas.firstOrNull { it.personaId == personaId }
    val conversationId = remember(personaId, persona?.rawLegacyJson) {
        val legacy = runCatching { JSONObject(persona?.rawLegacyJson ?: "{}") }.getOrNull()
        val legacyPersonaId = legacy?.optString("id").orEmpty()
        if (legacyPersonaId.isNotBlank()) {
            val sourceProfileId = legacy?.optString("_nativeSourceProfileId").orEmpty()
                .ifBlank { legacy?.optString("boundUser").orEmpty().ifBlank { "default" } }
            "legacy-chat-" + nativeSafeId(sourceProfileId) + "-" + nativeSafeId(legacyPersonaId)
        } else "native-chat-" + personaId
    }
    val worldbookEntries by dao.observeWorldbookEntries(CHAT_PROFILE_ID, personaId).collectAsState(initial = emptyList())
    val memoryEntries by dao.observeMemoryEntries(conversationId).collectAsState(initial = emptyList())
    val personaPrompt = remember(persona?.rawLegacyJson) {
        runCatching { JSONObject(persona?.rawLegacyJson ?: "{}").optString("prompt") }.getOrDefault("")
    }
    val personaGreeting = remember(persona?.rawLegacyJson) {
        runCatching { JSONObject(persona?.rawLegacyJson ?: "{}").optString("greet").trim() }.getOrDefault("")
    }
    val messages by dao.observeMessages(conversationId).collectAsState(initial = emptyList())
    val configStore = remember { ApiConfigStore(context) }
    val apiConfig by configStore.config.collectAsState(initial = SavedApiConfig())

    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var streamingContent by remember { mutableStateOf("") }
    var streamingReasoning by remember { mutableStateOf("") }
    var sendJob by remember { mutableStateOf<Job?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            attachedImageUri = uri
        }
    }
    var editBaseUrl by remember { mutableStateOf("") }
    var editModel by remember { mutableStateOf("") }
    var editApiKey by remember { mutableStateOf("") }
    var editHeaders by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val visibleMessages = remember(messages, searchOpen, searchQuery) {
        if (searchOpen && searchQuery.isNotBlank()) messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
        else messages
    }

    LaunchedEffect(conversationId, persona) {
        if (persona != null && dao.getConversation(conversationId) == null) {
            val createdAt = System.currentTimeMillis()
            dao.upsertConversation(
                ConversationEntity(
                    id = conversationId,
                    profileId = CHAT_PROFILE_ID,
                    personaId = personaId,
                    title = "新对话",
                    createdAt = createdAt,
                    updatedAt = createdAt
                )
            )
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LaunchedEffect(showSettings) {
        if (showSettings) {
            editBaseUrl = apiConfig.baseUrl
            editModel = apiConfig.model
            editApiKey = apiConfig.apiKey
            editHeaders = apiConfig.customHeadersJson
        }
    }

    fun sendMessage() {
        val text = draft.trim()
        val imageToSend = attachedImageUri
        if ((text.isEmpty() && imageToSend == null) || sending) return
        draft = ""
        attachedImageUri = null
        errorText = null
        sending = true
        streamingContent = ""
        streamingReasoning = ""
        sendJob = scope.launch {
            val now = System.currentTimeMillis()
            val userMessage = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                position = messages.size.toLong(),
                role = "user",
                content = text.ifBlank { "[图片]" },
                timestamp = now,
                reasoning = null,
                attachmentJson = imageToSend?.let { JSONObject().put("type", "image").put("uri", it.toString()).toString() },
                metadataJson = null,
                rawLegacyJson = "{}"
            )
            dao.insertMessages(listOf(userMessage))
            val existingConversation = dao.getConversation(conversationId)
            val originalCreatedAt = existingConversation?.createdAt ?: now
            dao.upsertConversation(
                ConversationEntity(
                    id = conversationId,
                    profileId = CHAT_PROFILE_ID,
                    personaId = personaId,
                    title = text.take(36),
                    createdAt = originalCreatedAt,
                    updatedAt = now
                )
            )
            if (apiConfig.baseUrl.isBlank() || apiConfig.model.isBlank() || apiConfig.apiKey.isBlank()) {
                errorText = "消息已保存在本机。请点右上角设置 API 地址、模型和密钥后再发送。"
                sending = false
                return@launch
            }
            try {
                val activeWorldbook = worldbookEntries.filter { entry ->
                    if (!entry.enabled) return@filter false
                    val raw = runCatching { JSONObject(entry.rawLegacyJson) }.getOrNull()
                    val trigger = raw?.optString("trigger", "global") ?: "global"
                    if (trigger != "keyword") true
                    else {
                        val keys = raw?.optString("keys").orEmpty()
                            .split(",", "，", "、", ";", "；")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        keys.any { text.contains(it, ignoreCase = true) }
                    }
                }
                val beforeWorldbook = activeWorldbook.filter {
                    runCatching { JSONObject(it.rawLegacyJson).optString("position", "after") }.getOrDefault("after") == "before"
                }
                val afterWorldbook = activeWorldbook - beforeWorldbook.toSet()
                val activeMemories = memoryEntries
                val imageConfig = ImageApiConfigStore(context).load()
                val systemPrompt = buildString {
                    beforeWorldbook.forEach { append("【世界书：").append(it.title).append("】\n").append(it.content).append("\n\n") }
                    if (personaPrompt.isNotBlank()) append(personaPrompt).append("\n\n")
                    val manualMemories = activeMemories.filter { it.type == "traditional" }
                    val memoryNotes = activeMemories.filter { it.type == "note" }.take(8)
                    if (manualMemories.isNotEmpty()) {
                        append("【传统记忆】以下是用户为当前聊天手动保存的长期信息，每次回复都应参考：\n")
                        manualMemories.forEach { append("· ").append(it.text.take(2000)).append("\n") }
                        append("\n")
                    }
                    if (memoryNotes.isNotEmpty()) {
                        append("【聊天总结纸条】以下内容是当前聊天的历史记忆，请在相关话题中自然参考，不要把旧事说成刚刚发生：\n")
                        memoryNotes.take(8).forEach { append("· ").append(it.text.take(600)).append("\n") }
                        append("\n")
                    }
                    val olderMemoryNotes = activeMemories.filter { it.type == "note" }.drop(8)
                    if (olderMemoryNotes.isNotEmpty()) {
                        val recallIndex = ((System.currentTimeMillis() / 86400000L) % olderMemoryNotes.size).toInt()
                        val recalled = olderMemoryNotes[recallIndex]
                        val recalledDate = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                            .format(java.util.Date(recalled.createdAt))
                        append("【今天偶然想起的旧事】这是较早的聊天记忆，不要当成刚刚发生；只有话题相关时才自然提及：\n")
                        append("记于 ").append(recalledDate).append("：").append(recalled.text.take(300)).append("\n\n")
                    }
                    afterWorldbook.forEach { append("【世界书：").append(it.title).append("】\n").append(it.content).append("\n\n") }
                    append("在保持角色人设和自然对话的前提下，请一次性输出严格 JSON，不要 Markdown 代码块，格式为 {\"messages\":[\"第一条独立聊天消息\",\"第二条独立聊天消息\"],\"reply\":\"兼容字段\",\"thought\":\"角色此刻未说出口的内心想法\",\"image_prompt\":\"\"}。messages 是角色按真实聊天节奏连续发送的消息数组：结合语境，适合分开发的内容就拆成 2-5 条短消息，每条只放一个自然聊天气泡；不要把几句话用换行或一大段塞进同一个气泡。只有确实一句就能说清时才只输出一条。不要机械地每次都拆成多条。reply 可作为 messages 的兼容备用字段。thought 是简短内心活动。")
                    append(if (imageConfig.enabled) {
                        val base = "【发图规则】只要用户想看你的照片、自拍或某个画面（比如“发张照片”“给我看看”“拍给我”“你现在什么样”），或者你自己决定发图/发自拍，就必须填写 image_prompt，系统才会真的生成并发出图片；绝不能只在 messages 或旁白里写“发了一张照片”却把 image_prompt 留空。同时 messages 里正常接话，不要在文字里描述图片内容。没有这类场景时 image_prompt 留空。"
                        if (imageConfig.provider == "novelai") base + "当前生图引擎是 NovelAI：image_prompt 必须写成英文 Danbooru 风格标签，用英文逗号分隔，例如“1boy, solo, messy hair, half-closed eyes, selfie, indoors, upper body”；按角色人设写人数/性别、发型发色、表情、动作、场景、镜头；不要写中文、不要写完整句子、不要写 masterpiece 之类质量词（系统会自动加前缀）。"
                        else base + "image_prompt 用自然语言详细描述画面：人物外貌、表情、动作、场景、镜头、光线。"
                    } else "若用户明确要求生成/发图片，image_prompt 填画面描述，否则留空。")
                    append("如果某条回复是针对对话里某一条具体消息的回应，可以偶尔把这一条写成对象 {\"text\":\"回复内容\",\"quote\":\"被引用消息的原文片段，原样摘抄，不超过30字\"}，其余条目仍写成普通字符串。")
                }.trim()
                val memoryPrefs = context.getSharedPreferences("mio_memory_settings", android.content.Context.MODE_PRIVATE)
                val configuredContextCount = memoryPrefs.getInt("context_count_$conversationId", 0)
                val recentMessages = (messages.map { it } + userMessage).let { all ->
                    if (configuredContextCount > 0) all.takeLast(configuredContextCount) else all
                }
                val history = withContext(Dispatchers.IO) {
                    buildList {
                        if (systemPrompt.isNotBlank()) add(ChatMessagePayload.text("system", systemPrompt))
                        recentMessages.forEach { message ->
                            val attachmentUri = runCatching {
                                JSONObject(message.attachmentJson ?: "{}")
                                    .optString("uri")
                                    .takeIf { it.isNotBlank() }
                            }.getOrNull()
                            if (attachmentUri == null) {
                                add(ChatMessagePayload.text(message.role, message.content))
                            } else {
                                val dataUrl = try {
                                    encodeChatImage(context, android.net.Uri.parse(attachmentUri))
                                } catch (failure: Exception) {
                                    if (message.id == userMessage.id) {
                                        throw IOException("图片读取失败，请重新选择图片后发送。", failure)
                                    }
                                    null
                                }
                                if (dataUrl != null) {
                                    add(ChatMessagePayload.image(message.role, message.content, dataUrl))
                                } else {
                                    add(ChatMessagePayload.text(message.role, message.content))
                                }
                            }
                        }
                    }
                }
                val result = withContext(Dispatchers.IO) {
                    val sessions: ApiSessionStore = DataStoreApiSessionStore(context)
                    val client = ChatCompletionClient(ApiHeaderResolver(sessions))
                    client.complete(
                        ApiConnectionConfig(
                            baseUrl = apiConfig.baseUrl,
                            apiKey = apiConfig.apiKey,
                            customHeadersJson = apiConfig.customHeadersJson,
                            userProfileId = CHAT_PROFILE_ID,
                            conversationId = conversationId
                        ),
                        ChatCompletionRequest(
                            model = apiConfig.model,
                            messages = history,
                            stream = true
                        ),
                        onDelta = { delta ->
                            scope.launch(Dispatchers.Main.immediate) {
                                streamingContent += delta.content
                                streamingReasoning += delta.reasoning
                            }
                        }
                    )
                }
                val reply = result.content.trim()
                val structuredReply = StructuredChatReplyParser.parse(reply)
                if (structuredReply.messages.isEmpty()) {
                    errorText = "模型返回了空内容，请检查模型配置或服务商响应。"
                } else {
                    val assistantNow = System.currentTimeMillis()
                    val assistantMessages = structuredReply.messages.mapIndexed { index, item ->
                        val thought = if (index == 0 && !item.isNarration) {
                            result.reasoning.ifBlank { structuredReply.thought }.takeIf { it.isNotBlank() }
                        } else null
                        MessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            position = messages.size.toLong() + 1 + index,
                            role = "assistant",
                            content = item.text,
                            timestamp = assistantNow + index,
                            reasoning = thought,
                            attachmentJson = null,
                            metadataJson = null,
                            rawLegacyJson = "{}"
                        )
                    }
                    dao.insertMessages(assistantMessages)
                    dao.upsertConversation(
                        ConversationEntity(
                            id = conversationId,
                            profileId = CHAT_PROFILE_ID,
                            personaId = personaId,
                            title = text.take(36),
                            createdAt = originalCreatedAt,
                            updatedAt = assistantNow + assistantMessages.size
                        )
                    )
                    if (imageConfig.enabled && structuredReply.imagePrompt.isNotBlank()) {
                        try {
                            val generatedUri = withContext(Dispatchers.IO) {
                                ImageGenerationClient(context).generate(structuredReply.imagePrompt, imageConfig)
                            }
                            dao.insertMessages(listOf(
                                MessageEntity(
                                    id = UUID.randomUUID().toString(),
                                    conversationId = conversationId,
                                    position = messages.size.toLong() + 1 + assistantMessages.size,
                                    role = "assistant",
                                    content = "",
                                    timestamp = System.currentTimeMillis(),
                                    reasoning = "我把刚才描述的画面生成出来了。",
                                    attachmentJson = JSONObject().put("type", "image").put("uri", generatedUri).toString(),
                                    metadataJson = null,
                                    rawLegacyJson = "{}"
                                )
                            ))
                        } catch (imageError: Exception) {
                            dao.insertMessages(listOf(
                                MessageEntity(
                                    id = UUID.randomUUID().toString(),
                                    conversationId = conversationId,
                                    position = messages.size.toLong() + 1 + assistantMessages.size,
                                    role = "assistant",
                                    content = "图片生成失败：" + (imageError.message ?: "未知错误"),
                                    timestamp = System.currentTimeMillis(),
                                    reasoning = null,
                                    attachmentJson = null,
                                    metadataJson = null,
                                    rawLegacyJson = "{}"
                                )
                            ))
                        }
                    }
                    // Optional per-chat automatic memory generation. Failures never block the reply.
                    val memoryPrefs = context.getSharedPreferences("mio_memory_settings", android.content.Context.MODE_PRIVATE)
                    val autoEnabled = memoryPrefs.getBoolean("auto_summary_$conversationId", false)
                    val summaryInterval = memoryPrefs.getInt("auto_summary_interval_$conversationId", 200).coerceIn(20, 2000)
                    val allMessages = dao.getMessagesForConversation(conversationId)
                    val lastSummarizedCount = memoryPrefs.getInt("last_auto_summary_count_$conversationId", 0)
                    if (autoEnabled && allMessages.size - lastSummarizedCount >= summaryInterval) {
                        runCatching {
                            val summaryInput = allMessages.drop(lastSummarizedCount.coerceIn(0, allMessages.size))
                                .joinToString("\n") { message ->
                                    (if (message.role == "user") "我：" else "角色：") + message.content
                                }.takeLast(24000)
                            if (summaryInput.isNotBlank()) {
                                val customInstruction = memoryPrefs.getString("summary_instruction_$conversationId", "").orEmpty().trim()
                                val summarySystem = buildString {
                                    append("你是聊天记忆整理助手。请把聊天记录整理成未来聊天可用的长期记忆纸条。保留明确事实、关系变化、称呼、稳定偏好、共同经历和未完成约定；区分双方，不确定内容不要编造。简体中文，分点，600字以内，只输出记忆内容。")
                                    if (customInstruction.isNotBlank()) append("\n\n用户额外总结规则：\n").append(customInstruction)
                                }
                                val summaryResult = withContext(Dispatchers.IO) {
                                    val sessions: ApiSessionStore = DataStoreApiSessionStore(context)
                                    val client = ChatCompletionClient(ApiHeaderResolver(sessions))
                                    client.complete(
                                        ApiConnectionConfig(
                                            baseUrl = apiConfig.baseUrl,
                                            apiKey = apiConfig.apiKey,
                                            customHeadersJson = apiConfig.customHeadersJson,
                                            userProfileId = CHAT_PROFILE_ID,
                                            conversationId = conversationId
                                        ),
                                        ChatCompletionRequest(
                                            model = apiConfig.model,
                                            messages = listOf(
                                                ChatMessagePayload.text("system", summarySystem),
                                                ChatMessagePayload.text("user", summaryInput)
                                            ),
                                            stream = false
                                        )
                                    )
                                }
                                val memoryText = summaryResult.content.trim()
                                if (memoryText.isNotBlank()) {
                                    dao.upsertMemoryEntry(
                                        MemoryEntryEntity(
                                            id = UUID.randomUUID().toString(),
                                            conversationId = conversationId,
                                            type = "note",
                                            text = memoryText,
                                            createdAt = System.currentTimeMillis(),
                                            rangeStart = lastSummarizedCount + 1,
                                            rangeEnd = allMessages.size,
                                            isMerged = false
                                        )
                                    )
                                    memoryPrefs.edit().putInt("last_auto_summary_count_$conversationId", allMessages.size).apply()
                                }
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                errorText = failure.message ?: "请求失败，请检查网络与 API 配置。"
            } finally {
                sending = false
                sendJob = null
                streamingContent = ""
                streamingReasoning = ""
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(chatBackground).imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).background(chatBackground).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onBack).padding(top = 1.dp),
                color = chatText,
                fontSize = 34.sp,
                textAlign = TextAlign.Center
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(persona?.name?.takeIf { it.isNotBlank() } ?: "聊天", color = chatText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    if (apiConfig.model.isBlank()) "尚未配置模型" else apiConfig.model,
                    color = chatSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⌕",
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                        searchOpen = !searchOpen
                        if (!searchOpen) searchQuery = ""
                    }.padding(horizontal = 9.dp, vertical = 7.dp),
                    color = chatText,
                    fontSize = 22.sp
                )
                Text(
                    "⋯",
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { showMoreMenu = true }.padding(horizontal = 8.dp, vertical = 7.dp),
                    color = chatText,
                    fontSize = 12.sp
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(chatDivider))
        if (searchOpen) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("搜索当前聊天") },
                    colors = chatFieldColors
                )
                TextButton(onClick = { searchOpen = false; searchQuery = "" }) { Text("关闭") }
            }
        }

        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(66.dp).clip(RoundedCornerShape(22.dp)).background(chatSurface),
                    contentAlignment = Alignment.Center
                ) { Text("M", color = chatText, fontSize = 27.sp, fontWeight = FontWeight.Light) }
                Spacer(Modifier.height(18.dp))
                Text(
                    if (personaGreeting.isNotBlank()) persona?.name ?: "开场白" else "开始一段对话",
                    color = chatText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    personaGreeting.ifBlank {
                        if (apiConfig.model.isBlank()) "先配置兼容 OpenAI 的 API，再发送第一条消息。" else "发送消息，开启你们的故事。"
                    },
                    color = chatSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                )
                if (apiConfig.model.isBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text("配置 API", modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(chatSurface).clickable { showSettings = true }.padding(horizontal = 16.dp, vertical = 9.dp), color = chatText, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text("今天", modifier = Modifier.fillMaxWidth(), color = chatSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                if (visibleMessages.isEmpty()) {
                    item { Text("没有找到匹配的消息", modifier = Modifier.fillMaxWidth().padding(28.dp), color = chatSecondary, fontSize = 12.sp, textAlign = TextAlign.Center) }
                }
                itemsIndexed(visibleMessages, key = { _, message -> message.id }) { index, message ->
                    val firstInGroup = index == 0 || visibleMessages[index - 1].role != message.role
                    ChatMessageBubble(message, firstInGroup, persona?.avatar, darkTheme, chatIncoming, chatText, chatSecondary) { selectedMessage = it }
                }
                if (sending) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = chatSecondary)
                            Text(streamingContent.ifBlank { "正在回复…" }, color = chatSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        errorText?.let { message ->
            Text(message, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), color = Color(0xFF9A5B35), fontSize = 11.sp, lineHeight = 16.sp)
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(chatBackground).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
        ) {
            attachedImageUri?.let { uri ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = uri, contentDescription = "待发送图片", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(10.dp))
                    Text("已选择图片", color = chatSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("移除", color = chatSecondary, fontSize = 12.sp, modifier = Modifier.clickable { attachedImageUri = null }.padding(8.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(chatSurface).padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text("＋", color = chatSecondary, fontSize = 23.sp, modifier = Modifier.clip(CircleShape).clickable { showAttachmentMenu = true }.padding(horizontal = 5.dp, vertical = 2.dp))
                Spacer(Modifier.width(9.dp))
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).padding(vertical = 7.dp),
                    textStyle = TextStyle(color = chatText, fontSize = 14.sp, lineHeight = 20.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(chatText),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("发送消息…", color = chatSecondary, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                        .background(if (sending || draft.isNotBlank() || attachedImageUri != null) Color(0xFF111111) else chatDivider)
                        .clickable(enabled = sending || draft.isNotBlank() || attachedImageUri != null) {
                            if (sending) {
                                sendJob?.cancel()
                                sendJob = null
                                sending = false
                                streamingContent = ""
                                streamingReasoning = ""
                            } else sendMessage()
                        },
                    contentAlignment = Alignment.Center
                ) { Text(if (sending) "■" else "↑", color = Color.White, fontSize = if (sending) 13.sp else 19.sp, fontWeight = FontWeight.Medium) }
            }
            Text(
                "消息保存在本机 · API 密钥由 Android Keystore 加密",
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                color = chatSecondary, fontSize = 9.sp, textAlign = TextAlign.Center
            )
        }
    }

    if (showAttachmentMenu) {
        AlertDialog(
            containerColor = chatSurface,
            titleContentColor = chatText,
            textContentColor = chatText,
            onDismissRequest = { showAttachmentMenu = false },
            title = { Text("添加内容") },
            text = { Text("选择一张图片添加到当前聊天。图片会保存在本机聊天记录中。") },
            confirmButton = {
                TextButton(onClick = { showAttachmentMenu = false; imagePicker.launch(arrayOf("image/*")) }) { Text("选择图片") }
            },
            dismissButton = { TextButton(onClick = { showAttachmentMenu = false }) { Text("取消") } }
        )
    }

    if (showMoreMenu) {
        AlertDialog(
            containerColor = chatSurface,
            titleContentColor = chatText,
            textContentColor = chatText,
            onDismissRequest = { showMoreMenu = false },
            title = { Text("聊天操作") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showMoreMenu = false; showSettings = true }) { Text("聊天设置与 API") }
                    TextButton(onClick = { showMoreMenu = false; onOpenMemory(conversationId) }) { Text("记忆管理") }
                    TextButton(onClick = { showMoreMenu = false; showClearConfirm = true }) { Text("清空当前对话", color = Color(0xFFD93636)) }
                }
            },
            confirmButton = { TextButton(onClick = { showMoreMenu = false }) { Text("完成") } }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            containerColor = chatSurface,
            titleContentColor = chatText,
            textContentColor = chatText,
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空当前对话？") },
            text = { Text("这会删除当前角色的全部聊天消息，无法撤销。记忆和世界书不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (sending) sendJob?.cancel()
                        sending = false
                        sendJob = null
                        streamingContent = ""
                        streamingReasoning = ""
                        dao.clearMessages(conversationId)
                        showClearConfirm = false
                    }
                }) { Text("清空", color = Color(0xFFD93636)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("取消") } }
        )
    }

    selectedMessage?.let { message ->
        AlertDialog(
            containerColor = chatSurface,
            titleContentColor = chatText,
            textContentColor = chatText,
            onDismissRequest = { selectedMessage = null },
            title = { Text("消息操作") },
            text = { Text(if (message.content.isBlank()) "图片消息" else message.content.take(240)) },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(message.content))
                    selectedMessage = null
                }) { Text("复制") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch { dao.deleteMessage(message.id, conversationId) }
                        selectedMessage = null
                    }) { Text("删除", color = Color(0xFFD93636)) }
                    TextButton(onClick = { selectedMessage = null }) { Text("取消") }
                }
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            containerColor = chatSurface,
            titleContentColor = chatText,
            textContentColor = chatText,
            onDismissRequest = { showSettings = false },
            title = { Text("聊天设置") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("记忆", color = chatText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(chatSurface)
                            .clickable { showSettings = false; onOpenMemory(conversationId) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("记忆管理", color = chatText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("生成总结条目、编辑和删除记忆", color = chatSecondary, fontSize = 11.sp)
                        }
                        Text("›", color = Color(0xFF8A929B), fontSize = 20.sp)
                    }
                    Text("模型 API", color = chatText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(editBaseUrl, { editBaseUrl = it }, label = { Text("API 地址") }, placeholder = { Text("https://example.com/v1") }, singleLine = true, colors = chatFieldColors)
                    OutlinedTextField(editModel, { editModel = it }, label = { Text("模型名称") }, singleLine = true, colors = chatFieldColors)
                    OutlinedTextField(editApiKey, { editApiKey = it }, label = { Text("API 密钥") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, colors = chatFieldColors)
                    OutlinedTextField(editHeaders, { editHeaders = it }, label = { Text("自定义请求头（JSON，可选）") }, placeholder = { Text("{\"x-opencode-session\": \"\$SESSION_ID\"}") }, minLines = 2, maxLines = 4, colors = chatFieldColors)
                    Text("OpenCode Go 可在自定义请求头中添加服务商要求的请求头。会话占位符会按当前对话生成稳定 ID。", color = chatSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        configStore.save(SavedApiConfig(editBaseUrl, editModel, editApiKey, editHeaders))
                        showSettings = false
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun ChatMessageBubble(message: MessageEntity, firstInGroup: Boolean, personaAvatar: String?, darkTheme: Boolean, incomingColor: Color, messageTextColor: Color, secondaryTextColor: Color, onMessageLongPress: (MessageEntity) -> Unit) {
    var showReasoning by remember(message.id) { mutableStateOf(false) }
    val fromUser = message.role == "user"
    val avatar = @Composable {
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(if (firstInGroup) if (darkTheme) Color(0xFF303438) else Color(0xFFF0F1F2) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (firstInGroup) {
                if (!fromUser && !personaAvatar.isNullOrBlank()) {
                    AsyncImage(model = personaAvatar, contentDescription = "角色头像", modifier = Modifier.fillMaxSize().clip(CircleShape))
                } else {
                    Text(if (fromUser) "我" else "M", color = if (darkTheme) Color(0xFFD0D5DA) else Color(0xFF555D66), fontSize = 13.sp)
                }
            }
        }
    }
    val bubbleShape = RoundedCornerShape(
        topStart = if (!fromUser && firstInGroup) 4.dp else 16.dp,
        topEnd = if (fromUser && firstInGroup) 4.dp else 16.dp,
        bottomEnd = 16.dp,
        bottomStart = 16.dp
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = if (firstInGroup) 10.dp else 0.dp),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!fromUser) {
            avatar()
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier.widthIn(max = 270.dp)
                .clip(bubbleShape)
                .background(if (fromUser) Color(0xFF111111) else incomingColor)
                .combinedClickable(onClick = {}, onLongClick = { onMessageLongPress(message) })
                .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val attachmentUri = remember(message.attachmentJson) {
                    runCatching { JSONObject(message.attachmentJson ?: "{}").optString("uri").takeIf { it.isNotBlank() } }.getOrNull()
                }
                if (attachmentUri != null) {
                    AsyncImage(model = attachmentUri, contentDescription = "聊天图片", modifier = Modifier.widthIn(max = 210.dp).height(150.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                }
                if (message.content.isNotBlank() && message.content != "[图片]") Text(
                    message.content,
                    color = if (fromUser) Color.White else messageTextColor,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                if (!fromUser && !message.reasoning.isNullOrBlank()) {
                    Text(
                        if (showReasoning) "收起思考过程 ▴" else "查看思考过程 ▾",
                        modifier = Modifier.clickable { showReasoning = !showReasoning },
                        color = secondaryTextColor,
                        fontSize = 10.sp
                    )
                    if (showReasoning) {
                        Text(
                            message.reasoning.orEmpty(),
                            color = secondaryTextColor,
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
        if (fromUser) {
            Spacer(Modifier.width(8.dp))
            avatar()
        }
    }
}


private fun nativeSafeId(value: String): String =
    value.lowercase(java.util.Locale.ROOT)
        .replace(Regex("[^a-z0-9_-]"), "_")
        .take(64)
        .ifBlank { "item" }


private fun encodeChatImage(context: android.content.Context, uri: android.net.Uri): String {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    } ?: throw IOException("无法打开图片文件")
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("图片格式无法识别")

    var sampleSize = 1
    val longestSide = maxOf(bounds.outWidth, bounds.outHeight)
    while (longestSide / sampleSize > 1600) sampleSize *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = resolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    } ?: throw IOException("图片解码失败")

    val output = ByteArrayOutputStream()
    try {
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)) {
            throw IOException("图片压缩失败")
        }
    } finally {
        bitmap.recycle()
    }
    val bytes = output.toByteArray()
    if (bytes.size > 5 * 1024 * 1024) {
        throw IOException("图片压缩后仍超过 5 MB，请选择较小的图片")
    }
    return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}
