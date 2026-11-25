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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val MEMORY_PROFILE_ID = "default"

@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    personaId: String = "default",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val scope = rememberCoroutineScope()
    val entries by dao.observeMemoryEntries(MEMORY_PROFILE_ID, personaId).collectAsState(initial = emptyList())
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<MemoryEntryEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("日常") }
    var shared by remember { mutableStateOf(false) }

    fun startNew() {
        editing = null
        title = ""
        content = ""
        category = "日常"
        shared = false
        showEditor = true
    }
    fun startEdit(entry: MemoryEntryEntity) {
        if (entry.isLocked) return
        editing = entry
        title = entry.title
        content = entry.content
        category = entry.category
        shared = entry.personaId == null
        showEditor = true
    }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF7F8FA)).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("‹", Modifier.clickable { onBack() }.padding(end = 12.dp), fontSize = 30.sp, color = Color(0xFF20242A))
            Column(Modifier.weight(1f)) {
                Text("记忆", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF171A1F))
                Text("保存重要的事，让它们在聊天中继续生效", fontSize = 11.sp, color = Color(0xFF858C96))
            }
            Text("＋", Modifier.clickable { startNew() }.padding(8.dp), fontSize = 25.sp, color = Color(0xFF20242A))
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("搜索记忆") },
            shape = RoundedCornerShape(14.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("共 ${entries.size} 条记忆", color = Color(0xFF858C96), fontSize = 11.sp)
            Text("置顶记忆会优先参与聊天", color = Color(0xFF858C96), fontSize = 11.sp)
        }
        val filtered = entries.filter {
            search.isBlank() || it.title.contains(search, true) || it.content.contains(search, true) || it.category.contains(search, true)
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (search.isBlank()) "还没有记忆" else "没有找到相关记忆", color = Color(0xFF6F7782), fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(if (search.isBlank()) "把你们的重要约定、喜好和共同经历记在这里" else "试试其他关键词", color = Color(0xFFA0A6AE), fontSize = 11.sp)
                    if (search.isBlank()) Text("添加第一条记忆  ＋", Modifier.clickable { startNew() }.padding(12.dp), color = Color(0xFF303741), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.title.ifBlank { "未命名记忆" }, Modifier.weight(1f), color = Color(0xFF20242A), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                if (entry.isPinned) Text("置顶", color = Color(0xFF6B7480), fontSize = 10.sp)
                                if (entry.isLocked) Text(" · 已锁定", color = Color(0xFF6B7480), fontSize = 10.sp)
                            }
                            Text(entry.content, color = Color(0xFF515A65), fontSize = 13.sp, lineHeight = 19.sp)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${entry.category} · ${if (entry.personaId == null) "共同记忆" else "角色记忆"} · ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(entry.updatedAt))}", color = Color(0xFF9AA1AA), fontSize = 10.sp)
                                Row {
                                    Text(if (entry.isPinned) "取消置顶" else "置顶", Modifier.clickable {
                                        scope.launch { dao.upsertMemoryEntry(entry.copy(isPinned = !entry.isPinned, updatedAt = System.currentTimeMillis())) }
                                    }.padding(horizontal = 5.dp), color = Color(0xFF737C87), fontSize = 10.sp)
                                    Text(if (entry.isLocked) "解锁" else "锁定", Modifier.clickable {
                                        scope.launch { dao.upsertMemoryEntry(entry.copy(isLocked = !entry.isLocked, updatedAt = System.currentTimeMillis())) }
                                    }.padding(horizontal = 5.dp), color = Color(0xFF737C87), fontSize = 10.sp)
                                    if (!entry.isLocked) {
                                        Text("编辑", Modifier.clickable { startEdit(entry) }.padding(horizontal = 5.dp), color = Color(0xFF303741), fontSize = 10.sp)
                                        Text("删除", Modifier.clickable { scope.launch { dao.deleteMemoryEntry(entry.id, MEMORY_PROFILE_ID) } }.padding(start = 5.dp), color = Color(0xFFB45C5C), fontSize = 10.sp)
                                    }
                                }
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
            title = { Text(if (editing == null) "添加记忆" else "编辑记忆") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("标题") }, singleLine = true)
                    OutlinedTextField(content, { content = it }, label = { Text("记忆内容") }, minLines = 3)
                    OutlinedTextField(category, { category = it }, label = { Text("分类") }, singleLine = true)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("保存为共同记忆（所有角色可用）", Modifier.weight(1f), fontSize = 12.sp)
                        androidx.compose.material3.Switch(checked = shared, onCheckedChange = { shared = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val now = System.currentTimeMillis()
                    val old = editing
                    scope.launch {
                        dao.upsertMemoryEntry(
                            MemoryEntryEntity(
                                id = old?.id ?: UUID.randomUUID().toString(),
                                profileId = MEMORY_PROFILE_ID,
                                personaId = if (shared) null else personaId,
                                title = title.trim(),
                                content = content.trim(),
                                category = category.trim().ifBlank { "日常" },
                                isPinned = old?.isPinned ?: false,
                                isLocked = old?.isLocked ?: false,
                                createdAt = old?.createdAt ?: now,
                                updatedAt = now
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
