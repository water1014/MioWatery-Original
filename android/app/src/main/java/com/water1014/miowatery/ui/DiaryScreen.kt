package com.water1014.miowatery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    var entryDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var showDeleteConfirm by remember { mutableStateOf<DiaryEntryEntity?>(null) }

    fun beginCreate() {
        editing = null
        title = ""
        content = ""
        entryDate = LocalDate.now().toString()
        showEditor = true
    }
    fun beginEdit(entry: DiaryEntryEntity) {
        editing = entry
        title = entry.title
        content = entry.content
        entryDate = entry.entryDate
        showEditor = true
    }

    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("日记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("${entries.size} 篇记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { beginCreate() }) { Text("新建") }
        }
        if (entries.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("还没有日记", style = MaterialTheme.typography.titleMedium)
                    Text("把想留下来的日常写在这里。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { beginCreate() }) { Text("写第一篇") }
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
                            Text(
                                runCatching { LocalDate.parse(entry.entryDate).format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)) }.getOrDefault(entry.entryDate),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(entry.title.ifBlank { "无标题" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(entry.content, maxLines = 4, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showDeleteConfirm = entry }) { Text("删除") }
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
            title = { Text(if (editing == null) "新建日记" else "编辑日记") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = entryDate, onValueChange = { entryDate = it }, label = { Text("日期（YYYY-MM-DD）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("正文") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
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
                                    mood = "",
                                    isFavorite = false,
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
    showDeleteConfirm?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除这篇日记？") },
            text = { Text("删除后无法恢复。") },
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
