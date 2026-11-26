package com.water1014.miowatery.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water1014.miowatery.core.database.MioDatabase
import com.water1014.miowatery.core.database.PersonaEntity
import kotlinx.coroutines.launch
import org.json.JSONObject

private data class RolePhoneApp(val id: String, val name: String, val icon: String, val hint: String)

private val rolePhoneApps = listOf(
    RolePhoneApp("message", "微信", "☏", "聊天记录与联系人"),
    RolePhoneApp("notes", "备忘录", "▤", "日常记录与想做的事"),
    RolePhoneApp("twitter", "Twitter", "𝕏", "个人主页与动态"),
    RolePhoneApp("phone", "电话", "☎", "通话记录"),
    RolePhoneApp("health", "健康", "♡", "身体数据与健康记录"),
    RolePhoneApp("music", "Apple Music", "♫", "歌单与最近播放")
)

@Composable
fun RolePhoneScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val personas by dao.observePersonas("default").collectAsState(initial = emptyList())
    var selectedPersona by remember { mutableStateOf<PersonaEntity?>(null) }
    var activeApp by remember { mutableStateOf<RolePhoneApp?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("role_phone_native_v1", Context.MODE_PRIVATE) }

    fun readContent(personaId: String, appId: String): String =
        prefs.getString("content_${personaId}_${appId}", "").orEmpty()

    fun openApp(app: RolePhoneApp, persona: PersonaEntity) {
        activeApp = app
        saved = readContent(persona.personaId, app.id)
        revision++
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹", Modifier.clip(CircleShape).clickable {
                    if (activeApp != null) activeApp = null
                    else if (selectedPersona != null) selectedPersona = null
                    else onBack()
                }.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 30.sp)
                Text(
                    activeApp?.name ?: selectedPersona?.let { "${it.name} 的手机" } ?: "角色手机",
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {
                    if (activeApp != null) activeApp = null
                    else if (selectedPersona != null) selectedPersona = null
                    else onBack()
                }) { Text(if (activeApp != null) "桌面" else "返回") }
            }

            when {
                selectedPersona == null -> {
                    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(16.dp))
                        Text("选择一位角色", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("进入角色的私人手机，查看属于 TA 的数字生活。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(Modifier.height(22.dp))
                        if (personas.isEmpty()) {
                            Text("还没有联系人，请先到「联系人设定」添加角色。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(personas, key = { it.personaId }) { p ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedPersona = p }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                                            Text(p.name.take(1), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                            Text(p.description.orEmpty().ifBlank { "角色手机" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                                        }
                                        Text("进入 ›", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                activeApp == null -> {
                    val persona = selectedPersona!!
                    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp)
                        ) {
                            Column {
                                Text("今天", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(persona.name, fontSize = 27.sp, fontWeight = FontWeight.Light)
                                Text("私人空间 · iPhone", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("应用", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            rolePhoneApps.chunked(3).forEach { rowApps ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    rowApps.forEach { app ->
                                        Column(
                                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { openApp(app, persona) }
                                                .padding(vertical = 16.dp, horizontal = 5.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                                                Text(app.icon, fontSize = 25.sp)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(app.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                                            Text(if (readContent(persona.personaId, app.id).isBlank()) "暂无内容" else "已有内容", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    repeat(3 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("内容按角色独立保存，更新应用不会清除这些记录。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
                else -> {
                    val persona = selectedPersona!!
                    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                        Text(activeApp!!.hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("内容", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = {
                                draft = saved
                                showEditor = true
                            }) { Text("编辑") }
                        }
                        if (saved.isBlank()) {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("还没有内容", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(6.dp))
                                    Text("可以先手动添加记录，内容会保存在这位角色的手机里。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { draft = ""; showEditor = true }) { Text("添加内容") }
                                }
                            }
                        } else {
                            Text(
                                saved,
                                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                                fontSize = 14.sp,
                                lineHeight = 23.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("编辑${activeApp?.name.orEmpty()}") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    maxLines = 14,
                    placeholder = { Text("添加角色手机里的内容…") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val persona = selectedPersona
                    val app = activeApp
                    if (persona != null && app != null) {
                        prefs.edit().putString("content_${persona.personaId}_${app.id}", draft).apply()
                        saved = draft
                        revision++
                    }
                    showEditor = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }
}
