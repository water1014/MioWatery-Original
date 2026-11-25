package com.water1014.miowatery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import com.water1014.miowatery.core.database.ConversationEntity
import com.water1014.miowatery.core.database.MessageEntity
import com.water1014.miowatery.core.database.MioDatabase
import com.water1014.miowatery.core.network.ApiConfigStore
import com.water1014.miowatery.core.network.ApiConnectionConfig
import com.water1014.miowatery.core.network.ApiHeaderResolver
import com.water1014.miowatery.core.network.ApiSessionStore
import com.water1014.miowatery.core.network.ChatCompletionClient
import com.water1014.miowatery.core.network.ChatCompletionRequest
import com.water1014.miowatery.core.network.ChatMessagePayload
import com.water1014.miowatery.core.network.DataStoreApiSessionStore
import com.water1014.miowatery.core.network.SavedApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val CHAT_PROFILE_ID = "default"
private const val CHAT_PERSONA_ID = "default"

@Composable
fun ChatScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conversationId = remember { "native-chat-" + UUID.randomUUID().toString() }
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val messages by dao.observeMessages(conversationId).collectAsState(initial = emptyList())
    val configStore = remember { ApiConfigStore(context) }
    val apiConfig by configStore.config.collectAsState(initial = SavedApiConfig())

    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var editBaseUrl by remember { mutableStateOf("") }
    var editModel by remember { mutableStateOf("") }
    var editApiKey by remember { mutableStateOf("") }
    var editHeaders by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        dao.upsertConversation(
            ConversationEntity(
                id = conversationId,
                profileId = CHAT_PROFILE_ID,
                personaId = CHAT_PERSONA_ID,
                title = "新对话",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
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
        if (text.isEmpty() || sending) return
        draft = ""
        errorText = null
        sending = true
        scope.launch {
            val now = System.currentTimeMillis()
            val userMessage = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                position = messages.size.toLong(),
                role = "user",
                content = text,
                timestamp = now,
                reasoning = null,
                attachmentJson = null,
                metadataJson = null,
                rawLegacyJson = "{}"
            )
            dao.insertMessages(listOf(userMessage))
            dao.upsertConversation(
                ConversationEntity(
                    id = conversationId,
                    profileId = CHAT_PROFILE_ID,
                    personaId = CHAT_PERSONA_ID,
                    title = text.take(36),
                    createdAt = now,
                    updatedAt = now
                )
            )
            if (apiConfig.baseUrl.isBlank() || apiConfig.model.isBlank() || apiConfig.apiKey.isBlank()) {
                errorText = "消息已保存在本机。请点右上角设置 API 地址、模型和密钥后再发送。"
                sending = false
                return@launch
            }
            try {
                val history = (messages.map { it } + userMessage).map {
                    ChatMessagePayload.text(it.role, it.content)
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
                            stream = false
                        )
                    )
                }
                val reply = result.content.trim()
                if (reply.isBlank()) {
                    errorText = "模型返回了空内容，请检查模型配置或服务商响应。"
                } else {
                    val assistantNow = System.currentTimeMillis()
                    dao.insertMessages(
                        listOf(
                            MessageEntity(
                                id = UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                position = messages.size.toLong() + 1,
                                role = "assistant",
                                content = reply,
                                timestamp = assistantNow,
                                reasoning = result.reasoning.takeIf { it.isNotBlank() },
                                attachmentJson = null,
                                metadataJson = null,
                                rawLegacyJson = "{}"
                            )
                        )
                    )
                    dao.upsertConversation(
                        ConversationEntity(
                            id = conversationId,
                            profileId = CHAT_PROFILE_ID,
                            personaId = CHAT_PERSONA_ID,
                            title = text.take(36),
                            createdAt = now,
                            updatedAt = assistantNow
                        )
                    )
                }
            } catch (failure: Exception) {
                errorText = failure.message ?: "请求失败，请检查网络与 API 配置。"
            } finally {
                sending = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(Color.White).imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onBack).padding(top = 1.dp),
                color = Color(0xFF0F1419),
                fontSize = 34.sp,
                textAlign = TextAlign.Center
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("聊天", color = Color(0xFF0F1419), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (apiConfig.model.isBlank()) "尚未配置模型" else apiConfig.model,
                    color = Color(0xFF8A929B),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Text(
                "设置",
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { showSettings = true }.padding(horizontal = 8.dp, vertical = 7.dp),
                color = Color(0xFF0F1419),
                fontSize = 12.sp
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEAECEE)))

        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(66.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFF3F4F5)),
                    contentAlignment = Alignment.Center
                ) { Text("M", color = Color(0xFF0F1419), fontSize = 27.sp, fontWeight = FontWeight.Light) }
                Spacer(Modifier.height(18.dp))
                Text("开始一段对话", color = Color(0xFF0F1419), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                Text(
                    if (apiConfig.model.isBlank()) "先配置兼容 OpenAI 的 API，再发送第一条消息。" else "发送消息，开启你们的故事。",
                    color = Color(0xFF7B848E), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                )
                if (apiConfig.model.isBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text("配置 API", modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFFF3F4F5)).clickable { showSettings = true }.padding(horizontal = 16.dp, vertical = 9.dp), color = Color(0xFF0F1419), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("今天", modifier = Modifier.fillMaxWidth(), color = Color(0xFF9AA1A9), fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                    val firstInGroup = index == 0 || messages[index - 1].role != message.role
                    ChatMessageBubble(message, firstInGroup)
                }
                if (sending) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = Color(0xFF737B84))
                            Text("正在回复…", color = Color(0xFF8A929B), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        errorText?.let { message ->
            Text(message, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), color = Color(0xFF9A5B35), fontSize = 11.sp, lineHeight = 16.sp)
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFFF3F4F5)).padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text("＋", color = Color(0xFF59616B), fontSize = 23.sp, modifier = Modifier.padding(bottom = 1.dp))
                Spacer(Modifier.width(9.dp))
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).padding(vertical = 7.dp),
                    textStyle = TextStyle(color = Color(0xFF0F1419), fontSize = 14.sp, lineHeight = 20.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0F1419)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("发送消息…", color = Color(0xFF9AA1A9), fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                        .background(if (draft.isBlank() || sending) Color(0xFFD9DDE1) else Color(0xFF111111))
                        .clickable(enabled = draft.isNotBlank() && !sending) { sendMessage() },
                    contentAlignment = Alignment.Center
                ) { Text(if (sending) "…" else "↑", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium) }
            }
            Text(
                "消息保存在本机 · API 密钥由 Android Keystore 加密",
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                color = Color(0xFFA3AAB1), fontSize = 9.sp, textAlign = TextAlign.Center
            )
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("模型 API 设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editBaseUrl, { editBaseUrl = it }, label = { Text("API 地址") }, placeholder = { Text("https://example.com/v1") }, singleLine = true)
                    OutlinedTextField(editModel, { editModel = it }, label = { Text("模型名称") }, singleLine = true)
                    OutlinedTextField(editApiKey, { editApiKey = it }, label = { Text("API 密钥") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(editHeaders, { editHeaders = it }, label = { Text("自定义请求头（JSON，可选）") }, placeholder = { Text("{\"x-opencode-session\": \"\$SESSION_ID\"}") }, minLines = 2, maxLines = 4)
                    Text("OpenCode Go 可在自定义请求头中添加服务商要求的请求头。会话占位符会按当前对话生成稳定 ID。", color = Color(0xFF7B848E), fontSize = 11.sp, lineHeight = 16.sp)
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
private fun ChatMessageBubble(message: MessageEntity, firstInGroup: Boolean) {
    val fromUser = message.role == "user"
    val avatar = @Composable {
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(if (firstInGroup) Color(0xFFF0F1F2) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (firstInGroup) {
                Text(if (fromUser) "我" else "M", color = Color(0xFF555D66), fontSize = 13.sp)
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
        modifier = Modifier.fillMaxWidth(),
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
                .background(if (fromUser) Color(0xFF111111) else Color(0xFFECEEF0))
                .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Text(
                message.content,
                color = if (fromUser) Color.White else Color(0xFF0F1419),
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
        if (fromUser) {
            Spacer(Modifier.width(8.dp))
            avatar()
        }
    }
}
