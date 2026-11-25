package com.water1014.miowatery.ui

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
    AppEntry("chat", "聊天", "和角色继续对话", Icons.AutoMirrored.Filled.Chat),
    AppEntry("memory", "记忆", "重要片段与回忆", Icons.Default.FavoriteBorder),
    AppEntry("worldbook", "世界书", "设定、词条与规则", Icons.Default.Book),
    AppEntry("image", "生图工坊", "提示词与图片创作", Icons.Default.Image),
    AppEntry("music", "音乐", "曲库与播放", Icons.Default.MusicNote),
    AppEntry("album", "相册", "图片与收藏", Icons.Default.Album),
    AppEntry("diary", "日记", "记录日常片段", Icons.Default.Today),
    AppEntry("calendar", "日历", "日期与安排", Icons.Default.CalendarMonth),
    AppEntry("proactive", "主动消息", "陪伴通知与提醒", Icons.Default.AutoAwesome),
    AppEntry("settings", "设置", "模型、外观与数据", Icons.Default.Settings)
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
