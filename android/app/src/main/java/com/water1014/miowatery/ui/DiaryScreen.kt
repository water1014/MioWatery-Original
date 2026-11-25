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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.water1014.miowatery.core.database.DiaryEntryEntity
import com.water1014.miowatery.core.database.MioDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun DiaryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val scope = rememberCoroutineScope()
    val entries by dao.observeDiaryEntries().collectAsState(initial = emptyList())
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DiaryEntryEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("平静") }
    var entryDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var showLockedHint by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<DiaryEntryEntity?>(null) }

    fun beginCreate() {
        editing = null
        title = ""
        content = ""
        mood = "平静"
        entryDate = LocalDate.now().toString()
        showEditor = true
    }
    fun beginEdit(entry: DiaryEntryEntity) {
        if (entry.isLocked) {
            showLockedHint = true
            return
        }
        editing = entry
        title = entry.title
        content = entry.content
        mood = entry.mood
        entryDate = entry.entryDate
        showEditor = true
    }

    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("把日子收进纸页", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("${entries.size} 篇记录 · 只保存在这台设备", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { beginCreate() }) { Text("写日记") }
        }
        if (entries.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("今天还没有留下文字", style = MaterialTheme.typography.titleMedium)
                    Text("写下一个瞬间、一顿饭，或一句想留住的话。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { beginCreate() }) { Text("写下第一篇") }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Card(
                        Modifier.fillMaxWidth().clickable { beginEdit(entry) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    runCatching { LocalDate.parse(entry.entryDate).format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)) }.getOrDefault(entry.entryDate),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(buildString {
                                    append(entry.mood)
                                    if (entry.isFavorite) append("  ·  ♥")
                                    if (entry.isLocked) append("  ·  已锁定")
                                }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(entry.title.ifBlank { "无标题" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(entry.content, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    scope.launch { dao.upsertDiaryEntry(entry.copy(isFavorite = !entry.isFavorite, updatedAt = System.currentTimeMillis())) }
                                }) { Text(if (entry.isFavorite) "取消收藏" else "收藏") }
                                TextButton(onClick = {
                                    scope.launch { dao.upsertDiaryEntry(entry.copy(isLocked = !entry.isLocked, updatedAt = System.currentTimeMillis())) }
                                }) { Text(if (entry.isLocked) "解除锁定" else "锁定") }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { if (entry.isLocked) showLockedHint = true else showDeleteConfirm = entry }) { Text("删除") }
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
            title = { Text(if (editing == null) "写一篇日记" else "编辑日记") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = entryDate, onValueChange = { entryDate = it }, label = { Text("日期（YYYY-MM-DD）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("今天的心情", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("开心", "平静", "低落", "期待").forEach { option ->
                            FilterChip(selected = mood == option, onClick = { mood = option }, label = { Text(option) })
                        }
                    }
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("写点什么…") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = content.isNotBlank() && runCatching { LocalDate.parse(entryDate); true }.getOrDefault(false),
                    onClick = {
                        val now = System.currentTimeMillis()
                        val old = editing
                        scope.launch {
                            dao.upsertDiaryEntry(
                                DiaryEntryEntity(
                                    id = old?.id ?: UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    content = content.trim(),
                                    entryDate = entryDate,
                                    mood = mood,
                                    isFavorite = old?.isFavorite ?: false,
                                    isLocked = false,
                                    createdAt = old?.createdAt ?: now,
                                    updatedAt = now
                                )
                            )
                        }
                        showEditor = false
                    }
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }
    if (showLockedHint) {
        AlertDialog(
            onDismissRequest = { showLockedHint = false },
            title = { Text("这篇日记已锁定") },
            text = { Text("先在日记卡片上解除锁定，才能编辑或删除。") },
            confirmButton = { TextButton(onClick = { showLockedHint = false }) { Text("知道了") } }
        )
    }
    showDeleteConfirm?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除这篇日记？") },
            text = { Text("删除后无法从日记列表恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteDiaryEntry(entry.id) }
                    showDeleteConfirm = null
                }) { Text("确认删除") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") } }
        )
    }
}
