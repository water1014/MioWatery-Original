package com.water1014.miowatery.ui

import com.water1014.miowatery.R

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import com.water1014.miowatery.core.migration.LegacyBackupInspector
import com.water1014.miowatery.core.migration.LegacyBackupPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private data class AppEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: Int
)

private val appEntries = listOf(
    AppEntry("app", "聊天与应用", "聊天、联系人和动态", R.drawable.mio_ic_app),
    AppEntry("worlds", "世界书", "设定、词条与规则", R.drawable.mio_ic_worlds),
    AppEntry("personas", "联系人", "角色资料与管理", R.drawable.mio_ic_personas),
    AppEntry("diary", "日记", "记录日常片段", R.drawable.mio_ic_diary),
    AppEntry("music", "音乐", "曲库、歌词与播放", R.drawable.mio_ic_music),
    AppEntry("couple", "情侣空间", "关系与共同记录", R.drawable.mio_ic_couple),
    AppEntry("coread", "共读", "一起阅读与交流", R.drawable.mio_ic_coread),
    AppEntry("crread", "阅读", "书籍与阅读记录", R.drawable.mio_ic_crread),
    AppEntry("msgs", "讯息", "消息与会话", R.drawable.mio_ic_msgs),
    AppEntry("wallet", "钱包", "钱包与记录", R.drawable.mio_ic_wallet),
    AppEntry("mcp", "MCP", "工具与服务连接", R.drawable.mio_ic_mcp),
    AppEntry("preset", "预设", "提示词与配置预设", R.drawable.mio_ic_preset),
    AppEntry("life", "生活", "Time is Nothing", R.drawable.mio_ic_life),
    AppEntry("iff", "IF 线", "分支剧情空间", R.drawable.mio_ic_iff),
    AppEntry("xinji", "心迹", "互动与心情记录", R.drawable.mio_ic_xinji),
    AppEntry("repo", "作品仓库", "浏览、上传与导入", R.drawable.mio_ic_repo),
    AppEntry("youtube", "YouTube", "视频与媒体内容", R.drawable.mio_ic_youtube),
    AppEntry("offline", "线下", "线下相关内容", R.drawable.mio_ic_offline),
    AppEntry("rolephones", "角色手机", "角色的虚构手机空间", R.drawable.mio_ic_rolephones),
    AppEntry("desktopcustom", "桌面美化", "壁纸、图标与组件", R.drawable.mio_ic_desktopcustom),
    AppEntry("css", "美化", "外观与样式预设", R.drawable.mio_ic_css),
    AppEntry("stickers", "表情包", "贴纸与表情管理", R.drawable.mio_ic_stickers),
    AppEntry("settings", "设置", "模型、语音、生图与数据", R.drawable.mio_ic_settings),
    AppEntry("backup", "数据迁移", "旧版备份只读检查", R.drawable.mio_ic_backup),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MioWateryApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }
    var selectedPage by remember { mutableStateOf("home") }
    var selectedPersonaId by remember { mutableStateOf("default") }
    var chatReturnPage by remember { mutableStateOf("home") }
    val selectedEntry = appEntries.firstOrNull { it.id == selectedPage }

    Box(Modifier.fillMaxSize()) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "MioWatery",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
                )
                NavigationDrawerItem(
                    label = { Text("首页") },
                    selected = selectedPage == "home",
                    onClick = {
                        selectedPage = "home"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                appEntries.forEach { entry ->
                    NavigationDrawerItem(
                        label = { Text(entry.title) },
                        selected = selectedPage == entry.id,
                        onClick = {
                            selectedPage = entry.id
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        icon = { Icon(painterResource(entry.icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (selectedPage != "home" && selectedPage != "app" && selectedPage != "msgs" && selectedPage != "personas" && selectedPage != "worlds") TopAppBar(
                    title = {
                        Column {
                            Text(selectedEntry?.title ?: "MioWatery")
                            if (selectedPage == "home") {
                                Text(
                                    "原生版 · 初始框架",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (selectedPage == "home") {
                                    scope.launch { drawerState.open() }
                                } else {
                                    selectedPage = "home"
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(if (selectedPage == "home") R.drawable.mio_ic_menu else R.drawable.mio_ic_back),
                                contentDescription = if (selectedPage == "home") "打开导航" else "返回首页"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (selectedPage == "home") {
                HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    onOpen = { selectedPage = it }
                )
            } else if (selectedPage == "personas") {
                PersonaScreen(
                    onBack = { selectedPage = "home" },
                    onOpenChat = { id ->
                        selectedPersonaId = id
                        chatReturnPage = "personas"
                        selectedPage = "app"
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            } else if (selectedPage == "app" || selectedPage == "msgs") {
                ChatScreen(
                    onBack = { selectedPage = chatReturnPage },
                    personaId = selectedPersonaId,
                    modifier = Modifier.padding(innerPadding)
                )
            } else if (selectedPage == "worlds") {
                WorldbookScreen(modifier = Modifier.padding(innerPadding))
            } else if (selectedPage == "backup") {
                BackupPreviewScreen(modifier = Modifier.padding(innerPadding))
            } else {
                FeaturePlaceholder(
                    title = selectedEntry?.title ?: "功能",
                    modifier = Modifier.padding(innerPadding),
                    onBack = { selectedPage = "home" }
                )
            }
        }
    }
    if (showSplash) {
        MioSplashScreen(onFinished = { showSplash = false })
    }
}
}

@Composable
private fun BackupPreviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<LegacyBackupPreview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { loading = true; preview = null; error = null
            scope.launch { try { preview = withContext(Dispatchers.IO) {
                val name = queryBackupName(context, uri)
                val stream = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("无法打开所选文件")
                LegacyBackupInspector.inspect(stream, name)
            } } catch (failure: Exception) { error = failure.message ?: "读取备份失败，请检查文件是否完整"
            } finally { loading = false } }
        }
    }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painter = painterResource(R.drawable.mio_ic_backup), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                Text("检查旧版备份", style = MaterialTheme.typography.titleLarge)
                Text("选择 MioWatery 旧版导出的 JSON 或 ZIP 文件，先查看可识别的数据数量。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { picker.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "*/*")) }, enabled = !loading) { Text("选择备份文件") }
            }
        }
        if (loading) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp); Text("正在读取备份…") } } }
        error?.let { message -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
        preview?.let { data ->
            Text("备份概览", style = MaterialTheme.typography.titleMedium)
            Text(data.sourceFormat + " · " + data.jsonFileName + " · " + (data.jsonBytes / 1024) + " KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { BackupCountCard("用户资料", data.profileCount, Modifier.weight(1f)); BackupCountCard("角色", data.personaCount, Modifier.weight(1f)); BackupCountCard("世界书", data.worldbookCount, Modifier.weight(1f)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { BackupCountCard("聊天归属", data.chatOwnerCount, Modifier.weight(1f)); BackupCountCard("会话", data.conversationCount, Modifier.weight(1f)); BackupCountCard("消息", data.messageCount, Modifier.weight(1f)) }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("检查提示", style = MaterialTheme.typography.titleSmall)
                    data.warnings.forEach { warning -> Text("• " + warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("识别到的顶层字段", style = MaterialTheme.typography.titleSmall)
                    val extra = if (data.topLevelKeys.size > 18) " …（共 " + data.topLevelKeys.size + " 项）" else ""
                    Text(data.topLevelKeys.take(18).joinToString(" · ") + extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
        Text("安全说明：当前版本只读取并展示预览，不会写入原生数据库，也不会修改所选备份。正式迁移前还会补齐旧版字段映射与敏感信息检查。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BackupCountCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(count.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}

private fun queryBackupName(context: Context, uri: Uri): String {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    return name ?: uri.lastPathSegment ?: "backup.json"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var now by remember { mutableStateOf(java.time.LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = java.time.LocalDateTime.now(); kotlinx.coroutines.delay(15_000) } }
    val ink = Color(0xFF0F1419)
    val sub = Color(0xFF6B7480)
    val panel = Color(0xFFF3F4F5)
    val dock = listOf("app", "worlds", "personas", "settings").map { id -> appEntries.first { it.id == id } }
    Column(modifier.fillMaxSize().background(Color.White), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().weight(1f), beyondViewportPageCount = 1) { page ->
            when (page) {
                0 -> Column(Modifier.fillMaxSize().widthIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 20.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), color = ink, fontSize = 60.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                        Text(now.format(java.time.format.DateTimeFormatter.ofPattern("M月d日  EEEE", java.util.Locale.SIMPLIFIED_CHINESE)), color = sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 7.dp))
                    }
                    HomePanel("人と人の漣", Modifier.fillMaxWidth()) { Text("把今天的心情，留在这里。", color = sub, style = MaterialTheme.typography.bodySmall) }
                    Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(22.dp)).padding(16.dp)) {
                        Text("备忘录图片", color = ink, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(9.dp))
                        Box(Modifier.fillMaxWidth().height(140.dp).background(Color(0xFFE2E4E7), RoundedCornerShape(14.dp)).clickable { onOpen("diary") }, contentAlignment = Alignment.Center) { Text("点击添加图片", color = sub, style = MaterialTheme.typography.labelSmall) }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text("Set city", color = sub, style = MaterialTheme.typography.labelSmall); Text("--°", color = ink, fontSize = 32.sp, fontWeight = FontWeight.Light); Text("Tap to set city", color = sub, style = MaterialTheme.typography.labelSmall) }
                            Text("天气", color = sub, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(22.dp)).padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Messages", color = ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("See all", Modifier.clickable { onOpen("app") }, color = sub, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("还没有最近聊天", color = sub, style = MaterialTheme.typography.bodySmall)
                    }
                }
                1 -> Column(Modifier.fillMaxSize().widthIn(max = 560.dp).padding(horizontal = 8.dp).padding(top = 18.dp)) {
                    Text("应用", Modifier.padding(start = 10.dp, bottom = 10.dp), color = ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        items(appEntries) { entry -> Column(Modifier.fillMaxWidth().clickable { onOpen(entry.id) }.padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(58.dp).background(panel, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { Icon(painterResource(entry.icon), contentDescription = entry.title, tint = ink, modifier = Modifier.size(29.dp)) }
                            Spacer(Modifier.height(7.dp)); Text(entry.title, color = ink, fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center, maxLines = 2)
                        } }
                    }
                }
                else -> Column(Modifier.fillMaxSize().widthIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CalendarHomeCard(ink, sub)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomePanel("聊天预览", Modifier.weight(1f).height(120.dp)) { Text("开始一段对话", color = ink, style = MaterialTheme.typography.bodyMedium); Text("点此进入聊天", color = sub, style = MaterialTheme.typography.labelSmall) }
                        HomePanel("快捷入口", Modifier.weight(1f).height(120.dp)) { Text("联系人 · 世界书", color = ink, style = MaterialTheme.typography.bodyMedium); Text("角色与设定", color = sub, style = MaterialTheme.typography.labelSmall) }
                    }
                    Text("常用应用", color = ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf("app", "personas", "worlds", "diary").forEach { id -> val entry = appEntries.first { it.id == id }; Column(Modifier.weight(1f).clickable { onOpen(id) }, horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(40.dp).background(panel, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(painterResource(entry.icon), contentDescription = entry.title, tint = ink, modifier = Modifier.size(22.dp)) }; Spacer(Modifier.height(6.dp)); Text(entry.title, color = sub, fontSize = 10.sp, textAlign = TextAlign.Center) } } }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { repeat(3) { index -> Box(Modifier.padding(horizontal = 5.dp).size(if (pager.currentPage == index) 7.dp else 5.dp).background(if (pager.currentPage == index) ink else Color(0xFFD5D8DC), CircleShape).clickable { scope.launch { pager.animateScrollToPage(index) } }) } }
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 5.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            dock.forEach { entry -> Column(Modifier.weight(1f).clickable { onOpen(entry.id) }.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(54.dp).background(panel, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { Icon(painterResource(entry.icon), contentDescription = entry.title, tint = ink, modifier = Modifier.size(24.dp)) }; Spacer(Modifier.height(5.dp)); Text(if (entry.id == "app") "聊天" else if (entry.id == "personas") "联系人设定" else entry.title, color = ink, fontSize = 11.sp) } }
        }
    }
}

@Composable
private fun HomePanel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(Color(0xFFF3F4F5), RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text(title, color = Color(0xFF0F1419), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(9.dp))
        content()
    }
}

@Composable
private fun CalendarHomeCard(ink: Color, sub: Color) {
    val today = java.time.LocalDate.now()
    val firstDayOffset = today.withDayOfMonth(1).dayOfWeek.value % 7
    val monthDays = today.lengthOfMonth()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(today.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault())),
                color = ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                    Text(it, Modifier.weight(1f), color = sub, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            for (week in 0..5) {
                Row(Modifier.fillMaxWidth()) {
                    for (column in 0..6) {
                        val day = week * 7 + column - firstDayOffset + 1
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (day in 1..monthDays) {
                                Box(Modifier.size(30.dp).then(if (day == today.dayOfMonth) Modifier.background(ink, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center) {
                                    Text(day.toString(), color = if (day == today.dayOfMonth) Color.White else ink, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "这个模块还没有迁移到原生版。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "旧版功能和数据保持不变，后续会按迁移清单逐项实现。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        androidx.compose.material3.OutlinedButton(onClick = onBack) {
            Text("返回首页")
        }
    }
}
