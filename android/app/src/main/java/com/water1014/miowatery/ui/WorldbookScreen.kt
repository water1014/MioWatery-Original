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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.water1014.miowatery.core.database.MioDatabase
import com.water1014.miowatery.core.database.WorldbookEntryEntity
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

private const val WORLDBOOK_PROFILE_ID = "default"

@Composable
fun WorldbookScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val scope = rememberCoroutineScope()
    val entries by dao.observeAllWorldbookEntries(WORLDBOOK_PROFILE_ID).collectAsState(initial = emptyList())
    val personas by dao.observePersonas(WORLDBOOK_PROFILE_ID).collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<WorldbookEntryEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var bookId by remember { mutableStateOf("默认分类") }
    var title by remember { mutableStateOf("") }
    var keys by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var trigger by remember { mutableStateOf("global") }
    var position by remember { mutableStateOf("after") }
    var globalOnly by remember { mutableStateOf(true) }
    var entryPersonaId by remember { mutableStateOf<String?>(null) }
    var showPersonaMenu by remember { mutableStateOf(false) }
    var editorError by remember { mutableStateOf<String?>(null) }

    fun openEditor(item: WorldbookEntryEntity?) {
        editing = item
        entryPersonaId = item?.personaId ?: personas.firstOrNull()?.personaId
        globalOnly = item?.let { it.personaId == null } ?: globalOnly
        editorError = null
        bookId = item?.bookId ?: "默认分类"
        title = item?.title.orEmpty()
        body = item?.content.orEmpty()
        enabled = item?.enabled ?: true
        val raw = runCatching { JSONObject(item?.rawLegacyJson ?: "{}") }.getOrNull()
        keys = raw?.optString("keys").orEmpty()
        trigger = raw?.optString("trigger", "global") ?: "global"
        position = raw?.optString("position", "after") ?: "after"
        showEditor = true
    }

    Column(modifier.fillMaxSize().background(Color.White).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", Modifier.clickable(onClick = onBack).padding(end = 14.dp), color = Color(0xFF111111), fontSize = 28.sp)
            Column(Modifier.weight(1f)) {
                Text("世界书", color = Color(0xFF111111), fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Text("故事背景、人物关系与规则设定", color = Color(0xFF8A929B), fontSize = 11.sp)
            }
            Text("＋", Modifier.clickable { openEditor(null) }.padding(horizontal = 12.dp), color = Color(0xFF111111), fontSize = 27.sp)
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { globalOnly = true }) { Text(if (globalOnly) "● 全局" else "全局", color = Color(0xFF111111)) }
            TextButton(onClick = { globalOnly = false }) { Text(if (!globalOnly) "● 联系人" else "联系人", color = Color(0xFF111111)) }
        }
        val visible = entries.filter { if (globalOnly) it.personaId == null else it.personaId != null }
        if (visible.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("这里还没有世界书", color = Color(0xFF111111), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(7.dp))
                Text("把重要的背景、规则和故事设定收进一本书里。", color = Color(0xFF8A929B), fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Button(onClick = { openEditor(null) }) { Text("新建世界书") }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(visible, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { openEditor(item) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F5))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = Color(0xFF111111), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(item.bookId + " · " + if (item.enabled) "已启用" else "已停用", color = Color(0xFF8A929B), fontSize = 10.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(item.content, color = Color(0xFF59616B), fontSize = 12.sp, maxLines = 2, lineHeight = 17.sp)
                            }
                            Text("›", color = Color(0xFF9AA1A9), fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editing == null) "新建世界书" else "编辑世界书") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!globalOnly) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showPersonaMenu = true }) {
                                Text("所属联系人：${personas.firstOrNull { it.personaId == entryPersonaId }?.name ?: "请选择"}")
                            }
                            DropdownMenu(expanded = showPersonaMenu, onDismissRequest = { showPersonaMenu = false }) {
                                personas.forEach { persona ->
                                    DropdownMenuItem(
                                        text = { Text(persona.name) },
                                        onClick = { entryPersonaId = persona.personaId; showPersonaMenu = false }
                                    )
                                }
                            }
                        }
                        if (personas.isEmpty()) Text("请先创建联系人，再添加专属世界书词条。", color = Color(0xFFB45309), fontSize = 11.sp)
                    }
                    OutlinedTextField(bookId, { bookId = it }, label = { Text("分类 / 书籍名称") }, singleLine = true)
                    OutlinedTextField(title, { title = it }, label = { Text("词条标题") }, singleLine = true)
                    OutlinedTextField(keys, { keys = it }, label = { Text("触发关键词（逗号分隔）") }, singleLine = true)
                    OutlinedTextField(body, { body = it }, label = { Text("内容") }, minLines = 4, maxLines = 7)
                    editorError?.let { Text(it, color = Color(0xFFB45309), fontSize = 11.sp) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("启用词条", Modifier.weight(1f), color = Color(0xFF111111), fontSize = 13.sp)
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { trigger = "global" }) { Text(if (trigger == "global") "● 常驻注入" else "常驻注入") }
                        TextButton(onClick = { trigger = "keyword" }) { Text(if (trigger == "keyword") "● 关键词触发" else "关键词触发") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { position = "before" }) { Text(if (position == "before") "● 人设之前" else "人设之前") }
                        TextButton(onClick = { position = "after" }) { Text(if (position == "after") "● 人设之后" else "人设之后") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!globalOnly && entryPersonaId == null) {
                        editorError = "请先创建联系人，再保存专属词条。"
                        return@TextButton
                    }
                    val old = editing
                    val now = System.currentTimeMillis()
                    val raw = runCatching { JSONObject(old?.rawLegacyJson ?: "{}") }.getOrElse { JSONObject() }
                        .put("keys", keys.trim())
                        .put("trigger", trigger)
                        .put("position", position)
                        .put("scope", if (globalOnly) "global" else "contact")
                        .toString()
                    scope.launch {
                        dao.upsertWorldbookEntries(
                            listOf(
                                WorldbookEntryEntity(
                                    id = old?.id ?: UUID.randomUUID().toString(),
                                    profileId = WORLDBOOK_PROFILE_ID,
                                    personaId = if (globalOnly) null else entryPersonaId,
                                    bookId = bookId.trim().ifBlank { "默认分类" },
                                    title = title.trim().ifBlank { "未命名词条" },
                                    content = body,
                                    enabled = enabled,
                                    rawLegacyJson = raw,
                                    updatedAt = now
                                )
                            )
                        )
                        showEditor = false
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    if (editing != null) {
                        TextButton(onClick = {
                            scope.launch {
                                dao.deleteWorldbookEntry(editing!!.id)
                                showEditor = false
                            }
                        }) { Text("删除") }
                    }
                    TextButton(onClick = { showEditor = false }) { Text("取消") }
                }
            }
        )
    }
}
