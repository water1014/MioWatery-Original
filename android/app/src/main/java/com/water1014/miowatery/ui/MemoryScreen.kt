package com.water1014.miowatery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun MemoryScreen(onBack: () -> Unit, conversationId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val memoryPrefs = remember(conversationId) { context.getSharedPreferences("mio_memory_settings", android.content.Context.MODE_PRIVATE) }
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val scope = rememberCoroutineScope()
    val entries by dao.observeMemoryEntries(conversationId).collectAsState(initial = emptyList())
    val messages by dao.observeMessages(conversationId).collectAsState(initial = emptyList())
    val configStore = remember { ApiConfigStore(context) }
    val apiConfig by configStore.config.collectAsState(initial = SavedApiConfig())

    var rangeStart by remember { mutableStateOf("1") }
    var rangeEnd by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<MemoryEntryEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("traditional") }
    var expandedEntryId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<MemoryEntryEntity?>(null) }
    var traditionalExpanded by remember { mutableStateOf(false) }
    var contextCount by remember(conversationId) { mutableStateOf(memoryPrefs.getInt("context_count_$conversationId", 0).takeIf { it > 0 }?.toString().orEmpty()) }
    var summaryInstruction by remember(conversationId) { mutableStateOf(memoryPrefs.getString("summary_instruction_$conversationId", "").orEmpty()) }
    var settingsSaved by remember { mutableStateOf(false) }
    var autoSummaryEnabled by remember(conversationId) { mutableStateOf(memoryPrefs.getBoolean("auto_summary_$conversationId", false)) }
    var autoSummaryInterval by remember(conversationId) { mutableStateOf(memoryPrefs.getInt("auto_summary_interval_$conversationId", 200).toString()) }

    val traditionalEntries = entries.filter { it.type == "traditional" }
    val noteEntries = entries.filter { it.type != "traditional" }
    val filteredNotes = noteEntries.filter { search.isBlank() || it.text.contains(search, true) }
    val filteredTraditional = traditionalEntries.filter { search.isBlank() || it.text.contains(search, true) }
    val groupBackground = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant

    fun startNew(kind: String) {
        editing = null
        content = ""
        type = kind
        showEditor = true
    }
    fun startEdit(entry: MemoryEntryEntity) {
        editing = entry
        content = entry.text
        type = entry.type
        showEditor = true
    }
    fun generateSummary() {
        if (generating) return
        val start = (rangeStart.toIntOrNull() ?: 1).coerceAtLeast(1)
        val end = (rangeEnd.toIntOrNull() ?: messages.size).coerceAtMost(messages.size)
        if (messages.isEmpty() || start > messages.size || end < start) {
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
                val customInstruction = memoryPrefs.getString("summary_instruction_$conversationId", "").orEmpty().trim()
                val summaryPrompt = buildString {
                    append("你是聊天记忆整理助手。请把下面的聊天记录整理成给未来聊天使用的记忆纸条，不要写泛泛摘要。保留明确日期、双方关系变化、称呼、稳定偏好、共同经历、未完成约定和重要事实；区分谁说的，不确定的内容不要编造。用简体中文，分点清晰，控制在 600 字以内，只输出记忆内容。")
                    if (customInstruction.isNotBlank()) append("\n\n用户额外总结规则（优先遵守）：\n").append(customInstruction)
                }
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
                rangeEnd = ""
            } catch (failure: Exception) {
                generationError = failure.message ?: "记忆总结失败"
            } finally {
                generating = false
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", Modifier.clickable { onBack() }.padding(end = 16.dp), fontSize = 30.sp, color = ink)
                Column(Modifier.weight(1f)) {
                    Text("记忆管理", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = ink)
                    Text("当前聊天的长期记忆", fontSize = 12.sp, color = secondaryText)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = groupBackground)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        StatCell(noteEntries.size.toString(), "纸条", Modifier.weight(1f))
                        StatCell(traditionalEntries.size.toString(), "传统记忆", Modifier.weight(1f))
                        StatCell("0", "向量记忆", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("●", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp)
                        Text(" 传统模式", color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("手动记忆与最近 8 张纸条会参与回复", color = secondaryText, fontSize = 10.sp, textAlign = TextAlign.End)
                    }
                }
            }
        }
        item { SectionLabel("聊天记忆设置") }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = groupBackground)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("自动生成记忆纸条", color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("达到指定消息数量后，自动整理一次聊天记忆", color = secondaryText, fontSize = 11.sp)
                        }
                        Checkbox(checked = autoSummaryEnabled, onCheckedChange = { autoSummaryEnabled = it })
                    }
                    OutlinedTextField(
                        value = autoSummaryInterval,
                        onValueChange = { autoSummaryInterval = it.filter(Char::isDigit).take(4) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("总结间隔（20–2000 条）") },
                        singleLine = true,
                        enabled = autoSummaryEnabled
                    )
                    Text("上下文读取条数", color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("每次发送给 AI 的最近聊天消息数。留空则使用全部聊天记录。", color = secondaryText, fontSize = 11.sp)
                    OutlinedTextField(
                        value = contextCount,
                        onValueChange = { contextCount = it.filter(Char::isDigit).take(4) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("消息条数（1–1000，留空为全部）") },
                        singleLine = true
                    )
                    Text("自定义总结规则", color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = summaryInstruction,
                        onValueChange = { summaryInstruction = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如：优先记录关系变化、双方明确说过的偏好和承诺。") },
                        minLines = 3,
                        maxLines = 6
                    )
                    Text(
                        "保存后会应用于当前聊天，不影响其他角色。",
                        color = secondaryText,
                        fontSize = 11.sp
                    )
                    Text(
                        if (settingsSaved) "已保存" else "保存设置",
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                            .clickable {
                                val count = contextCount.toIntOrNull()?.coerceIn(1, 1000) ?: 0
                                val interval = autoSummaryInterval.toIntOrNull()?.coerceIn(20, 2000) ?: 200
                                autoSummaryInterval = interval.toString()
                                memoryPrefs.edit()
                                    .putInt("context_count_$conversationId", count)
                                    .putString("summary_instruction_$conversationId", summaryInstruction.trim())
                                    .putBoolean("auto_summary_$conversationId", autoSummaryEnabled)
                                    .putInt("auto_summary_interval_$conversationId", interval)
                                    .apply()
                                settingsSaved = true
                            }
                            .padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        item { SectionLabel("手动生成纸条") }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = groupBackground)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("选择要整理的聊天消息范围", color = secondaryText, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(rangeStart, { rangeStart = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("起始序号") }, singleLine = true)
                        Text("至", color = secondaryText)
                        OutlinedTextField(rangeEnd, { rangeEnd = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("结束序号") }, placeholder = { Text(messages.size.toString()) }, singleLine = true)
                    }
                    Text("当前聊天共 ${messages.size} 条消息", color = secondaryText, fontSize = 11.sp)
                    Text(
                        if (generating) "正在生成…" else "生成纸条",
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                            .clickable(enabled = !generating) { generateSummary() }.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                    generationError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            }
        }
        item { SectionLabel("纸条（${noteEntries.size}）") }
        item {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("搜索记忆") }, shape = RoundedCornerShape(12.dp))
        }
        if (filteredNotes.isEmpty()) {
            item { EmptyMemoryHint(if (search.isBlank()) "还没有纸条。选好消息范围后生成，或手动添加。" else "没有找到相关纸条") }
        } else {
            items(filteredNotes, key = { it.id }) { entry ->
                MemoryNoteCard(
                    entry, expandedEntryId == entry.id,
                    { expandedEntryId = if (expandedEntryId == entry.id) null else entry.id },
                    { startEdit(entry) }, { pendingDelete = entry },
                    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(entry.createdAt)),
                    groupBackground, ink, secondaryText
                )
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = groupBackground)) {
                Column {
                    Row(Modifier.fillMaxWidth().clickable { traditionalExpanded = !traditionalExpanded }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("传统记忆", color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("手动保存的人设、关系和长期约定", color = secondaryText, fontSize = 11.sp)
                        }
                        Text("${traditionalEntries.size}", color = secondaryText, fontSize = 14.sp)
                        Text(if (traditionalExpanded) "⌄" else "›", color = secondaryText, fontSize = 20.sp)
                    }
                    if (traditionalExpanded) {
                        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                            if (filteredTraditional.isEmpty()) {
                                EmptyMemoryHint(if (search.isBlank()) "还没有手动记忆" else "没有找到相关记忆")
                            } else {
                                filteredTraditional.forEach { entry ->
                                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(entry.text, color = ink, fontSize = 14.sp, lineHeight = 21.sp)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            Text("编辑", Modifier.clickable { startEdit(entry) }.padding(8.dp), color = secondaryText, fontSize = 12.sp)
                                            Text("删除", Modifier.clickable { pendingDelete = entry }.padding(8.dp), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                                }
                            }
                            TextButton(onClick = { startNew("traditional") }) { Text("＋ 添加手动记忆") }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { startNew("note") }) { Text("＋ 添加总结纸条") }
                Spacer(Modifier.weight(1f))
                Text("记忆仅属于当前聊天", Modifier.padding(top = 10.dp), color = secondaryText, fontSize = 10.sp)
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) if (type == "traditional") "添加手动记忆" else "添加总结纸条" else "编辑记忆") },
            text = {
                OutlinedTextField(content, { content = it }, label = { Text("记忆内容") }, minLines = 4, maxLines = 8)
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
    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条记忆？") },
            text = { Text("删除后不会再注入当前聊天的上下文。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteMemoryEntry(entry.id, conversationId) }
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, Modifier.padding(start = 2.dp, top = 4.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun EmptyMemoryHint(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
}

@Composable
private fun MemoryNoteCard(
    entry: MemoryEntryEntity,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dateLabel: String,
    groupBackground: Color,
    ink: Color,
    secondaryText: Color
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = groupBackground)) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (entry.isMerged) "早期记忆" else "总结纸条", color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    if (entry.rangeStart != null) Text("消息 ${entry.rangeStart}–${entry.rangeEnd}", color = secondaryText, fontSize = 10.sp)
                    Text("  $dateLabel", color = secondaryText, fontSize = 10.sp)
                    Text(if (expanded) "⌄" else "›", color = secondaryText, fontSize = 18.sp)
                }
                Text(entry.text.lineSequence().firstOrNull()?.take(100).orEmpty(), Modifier.padding(top = 7.dp), color = secondaryText, fontSize = 12.sp, maxLines = if (expanded) 4 else 2)
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                Text(entry.text, Modifier.fillMaxWidth().padding(14.dp), color = ink, fontSize = 13.sp, lineHeight = 20.sp)
                Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = onEdit) { Text("编辑") }
                }
            }
        }
    }
}
