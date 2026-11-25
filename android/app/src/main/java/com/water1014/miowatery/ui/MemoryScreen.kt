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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.water1014.miowatery.core.network.SavedApiConfig
import com.water1014.miowatery.core.database.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    conversationId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val scope = rememberCoroutineScope()
    val entries by dao.observeMemoryEntries(conversationId).collectAsState(initial = emptyList())
    val messages by dao.observeMessages(conversationId).collectAsState(initial = emptyList())
    val configStore = remember { ApiConfigStore(context) }
    val apiConfig by configStore.config.collectAsState(initial = SavedApiConfig())
    var rangeStart by remember { mutableStateOf("1") }
    var rangeEnd by remember { mutableStateOf("0") }
    var generating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<MemoryEntryEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("traditional") }

    fun generateSummary() {
        if (generating) return
        val start = (rangeStart.toIntOrNull() ?: 1).coerceAtLeast(1)
        val end = (rangeEnd.toIntOrNull() ?: messages.size).coerceAtMost(messages.size)
        if (end < start || messages.isEmpty()) {
            generationError = "请选择有效的聊天消息范围"
            return
        }
        if (apiConfig.baseUrl.isBlank() || apiConfig.apiKey.isBlank() || apiConfig.model.isBlank()) {
            generationError = "请先在聊天页面配置模型 API"
            return
        }
        generating = true
        generationError = null
        scope.launch {
            try {
                val transcript = messages.subList(start - 1, end).joinToString("\n") { message ->
                    (if (message.role == "user") "我：" else "角色：") + message.content
                }.takeLast(24000)
                val summaryPrompt = "你是聊天记忆整理助手。请把下面的聊天记录整理成给未来聊天使用的记忆纸条，不要写泛泛摘要。保留明确日期、双方关系变化、称呼、稳定偏好、共同经历、未完成约定和重要事实；区分谁说的，不确定的内容不要编造。用简体中文，分点清晰，控制在 600 字以内，只输出记忆内容。"
                val result = withContext(Dispatchers.IO) {
                    val sessions: ApiSessionStore = DataStoreApiSessionStore(context)
                    val client = ChatCompletionClient(ApiHeaderResolver(sessions))
                    client.complete(
                        ApiConnectionConfig(
                            baseUrl = apiConfig.baseUrl,
                            apiKey = apiConfig.apiKey,
                            customHeadersJson = apiConfig.customHeadersJson,
                            userProfileId = "default",
                            conversationId = conversationId
                        ),
                        ChatCompletionRequest(
                            model = apiConfig.model,
                            messages = listOf(
                                ChatMessagePayload.text("system", summaryPrompt),
                                ChatMessagePayload.text("user", "当前聊天的角色与用户记录：\n" + transcript)
                            ),
                            stream = false
                        )
                    )
                }
                val note = result.content.trim()
                if (note.isBlank()) error("模型没有返回记忆内容")
                dao.upsertMemoryEntry(
                    MemoryEntryEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        type = "note",
                        text = note,
                        createdAt = System.currentTimeMillis(),
                        rangeStart = start,
                        rangeEnd = end,
                        isMerged = false
                    )
                )
                rangeStart = "1"
                rangeEnd = messages.size.toString()
            } catch (failure: Exception) {
                generationError = failure.message ?: "记忆总结失败"
            } finally {
                generating = false
            }
        }
    }

    fun startNew(kind: String) {
        editing = null
        title = ""
        content = ""
        type = kind
        showEditor = true
    }
    fun startEdit(entry: MemoryEntryEntity) {
        editing = entry
        title = entry.text.lineSequence().firstOrNull()?.take(48).orEmpty()
        content = entry.text
        type = entry.type
        showEditor = true
    }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF7F8FA)).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("‹", Modifier.clickable { onBack() }.padding(end = 12.dp), fontSize = 30.sp, color = Color(0xFF20242A))
            Column(Modifier.weight(1f)) {
                Text("记忆管理", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF171A1F))
                Text("只属于当前聊天的记忆，不会串到其他角色", fontSize = 11.sp, color = Color(0xFF858C96))
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("生成聊天记忆", color = Color(0xFF20242A), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("选择消息范围，AI 会整理成之后聊天可参考的纸条", color = Color(0xFF858C96), fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(rangeStart, { rangeStart = it }, modifier = Modifier.weight(1f), label = { Text("起始消息") }, singleLine = true)
                    OutlinedTextField(rangeEnd, { rangeEnd = it }, modifier = Modifier.weight(1f), label = { Text("结束消息") }, placeholder = { Text(messages.size.toString()) }, singleLine = true)
                }
                Text("当前聊天共 ${messages.size} 条消息", color = Color(0xFF9AA1AA), fontSize = 10.sp)
                Text(if (generating) "正在整理记忆…" else "生成纸条", Modifier.fillMaxWidth().background(Color(0xFF171A1F), RoundedCornerShape(12.dp)).clickable(enabled = !generating) { generateSummary() }.padding(vertical = 11.dp), color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 12.sp)
                generationError?.let { Text(it, color = Color(0xFFB45C5C), fontSize = 10.sp) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f).clickable { startNew("traditional") }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(13.dp)) {
                    Text("＋ 手动记忆", fontWeight = FontWeight.SemiBold, color = Color(0xFF20242A), fontSize = 13.sp)
                    Text("每次聊天都会注入", color = Color(0xFF858C96), fontSize = 10.sp)
                }
            }
            Card(Modifier.weight(1f).clickable { startNew("note") }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(13.dp)) {
                    Text("＋ 总结纸条", fontWeight = FontWeight.SemiBold, color = Color(0xFF20242A), fontSize = 13.sp)
                    Text("聊天中的长期信息", color = Color(0xFF858C96), fontSize = 10.sp)
                }
            }
        }
        OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("搜索记忆") }, shape = RoundedCornerShape(14.dp))
        val filtered = entries.filter { search.isBlank() || it.text.contains(search, true) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("共 ${entries.size} 条", color = Color(0xFF858C96), fontSize = 11.sp)
            Text("手动记忆 ${entries.count { it.type == "traditional" }} · 纸条 ${entries.count { it.type == "note" }}", color = Color(0xFF858C96), fontSize = 11.sp)
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (search.isBlank()) "还没有记忆纸条" else "没有找到相关记忆", color = Color(0xFF6F7782), fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("可以手动保存重要设定，也可以整理聊天中的共同经历", color = Color(0xFFA0A6AE), fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { entry ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (entry.type == "traditional") "手动记忆" else if (entry.isMerged) "早期记忆" else "总结纸条", Modifier.weight(1f), color = Color(0xFF737C87), fontSize = 10.sp)
                                Text(SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(entry.createdAt)), color = Color(0xFF9AA1AA), fontSize = 10.sp)
                            }
                            Text(entry.text, color = Color(0xFF343B44), fontSize = 13.sp, lineHeight = 19.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("编辑", Modifier.clickable { startEdit(entry) }.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF303741), fontSize = 11.sp)
                                Text("删除", Modifier.clickable { scope.launch { dao.deleteMemoryEntry(entry.id, conversationId) } }.padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp), color = Color(0xFFB45C5C), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) if (type == "traditional") "添加手动记忆" else "添加总结纸条" else "编辑记忆") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(content, { content = it }, label = { Text("记忆内容") }, minLines = 4)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val old = editing
                    val now = System.currentTimeMillis()
                    scope.launch {
                        dao.upsertMemoryEntry(
                            MemoryEntryEntity(
                                id = old?.id ?: UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                type = type,
                                text = content.trim(),
                                createdAt = old?.createdAt ?: now,
                                rangeStart = old?.rangeStart,
                                rangeEnd = old?.rangeEnd,
                                isMerged = old?.isMerged ?: false
                            )
                        )
                    }
                    showEditor = false
                }, enabled = content.isNotBlank()) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }
}
