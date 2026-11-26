package com.water1014.miowatery.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water1014.miowatery.core.database.MioDatabase
import com.water1014.miowatery.core.database.PersonaEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class RolePhoneApp(val id: String, val name: String, val icon: String, val hint: String)

private val rolePhoneApps = listOf(
    RolePhoneApp("message", "微信", "●", "聊天记录与联系人"),
    RolePhoneApp("notes", "备忘录", "▤", "日常记录与想做的事"),
    RolePhoneApp("twitter", "Twitter", "𝕏", "个人主页与首页时间线"),
    RolePhoneApp("phone", "电话", "☎", "通话记录与联系人"),
    RolePhoneApp("health", "健康", "♡", "身体数据、高心率通知与血压"),
    RolePhoneApp("music", "Apple Music", "♫", "歌单与最近播放")
)

private data class PhoneWallpaper(val name: String, val color: androidx.compose.ui.graphics.Color, val dark: Boolean = false)

private val phoneWallpapers = listOf(
    PhoneWallpaper("暮光蓝紫", androidx.compose.ui.graphics.Color(0xFF6676B9)),
    PhoneWallpaper("石墨黑", androidx.compose.ui.graphics.Color(0xFF17191F), true),
    PhoneWallpaper("灰雨滴", androidx.compose.ui.graphics.Color(0xFF9A9DA4)),
    PhoneWallpaper("雾蓝", androidx.compose.ui.graphics.Color(0xFFDCE5EF)),
    PhoneWallpaper("低饱和粉", androidx.compose.ui.graphics.Color(0xFFEAD9DD)),
    PhoneWallpaper("奶油白", androidx.compose.ui.graphics.Color(0xFFF3EFE9)),
    PhoneWallpaper("深海", androidx.compose.ui.graphics.Color(0xFF173451), true)
)

@Composable
fun RolePhoneScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val personas by dao.observePersonas("default").collectAsState(initial = emptyList())
    val prefs = remember { context.getSharedPreferences("role_phone_native_v1", Context.MODE_PRIVATE) }
    var selectedPersona by remember { mutableStateOf<PersonaEntity?>(null) }
    var activeApp by remember { mutableStateOf<RolePhoneApp?>(null) }
    var unlocked by remember { mutableStateOf(false) }
    var wallpaperIndex by remember { mutableIntStateOf(prefs.getInt("wallpaper_home", 0).coerceIn(phoneWallpapers.indices)) }
    var lockWallpaperIndex by remember { mutableIntStateOf(prefs.getInt("wallpaper_lock", 1).coerceIn(phoneWallpapers.indices)) }
    var showWallpaperMenu by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }
    var dragDistance by remember { mutableFloatStateOf(0f) }

    fun readContent(personaId: String, appId: String): String =
        prefs.getString("content_${personaId}_${appId}", "").orEmpty()

    fun openApp(app: RolePhoneApp, persona: PersonaEntity) {
        activeApp = app
        saved = readContent(persona.personaId, app.id)
    }

    val phoneBg = if (unlocked) phoneWallpapers[wallpaperIndex] else phoneWallpapers[lockWallpaperIndex]
    val foreground = if (phoneBg.dark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF202127)
    val muted = foreground.copy(alpha = .72f)
    val now = LocalDateTime.now()
    val clock = now.format(DateTimeFormatter.ofPattern("HH:mm"))

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹", Modifier.clip(CircleShape).clickable {
                    when {
                        activeApp != null -> activeApp = null
                        selectedPersona != null && unlocked -> unlocked = false
                        selectedPersona != null -> selectedPersona = null
                        else -> onBack()
                    }
                }.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 30.sp)
                Text(
                    activeApp?.name ?: selectedPersona?.let { "${it.name} 的手机" } ?: "角色手机",
                    Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {
                    when {
                        activeApp != null -> activeApp = null
                        selectedPersona != null && unlocked -> unlocked = false
                        selectedPersona != null -> selectedPersona = null
                        else -> onBack()
                    }
                }) { Text(if (activeApp != null) "主屏幕" else "返回") }
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
                                            .clickable { selectedPersona = p; unlocked = false; activeApp = null }
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

                !unlocked -> {
                    val persona = selectedPersona!!
                    Box(
                        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(34.dp))
                            .background(phoneBg.color)
                            .pointerInput(persona.personaId) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, amount -> dragDistance += amount },
                                    onDragEnd = {
                                        if (dragDistance < -70f) unlocked = true
                                        dragDistance = 0f
                                    },
                                    onDragCancel = { dragDistance = 0f }
                                )
                            }
                    ) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("无 SIM 卡  ◔", color = foreground, fontSize = 11.sp)
                                Spacer(Modifier.weight(1f))
                                Text("▮▮  88%  ▰", color = foreground, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(38.dp))
                            Text(now.format(DateTimeFormatter.ofPattern("M月d日  EEEE", java.util.Locale.CHINA)), color = foreground, fontSize = 13.sp)
                            Text(clock, color = foreground, fontSize = 72.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp)
                            Spacer(Modifier.height(30.dp))
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                                    .background(foreground.copy(alpha = .10f)).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp).clip(CircleShape).background(foreground.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                                    Text(persona.name.take(1), color = foreground, fontSize = 18.sp)
                                }
                                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                                    Text(persona.name, color = foreground, fontWeight = FontWeight.SemiBold)
                                    Text("左滑进入这部手机", color = muted, fontSize = 11.sp)
                                }
                                Text("›", color = foreground, fontSize = 24.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("‹‹  向左滑动解锁  ››", color = foreground, fontSize = 13.sp)
                            Spacer(Modifier.height(18.dp))
                            Box {
                                TextButton(onClick = { showWallpaperMenu = true }) {
                                    Text("更换锁屏壁纸", color = foreground)
                                }
                                DropdownMenu(expanded = showWallpaperMenu, onDismissRequest = { showWallpaperMenu = false }) {
                                    phoneWallpapers.forEachIndexed { index, wallpaper ->
                                        DropdownMenuItem(
                                            text = { Text(wallpaper.name) },
                                            onClick = {
                                                lockWallpaperIndex = index
                                                prefs.edit().putInt("wallpaper_lock", index).apply()
                                                showWallpaperMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                activeApp == null -> {
                    val persona = selectedPersona!!
                    Box(
                        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(34.dp))
                            .background(phoneWallpapers[wallpaperIndex].color)
                    ) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 17.dp, vertical = 14.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("无服务  ◔", color = foreground, fontSize = 10.sp)
                                Spacer(Modifier.weight(1f))
                                Text(clock + "  ▰", color = foreground, fontSize = 10.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(now.format(DateTimeFormatter.ofPattern("EEEE, MMM d", java.util.Locale.US)), color = foreground, fontSize = 12.sp)
                            Text(persona.name, color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Light)
                            Spacer(Modifier.height(22.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                rolePhoneApps.chunked(3).forEach { rowApps ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        rowApps.forEach { app ->
                                            Column(
                                                Modifier.weight(1f).clip(RoundedCornerShape(17.dp))
                                                    .clickable { openApp(app, persona) }
                                                    .padding(vertical = 5.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    Modifier.size(54.dp).clip(RoundedCornerShape(16.dp))
                                                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = .92f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(app.icon, fontSize = 27.sp, color = androidx.compose.ui.graphics.Color(0xFF25262A))
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(if (app.id == "message") "Messages" else app.name, fontSize = 11.sp, color = foreground, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                        repeat(3 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = .24f))
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("电话", "微信", "音乐", "备忘录").forEach { label ->
                                    Text(label, color = foreground, fontSize = 11.sp)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                TextButton(onClick = { showWallpaperMenu = true }) { Text("更换主屏幕壁纸", color = foreground, fontSize = 11.sp) }
                                DropdownMenu(expanded = showWallpaperMenu, onDismissRequest = { showWallpaperMenu = false }) {
                                    phoneWallpapers.forEachIndexed { index, wallpaper ->
                                        DropdownMenuItem(
                                            text = { Text(wallpaper.name) },
                                            onClick = {
                                                wallpaperIndex = index
                                                prefs.edit().putInt("wallpaper_home", index).apply()
                                                showWallpaperMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    val persona = selectedPersona!!
                    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                        Text(activeApp!!.hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("内容", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { draft = saved; showEditor = true }) { Text("编辑") }
                        }
                        if (saved.isBlank()) {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("还没有内容", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(6.dp))
                                    Text("可以先添加记录，内容会保存在这位角色的手机里。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { draft = ""; showEditor = true }) { Text("添加内容") }
                                }
                            }
                        } else {
                            Text(
                                saved,
                                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                                fontSize = 14.sp, lineHeight = 23.sp
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
                    value = draft, onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 8, maxLines = 14,
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
                    }
                    showEditor = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }
}
