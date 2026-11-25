package com.water1014.miowatery.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import com.water1014.miowatery.core.migration.LegacyBackupInspector
import com.water1014.miowatery.core.migration.LegacyBackupPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class AppEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val appEntries = listOf(
    AppEntry("app", "聊天与应用", "聊天、联系人和动态", Icons.AutoMirrored.Filled.Chat),
    AppEntry("worlds", "世界书", "设定、词条与规则", Icons.Default.Book),
    AppEntry("personas", "联系人", "角色资料与管理", Icons.Default.FavoriteBorder),
    AppEntry("diary", "日记", "记录日常片段", Icons.Default.Today),
    AppEntry("music", "音乐", "曲库、歌词与播放", Icons.Default.MusicNote),
    AppEntry("couple", "情侣空间", "关系与共同记录", Icons.Default.FavoriteBorder),
    AppEntry("coread", "共读", "一起阅读与交流", Icons.Default.Book),
    AppEntry("crread", "阅读", "书籍与阅读记录", Icons.Default.Book),
    AppEntry("msgs", "讯息", "消息与会话", Icons.AutoMirrored.Filled.Chat),
    AppEntry("wallet", "钱包", "钱包与记录", Icons.Default.Album),
    AppEntry("mcp", "MCP", "工具与服务连接", Icons.Default.AutoAwesome),
    AppEntry("preset", "预设", "提示词与配置预设", Icons.Default.Book),
    AppEntry("life", "生活", "Time is Nothing", Icons.Default.Today),
    AppEntry("iff", "IF 线", "分支剧情空间", Icons.Default.AutoAwesome),
    AppEntry("xinji", "心迹", "互动与心情记录", Icons.Default.FavoriteBorder),
    AppEntry("repo", "作品仓库", "浏览、上传与导入", Icons.Default.Album),
    AppEntry("youtube", "YouTube", "视频与媒体内容", Icons.Default.Image),
    AppEntry("offline", "线下", "线下相关内容", Icons.Default.CalendarMonth),
    AppEntry("rolephones", "角色手机", "角色的虚构手机空间", Icons.Default.Album),
    AppEntry("desktopcustom", "桌面美化", "壁纸、图标与组件", Icons.Default.Image),
    AppEntry("css", "美化", "外观与样式预设", Icons.Default.Settings),
    AppEntry("stickers", "表情包", "贴纸与表情管理", Icons.Default.Image),
    AppEntry("settings", "设置", "模型、语音、生图与数据", Icons.Default.Settings),
    AppEntry("backup", "数据迁移", "旧版备份只读检查", Icons.Default.FolderOpen)
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
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
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
                                if (selectedPage == "home") Icons.Default.Menu
                                else Icons.AutoMirrored.Filled.ArrowBack,
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
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(30.dp))
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
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpen: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "把日常，慢慢收进这里。",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "原生应用正在搭建中，旧版数据不会被自动改动。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("新的开始", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "先搭好原生骨架，再逐项迁移完整功能。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("应用", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appEntries) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(entry.id) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            entry.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(entry.title, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            entry.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
