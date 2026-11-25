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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    var selectedPage by remember { mutableStateOf("home") }
    val selectedEntry = appEntries.firstOrNull { it.id == selectedPage }

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
                if (selectedPage != "home") TopAppBar(
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

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    var now by remember { mutableStateOf(java.time.LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { now = java.time.LocalDateTime.now(); delay(30_000) }
    }
    val time = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    val date = now.format(java.time.format.DateTimeFormatter.ofPattern("M月d日  EEEE", java.util.Locale.SIMPLIFIED_CHINESE))
    val dock = listOf(
        appEntries.first { it.id == "app" },
        appEntries.first { it.id == "worlds" },
        appEntries.first { it.id == "personas" },
        appEntries.first { it.id == "settings" }
    )
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 18.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(time, style = MaterialTheme.typography.displayLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Light, letterSpacing = (-2).sp), color = MaterialTheme.colorScheme.onBackground)
                Text(date, Modifier.padding(bottom = 7.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().height(290.dp), userScrollEnabled = false, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { HomePanel("便签", Modifier.height(140.dp)) { Text("点这里写便签", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                item { HomePanel("天气", Modifier.height(140.dp)) { Text("设置城市", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("--°", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Light)); Text("天气信息将在接入后显示", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) { HomePanel("Messages", Modifier.fillMaxWidth().height(130.dp), "See all") { Text("还没有最近聊天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            dock.forEach { entry ->
                Column(Modifier.weight(1f).clickable { onOpen(entry.id) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(54.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { Icon(painterResource(entry.icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.height(5.dp))
                    Text(if (entry.id == "personas") "联系人设定" else entry.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun HomePanel(title: String, modifier: Modifier = Modifier, trailing: String? = null, content: @Composable Column.() -> Unit) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        content()
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
