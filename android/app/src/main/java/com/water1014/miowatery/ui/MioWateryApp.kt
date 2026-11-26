package com.water1014.miowatery.ui

import com.water1014.miowatery.R

import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import org.json.JSONObject
import com.water1014.miowatery.core.desktop.DesktopThemePackStore
import com.water1014.miowatery.core.desktop.DesktopThemePackInspector
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import com.water1014.miowatery.core.migration.LegacyBackupInspector
import com.water1014.miowatery.core.migration.LegacyBackupPreview
import com.water1014.miowatery.core.migration.LegacyBackupImporter
import com.water1014.miowatery.core.migration.LegacyImportResult
import com.water1014.miowatery.core.database.MioDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.collectAsState
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
import okhttp3.OkHttpClient
import okhttp3.Request

private data class AppEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: Int
)

// Desktop shortcuts mirror the original Mio small-phone pages.
// The four fixed dock entries are also present here for route lookup, but are not repeated in page grids.
private val appEntries = listOf(
    AppEntry("app", "聊天", "聊天与角色对话", R.drawable.mio_ic_app),
    AppEntry("worlds", "世界书", "世界设定与条目", R.drawable.mio_ic_worlds),
    AppEntry("personas", "联系人设定", "联系人与角色资料", R.drawable.mio_ic_personas),
    AppEntry("diary", "日记", "日记记录", R.drawable.mio_ic_diary),
    AppEntry("rolephones", "iPhone", "角色手机", R.drawable.mio_ic_rolephones),
    AppEntry("desktopcustom", "桌面美化", "桌面壁纸、图标与组件", R.drawable.mio_ic_desktopcustom),
    AppEntry("music", "音乐", "音乐播放与资料库", R.drawable.mio_ic_music),
    AppEntry("couple", "情侣空间", "关系与共同记录", R.drawable.mio_ic_couple),
    AppEntry("coread", "共读", "共同阅读", R.drawable.mio_ic_coread),
    AppEntry("offline", "线下", "线下相关内容", R.drawable.mio_ic_offline),
    AppEntry("msgs", "讯息", "独立消息应用", R.drawable.mio_ic_msgs),
    AppEntry("wallet", "钱包", "钱包与收支记录", R.drawable.mio_ic_wallet),
    AppEntry("mcp", "MCP", "服务与工具连接", R.drawable.mio_ic_mcp),
    AppEntry("preset", "预设", "提示词与配置预设", R.drawable.mio_ic_preset),
    AppEntry("life", "生活", "生活模拟", R.drawable.mio_ic_life),
    AppEntry("iff", "IF线", "分支剧情", R.drawable.mio_ic_iff),
    AppEntry("xinji", "心迹", "心迹互动", R.drawable.mio_ic_xinji),
    AppEntry("repo", "仓库", "作品仓库", R.drawable.mio_ic_repo),
    AppEntry("youtube", "YouTube", "视频与媒体", R.drawable.mio_ic_youtube),
    AppEntry("settings", "设置", "应用设置", R.drawable.mio_ic_settings),
)

@Composable
fun MioWateryApp() {
    val context = LocalContext.current
    var desktopPackJson by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(context) {
        desktopPackJson = withContext(Dispatchers.IO) { DesktopThemePackStore.load(context) }
    }
    var showSplash by remember { mutableStateOf(true) }
    var selectedPage by remember { mutableStateOf("home") }
    var selectedPersonaId by remember { mutableStateOf("default") }
    var selectedConversationId by remember { mutableStateOf("native-chat-default") }
    var chatReturnPage by remember { mutableStateOf("home") }
    val selectedEntry = appEntries.firstOrNull { it.id == selectedPage }

    BackHandler(enabled = selectedPage != "home") {
        selectedPage = when (selectedPage) {
            "chatmemory" -> "app"
            "backup" -> "settings"
            "app" -> chatReturnPage
            else -> "home"
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (selectedPage) {
            "home" -> HomeScreen(
                modifier = Modifier.fillMaxSize(),
                desktopPackJson = desktopPackJson,
                onOpen = { selectedPage = it }
            )
            "personas" -> PersonaScreen(
                onBack = { selectedPage = "home" },
                onOpenChat = { id ->
                    selectedPersonaId = id
                    chatReturnPage = "personas"
                    selectedPage = "app"
                },
                modifier = Modifier.fillMaxSize()
            )
            "app" -> ChatScreen(
                onBack = { selectedPage = chatReturnPage },
                onOpenMemory = { id -> selectedConversationId = id; selectedPage = "chatmemory" },
                personaId = selectedPersonaId,
                modifier = Modifier.fillMaxSize()
            )
            "music" -> MusicScreen(onBack = { selectedPage = "home" }, modifier = Modifier.fillMaxSize())
            "wallet" -> WalletScreen(onBack = { selectedPage = "home" }, modifier = Modifier.fillMaxSize())
            "msgs" -> MessageAppScreen(onBack = { selectedPage = "home" }, modifier = Modifier.fillMaxSize())
            "couple" -> CoupleScreen(onBack = { selectedPage = "home" }, modifier = Modifier.fillMaxSize())
            "coread" -> CoreadScreen(onBack = { selectedPage = "home" }, modifier = Modifier.fillMaxSize())
            "worlds" -> WorldbookScreen(onBack = { selectedPage = "home" }, modifier = Modifier.fillMaxSize())
            "desktopcustom" -> DesktopThemePackScreen(
                onBack = { selectedPage = "home" },
                onApplied = { json -> desktopPackJson = json; selectedPage = "home" },
                modifier = Modifier.fillMaxSize()
            )
            "diary" -> DiaryScreen(onBack = { selectedPage = "home" }, modifier = Modifier.fillMaxSize())
            "rolephones" -> RolePhoneScreen(onBack = { selectedPage = "home" }, modifier = Modifier.fillMaxSize())
            "chatmemory" -> MemoryScreen(
                onBack = { selectedPage = "app" },
                conversationId = selectedConversationId,
                modifier = Modifier.fillMaxSize()
            )
            "settings" -> SettingsScreen(
                modifier = Modifier.fillMaxSize(),
                onOpenBackup = { selectedPage = "backup" },
                onBack = { selectedPage = "home" }
            )
            "backup" -> BackupPreviewScreen(
                modifier = Modifier.fillMaxSize(),
                onBack = { selectedPage = "settings" }
            )
            else -> FeaturePlaceholder(
                title = selectedEntry?.title ?: "功能",
                modifier = Modifier.fillMaxSize(),
                onBack = { selectedPage = "home" }
            )
        }
        if (showSplash) {
            MioSplashScreen(onFinished = { showSplash = false })
        }
    }
}

@Composable
private fun BackupPreviewScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<LegacyBackupPreview?>(null) }
    var importResult by remember { mutableStateOf<LegacyImportResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("backup.json") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            importResult = null
            loading = true
            preview = null
            error = null
            scope.launch {
                try {
                    preview = withContext(Dispatchers.IO) {
                        val name = queryBackupName(context, uri)
                        val stream = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("无法打开所选文件")
                        LegacyBackupInspector.inspect(stream, name)
                    }
                    selectedName = queryBackupName(context, uri)
                } catch (failure: Exception) {
                    error = failure.message ?: "读取备份失败，请检查文件是否完整"
                } finally {
                    loading = false
                }
            }
        }
    }
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("返回") }
            Text("备份与旧版数据迁移", style = MaterialTheme.typography.titleMedium)
        }
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painter = painterResource(R.drawable.mio_ic_backup), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                Text("旧版数据迁移", style = MaterialTheme.typography.titleLarge)
                Text("选择旧版导出的 JSON 或 ZIP。导入前先检查内容，原始备份会另外保存在应用私有目录中。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { picker.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "*/*")) }, enabled = !loading && !importing) {
                    Text("选择备份文件")
                }
            }
        }
        if (loading || importing) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(if (importing) "正在导入资料、角色、世界书和聊天记录…" else "正在读取备份…")
                }
            }
        }
        error?.let { message ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        preview?.let { data ->
            Text("备份概览", style = MaterialTheme.typography.titleMedium)
            Text(data.sourceFormat + " · " + data.jsonFileName + " · " + (data.jsonBytes / 1024) + " KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BackupCountCard("用户资料", data.profileCount, Modifier.weight(1f))
                BackupCountCard("角色", data.personaCount, Modifier.weight(1f))
                BackupCountCard("世界书", data.worldbookCount, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BackupCountCard("聊天归属", data.chatOwnerCount, Modifier.weight(1f))
                BackupCountCard("会话", data.conversationCount, Modifier.weight(1f))
                BackupCountCard("消息", data.messageCount, Modifier.weight(1f))
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("检查提示", style = MaterialTheme.typography.titleSmall)
                    data.warnings.forEach { warning ->
                        Text("• " + warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("识别到的顶层字段", style = MaterialTheme.typography.titleSmall)
                    val extra = if (data.topLevelKeys.size > 18) " …（共 " + data.topLevelKeys.size + " 项）" else ""
                    Text(data.topLevelKeys.take(18).joinToString(" · ") + extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = {
                    val uri = selectedUri ?: return@Button
                    importing = true
                    error = null
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                val stream = context.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("无法重新读取备份文件")
                                val json = LegacyBackupImporter.readJson(stream, selectedName)
                                LegacyBackupImporter.importJson(context, json, MioDatabase.getInstance(context))
                            }
                            importResult = result
                        } catch (failure: Exception) {
                            error = failure.message ?: "导入失败，原始备份文件仍然保留"
                        } finally {
                            importing = false
                        }
                    }
                },
                enabled = !importing && !loading
            ) { Text("确认导入到原生版") }
        }
        importResult?.let { result ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("导入完成", style = MaterialTheme.typography.titleMedium)
                    Text("用户资料 " + result.profiles + " · 角色 " + result.personas + " · 世界书 " + result.worldbookEntries)
                    Text("会话 " + result.conversations + " · 消息 " + result.messages)
                    Text("原始备份已保存在应用私有目录：" + result.preservedBackupPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            "导入会将旧数据写入独立的 legacy ID，不覆盖当前默认角色和原生聊天。API 密钥与服务商配置不会自动导入；未知字段通过原始 JSON 备份保留。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

private suspend fun fetchHomeWeather(city: String): Pair<String, String> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
    val geoRequest = Request.Builder()
        .url("https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=zh&format=json")
        .build()
    val geoBody = client.newCall(geoRequest).execute().use { response ->
        if (!response.isSuccessful) error("城市查询失败（HTTP ${response.code}）")
        response.body?.string() ?: error("城市查询没有返回内容")
    }
    val result = JSONObject(geoBody).optJSONArray("results")?.optJSONObject(0)
        ?: error("没有找到这个城市，请检查名称")
    val latitude = result.getDouble("latitude")
    val longitude = result.getDouble("longitude")
    val displayName = result.optString("name", city)
    val forecastRequest = Request.Builder()
        .url("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code&timezone=auto")
        .build()
    val forecastBody = client.newCall(forecastRequest).execute().use { response ->
        if (!response.isSuccessful) error("天气请求失败（HTTP ${response.code}）")
        response.body?.string() ?: error("天气服务没有返回内容")
    }
    val current = JSONObject(forecastBody).optJSONObject("current") ?: error("天气数据格式不正确")
    val temp = current.optDouble("temperature_2m", Double.NaN)
    val code = current.optInt("weather_code", -1)
    val description = when (code) {
        0 -> "晴"
        1, 2 -> "少云"
        3 -> "阴"
        45, 48 -> "雾"
        51, 53, 55, 56, 57 -> "毛毛雨"
        61, 63, 65, 66, 67 -> "雨"
        71, 73, 75, 77 -> "降雪"
        80, 81, 82 -> "阵雨"
        85, 86 -> "阵雪"
        95, 96, 99 -> "雷雨"
        else -> "天气未知"
    }
    "${if (temp.isNaN()) "--" else temp.toInt().toString()}°C" to "$displayName · $description"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    desktopPackJson: String? = null,
    onOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val homeDao = remember(context) { MioDatabase.getInstance(context).mioDao() }
    val recentMessages by homeDao.observeRecentMessagePreviews().collectAsState(initial = emptyList())
    val desktopOverrides = remember(desktopPackJson) {
        runCatching {
            val desktop = JSONObject(desktopPackJson ?: "{}").optJSONObject("desktop")
            val iconsJson = desktop?.optJSONObject("icons") ?: JSONObject()
            buildMap {
                iconsJson.keys().forEach { key -> put(key, iconsJson.optString(key)) }
            }
        }.getOrDefault(emptyMap())
    }
    val desktopWallpaper = remember(desktopPackJson) {
        runCatching {
            JSONObject(desktopPackJson ?: "{}").optJSONObject("desktop")?.optString("wallpaper")
                ?.takeIf { !it.isNullOrBlank() && it != "null" }
        }.getOrNull()
    }
    val hiddenDesktopIds = remember(desktopPackJson) {
        runCatching {
            val desktop = JSONObject(desktopPackJson ?: "{}").optJSONObject("desktop")
            val hidden = desktop?.optJSONArray("hidden") ?: org.json.JSONArray()
            buildSet {
                for (index in 0 until hidden.length()) {
                    hidden.optString(index).takeIf { it.matches(Regex("[A-Za-z0-9_:.\\-]{1,80}")) }?.let(::add)
                }
            }
        }.getOrDefault(emptySet())
    }
    val nativeWidgets = remember(desktopPackJson, hiddenDesktopIds) {
        runCatching {
            desktopPackJson?.let { DesktopThemePackInspector.inspect(it).widgets }
                .orEmpty().filterNot { it.id in hiddenDesktopIds }
        }.getOrDefault(emptyList())
    }
    val pager = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val homePrefs = remember(context) { context.getSharedPreferences("mio_home_widgets", Context.MODE_PRIVATE) }
    var weatherCity by remember { mutableStateOf(homePrefs.getString("weather_city", "") ?: "") }
    var weatherTemperature by remember { mutableStateOf("--°") }
    var weatherDescription by remember { mutableStateOf("尚未设置城市") }
    var weatherLoading by remember { mutableStateOf(false) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    var editWeatherCity by remember { mutableStateOf(false) }
    var weatherCityDraft by remember { mutableStateOf(weatherCity) }
    LaunchedEffect(weatherCity) {
        if (weatherCity.isBlank()) {
            weatherTemperature = "--°"
            weatherDescription = "尚未设置城市"
            return@LaunchedEffect
        }
        weatherLoading = true
        weatherError = null
        try {
            val result = fetchHomeWeather(weatherCity)
            weatherTemperature = result.first
            weatherDescription = result.second
        } catch (failure: Exception) {
            weatherError = failure.message ?: "天气获取失败"
            weatherDescription = "暂时无法获取天气"
        } finally {
            weatherLoading = false
        }
    }
    var memoImage by remember { mutableStateOf(homePrefs.getString("memo_image", null)) }
    val memoImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            memoImage = uri.toString()
            homePrefs.edit().putString("memo_image", memoImage).apply()
        }
    }
    var recent1 by remember { mutableStateOf(homePrefs.getString("recent_1", "") ?: "") }
    var recent2 by remember { mutableStateOf(homePrefs.getString("recent_2", "") ?: "") }
    var recent3 by remember { mutableStateOf(homePrefs.getString("recent_3", "") ?: "") }
    var quoteLine1 by remember { mutableStateOf(homePrefs.getString("quote_line_1", "留一点空白，给今天的心情。") ?: "") }
    var quoteLine2 by remember { mutableStateOf(homePrefs.getString("quote_line_2", "Somewhere, we keep choosing each other.") ?: "") }
    var quoteImage1 by remember { mutableStateOf(homePrefs.getString("quote_image_1", "") ?: "") }
    var quoteImage2 by remember { mutableStateOf(homePrefs.getString("quote_image_2", "") ?: "") }
    var quoteImage3 by remember { mutableStateOf(homePrefs.getString("quote_image_3", "") ?: "") }
    var imessageTitle by remember { mutableStateOf(homePrefs.getString("imessage_title", "iMessage") ?: "") }
    var imessageSubtitle by remember { mutableStateOf(homePrefs.getString("imessage_subtitle", "对话预览 · 点击编辑") ?: "") }
    var imessage1 by remember { mutableStateOf(homePrefs.getString("imessage_1", "到家了吗？") ?: "") }
    var imessage2 by remember { mutableStateOf(homePrefs.getString("imessage_2", "刚到，放心。") ?: "") }
    var imessage3 by remember { mutableStateOf(homePrefs.getString("imessage_3", "那就好，早点休息。") ?: "") }
    var imessage4 by remember { mutableStateOf(homePrefs.getString("imessage_4", "好，晚安。") ?: "") }
    val musicPrefs = remember(context) { context.getSharedPreferences("mio_music_player", Context.MODE_PRIVATE) }
    var musicTitle by remember { mutableStateOf(musicPrefs.getString("track_name", "尚未选择歌曲") ?: "尚未选择歌曲") }
    var musicArtist by remember { mutableStateOf(musicPrefs.getString("artist_name", "未知艺人") ?: "未知艺人") }
    var musicRecent by remember { mutableStateOf(readRecentMusicTracks(context)) }
    var musicCover by remember { mutableStateOf(musicPrefs.getString("cover_uri", null)) }
    var musicPlaying by remember { mutableStateOf(musicPrefs.getBoolean("is_playing", false)) }
    var lyricLine1 by remember { mutableStateOf(musicPrefs.getString("lyric_1", "") ?: "") }
    var lyricLine2 by remember { mutableStateOf(musicPrefs.getString("lyric_2", "") ?: "") }
    var editSearches by remember { mutableStateOf(false) }
    var editQuote by remember { mutableStateOf(false) }
    var editIMessage by remember { mutableStateOf(false) }
    var editLyrics by remember { mutableStateOf(false) }
    var imageSlot by remember { mutableStateOf(0) }
    val desktopImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val value = uri.toString()
            when (imageSlot) {
                1 -> { quoteImage1 = value; homePrefs.edit().putString("quote_image_1", value).apply() }
                2 -> { quoteImage2 = value; homePrefs.edit().putString("quote_image_2", value).apply() }
                3 -> { quoteImage3 = value; homePrefs.edit().putString("quote_image_3", value).apply() }
                4 -> { musicCover = value; musicPrefs.edit().putString("cover_uri", value).apply() }
            }
        }
    }
    var now by remember { mutableStateOf(java.time.LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = java.time.LocalDateTime.now(); kotlinx.coroutines.delay(15_000) } }
    if (editWeatherCity) {
        AlertDialog(
            onDismissRequest = { editWeatherCity = false },
            title = { Text("设置天气城市") },
            text = {
                OutlinedTextField(
                    value = weatherCityDraft,
                    onValueChange = { weatherCityDraft = it },
                    label = { Text("城市名称") },
                    placeholder = { Text("例如：南京、Milan") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    weatherCity = weatherCityDraft.trim()
                    homePrefs.edit().putString("weather_city", weatherCity).apply()
                    editWeatherCity = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editWeatherCity = false }) { Text("取消") } }
        )
    }
    val ink = Color(0xFF0F1419)
    val sub = Color(0xFF6B7480)
    val panel = Color(0xFFF3F4F5)
    val dock = listOf("app", "worlds", "personas", "settings").filterNot { it in hiddenDesktopIds }.map { id -> appEntries.first { it.id == id } }
    Box(modifier.fillMaxSize()) {
        desktopWallpaper?.let { image ->
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            Modifier.fillMaxSize().background(if (desktopWallpaper == null) Color.White else Color.White.copy(alpha = 0.82f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().weight(1f), beyondViewportPageCount = 1) { page ->
            when (page) {
                0 -> Column(Modifier.fillMaxSize().widthIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 20.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), color = ink, fontSize = 60.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                        Text(now.format(java.time.format.DateTimeFormatter.ofPattern("M月d日  EEEE", java.util.Locale.SIMPLIFIED_CHINESE)), color = sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 7.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("备忘录图片", color = sub, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 3.dp, bottom = 8.dp))
                            Box(Modifier.fillMaxWidth().height(140.dp).background(Color(0xFFE2E4E7), RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).clickable { memoImagePicker.launch(arrayOf("image/*")) }, contentAlignment = Alignment.Center) {
                                if (!memoImage.isNullOrBlank()) AsyncImage(model = memoImage, contentDescription = "备忘录图片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Text("点击选择图片", color = sub, style = MaterialTheme.typography.labelSmall)
                            }
                            Text("更换图片", Modifier.align(Alignment.End).clickable { memoImagePicker.launch(arrayOf("image/*")) }.padding(top = 6.dp), color = sub, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(Modifier.weight(1f).height(174.dp).background(Color.White, RoundedCornerShape(22.dp)).clickable { weatherCityDraft = weatherCity; editWeatherCity = true }.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(weatherCity.ifBlank { "城市" }, color = ink, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            Text(weatherTemperature, color = ink, fontSize = 42.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp)
                            Text(if (weatherLoading) "正在更新…" else weatherError ?: weatherDescription, color = sub, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                            Text("设置城市", color = sub, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(22.dp)).padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("最近消息", color = ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("查看聊天", Modifier.clickable { onOpen("app") }, color = sub, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(10.dp))
                        if (recentMessages.isEmpty()) {
                            Text("还没有最近聊天", color = sub, style = MaterialTheme.typography.bodySmall)
                        } else {
                            recentMessages.forEach { message ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onOpen("app") }) {
                                    Text(message.conversationTitle.ifBlank { "聊天" }, color = ink, style = MaterialTheme.typography.labelMedium)
                                    Text((if (message.role == "user") "我：" else "角色：") + message.content, color = sub, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }
                            }
                        }
                    }
                }
                else -> {
                    val pageApps = when (page) {
                        1 -> listOf("diary", "rolephones", "desktopcustom", "music")
                        2 -> listOf("couple", "coread", "offline", "msgs")
                        else -> listOf("wallet", "mcp", "preset", "life", "iff", "xinji", "repo", "youtube")
                    }
                    val visibleApps = pageApps.filterNot { it in hiddenDesktopIds }.mapNotNull { id -> appEntries.firstOrNull { it.id == id } }
                    Column(
                        Modifier.fillMaxSize().widthIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)
                    ) {
                        if (page == 2) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { onOpen("music") },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Box(
                                            Modifier.size(104.dp).clip(RoundedCornerShape(14.dp)).background(panel),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!musicCover.isNullOrBlank()) {
                                                AsyncImage(model = musicCover, contentDescription = "音乐封面", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            } else {
                                                Text("♫", color = sub, fontSize = 34.sp)
                                            }
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(if (musicPlaying) "正在播放" else "最近播放", color = sub, style = MaterialTheme.typography.labelSmall)
                                            Text(musicTitle, color = ink, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                            Text(musicArtist, color = sub, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                            Text(if (musicPlaying) "正在播放 · 轻触打开控制" else "已暂停 · 轻触打开音乐", color = sub, style = MaterialTheme.typography.bodySmall)
                                            Text("打开音乐播放器  ›", color = ink, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 5.dp))
                                        }
                                    }
                                    Text("最近播放", color = ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    if (musicRecent.isEmpty()) {
                                        Text("还没有播放记录", color = sub, style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        musicRecent.forEach { track ->
                                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                                musicTitle = track.name
                                                musicArtist = track.artist
                                                musicCover = track.coverUri
                                                musicPrefs.edit()
                                                    .putString("track_uri", track.uri)
                                                    .putString("track_name", track.name)
                                                    .putString("artist_name", track.artist)
                                                    .putString("cover_uri", track.coverUri)
                                                    .putBoolean("play_on_open", true)
                                                    .putBoolean("is_playing", false)
                                                    .apply()
                                                onOpen("music")
                                            }.padding(vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                                Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(panel), contentAlignment = Alignment.Center) {
                                                    if (!track.coverUri.isNullOrBlank()) AsyncImage(model = track.coverUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                    else Text("♫", color = sub, fontSize = 17.sp)
                                                }
                                                Column(Modifier.weight(1f)) {
                                                    Text(track.name, color = ink, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                                    Text(track.artist, color = sub, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                }
                                                Text("播放", color = sub, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (page == 2) {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Card(
                                    modifier = Modifier.weight(1.25f).clickable { editIMessage = true },
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(Modifier.size(34.dp).background(Color(0xFFEEF0F1), CircleShape), contentAlignment = Alignment.Center) {
                                                Text("♡", color = Color(0xFF6B7480), fontSize = 17.sp)
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text(imessageTitle, color = Color(0xFF171719), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text(imessageSubtitle, color = Color(0xFF8B8B90), fontSize = 9.sp)
                                            }
                                            Text("•••", color = Color(0xFF8B8B90), fontSize = 12.sp)
                                        }
                                        Divider(color = Color(0xFFEAECEE), thickness = 0.7.dp)
                                        listOf(imessage1, imessage2, imessage3, imessage4).forEachIndexed { index, message ->
                                            if (message.isNotBlank()) {
                                                Row(
                                                    Modifier.fillMaxWidth().padding(top = 7.dp),
                                                    horizontalArrangement = if (index % 2 == 0) Arrangement.Start else Arrangement.End
                                                ) {
                                                    Box(
                                                        Modifier.widthIn(max = 190.dp)
                                                            .background(
                                                                if (index % 2 == 0) Color(0xFFE9E9ED) else Color(0xFF3982F6),
                                                                RoundedCornerShape(13.dp)
                                                            )
                                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                                    ) {
                                                        Text(
                                                            message,
                                                            color = if (index % 2 == 0) Color(0xFF171719) else Color.White,
                                                            fontSize = 10.sp,
                                                            lineHeight = 14.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Row(
                                            Modifier.fillMaxWidth().padding(top = 11.dp)
                                                .border(1.dp, Color(0xFFE5E5E8), RoundedCornerShape(18.dp))
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("+", color = Color(0xFF8B8B90), fontSize = 16.sp)
                                            Text("iMessage", Modifier.weight(1f).padding(start = 8.dp), color = Color(0xFF8B8B90), fontSize = 10.sp)
                                            Box(Modifier.size(19.dp).background(Color(0xFF3982F6), CircleShape), contentAlignment = Alignment.Center) { Text("↑", color = Color.White, fontSize = 11.sp) }
                                        }
                                    }
                                }
                                Column(
                                    Modifier.weight(0.75f),
                                    verticalArrangement = Arrangement.spacedBy(15.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    visibleApps.forEach { entry ->
                                        Column(
                                            Modifier.fillMaxWidth().clickable { onOpen(entry.id) },
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(Modifier.size(44.dp).background(Color.White, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                                AppEntryIcon(entry, desktopOverrides, 22.dp)
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(entry.title, color = ink, fontSize = 10.sp, lineHeight = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
                                        }
                                    }
                                }
                            }
                        }
                        if (page == 1) {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.weight(1.05f).height(214.dp),
                                    contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(22.dp),
                                    userScrollEnabled = false
                                ) {
                                    items(visibleApps) { entry ->
                                        Column(
                                            Modifier.fillMaxWidth().clickable { onOpen(entry.id) }.padding(vertical = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(Modifier.size(58.dp).background(panel, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                                                AppEntryIcon(entry, desktopOverrides, 29.dp)
                                            }
                                            Spacer(Modifier.height(7.dp))
                                            Text(entry.title, color = ink, fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center, maxLines = 2)
                                        }
                                    }
                                }
                                Column(Modifier.weight(1.05f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HomePanel("文案与照片", Modifier.fillMaxWidth().clickable { editQuote = true }) {
                                        Text(quoteLine1, color = ink, style = MaterialTheme.typography.titleSmall)
                                        if (quoteLine2.isNotBlank()) Text(quoteLine2, color = sub, style = MaterialTheme.typography.bodySmall)
                                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(quoteImage1, quoteImage2, quoteImage3).forEachIndexed { index, image ->
                                                Box(Modifier.weight(1f).height(82.dp).background(panel, RoundedCornerShape(12.dp)).clickable {
                                                    imageSlot = index + 1
                                                    desktopImagePicker.launch(arrayOf("image/*"))
                                                }, contentAlignment = Alignment.Center) {
                                                    if (image.isNotBlank()) AsyncImage(model = image, contentDescription = "文案照片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                    else Text("＋", color = sub, fontSize = 22.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                                Column(Modifier.weight(0.95f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HomePanel("Recent Searches", Modifier.fillMaxWidth().clickable { editSearches = true }) {
                                        Row(Modifier.fillMaxWidth().padding(bottom = 7.dp), horizontalArrangement = Arrangement.End) {
                                            Text("Clear All", color = Color(0xFF8E8E93), fontSize = 9.sp)
                                        }
                                        listOf(recent1, recent2, recent3).forEach { term ->
                                            Row(
                                                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                                            ) {
                                                Text("⌕", color = Color(0xFF111111), fontSize = 16.sp)
                                                Text(term.ifBlank { " " }, Modifier.weight(1f), color = Color(0xFF111111), fontSize = 10.sp, maxLines = 2)
                                            }
                                            if (term != recent3) Divider(color = Color(0xFFE3E3E5), thickness = 0.6.dp)
                                        }
                                    }
                                }
                            }
                            nativeWidgets.forEach { widget ->
                                HomePanel(widget.title.ifBlank { "桌面组件" }, Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                    when (widget.kind) {
                                        "clock" -> Text(now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), color = ink, fontSize = 34.sp, fontWeight = FontWeight.Light)
                                        "text" -> Text(widget.text.orEmpty(), color = sub, style = MaterialTheme.typography.bodyMedium)
                                        "image" -> widget.image?.let { image ->
                                            AsyncImage(model = image, contentDescription = widget.title, modifier = Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Crop)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.fillMaxWidth().height(if (page == 3) 270.dp else 154.dp),
                                contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 28.dp, bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(26.dp),
                                userScrollEnabled = false
                            ) {
                                items(visibleApps) { entry ->
                                    Column(
                                        Modifier.fillMaxWidth().clickable { onOpen(entry.id) }.padding(vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(Modifier.size(58.dp).background(panel, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                                            AppEntryIcon(entry, desktopOverrides, 29.dp)
                                        }
                                        Spacer(Modifier.height(7.dp))
                                        Text(entry.title, color = ink, fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center, maxLines = 2)
                                    }
                                }
                            }
                        }
                        if (page == 3) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { onOpen("music") },
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Box(
                                        Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(15.dp))
                                            .background(panel)
                                            .clickable { imageSlot = 4; desktopImagePicker.launch(arrayOf("image/*")) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!musicCover.isNullOrBlank()) {
                                            AsyncImage(model = musicCover, contentDescription = "音乐封面", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Text("♫", color = Color(0xFF8B8B90), fontSize = 54.sp)
                                        }
                                        Text(
                                            "更换封面",
                                            Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.56f))
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            color = Color.White,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("0:00", color = sub, fontSize = 9.sp)
                                        Box(Modifier.weight(1f).padding(horizontal = 8.dp).height(3.dp).background(Color(0xFFE5E7EA), CircleShape)) {
                                            Box(Modifier.fillMaxWidth(0.36f).height(3.dp).background(Color(0xFF8B8B90), CircleShape))
                                        }
                                        Text("−5:20", color = sub, fontSize = 9.sp)
                                    }
                                    Text(lyricLine1.ifBlank { "水循環｜下一秒接住的會是你上世紀的淚嗎" }, color = ink, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                                    Text(lyricLine2.ifBlank { "Were I your tear, I would glide down your cheek until I reached your lips, for I long to kiss you" }, color = sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 5.dp), lineHeight = 16.sp)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        androidx.compose.material3.TextButton(onClick = { editLyrics = true }) { Text("编辑歌词与封面文案") }
                                    }
                                }
                            }
                        }                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { repeat(4) { index -> Box(Modifier.padding(horizontal = 5.dp).size(if (pager.currentPage == index) 7.dp else 5.dp).background(if (pager.currentPage == index) ink else Color(0xFFD5D8DC), CircleShape).clickable { scope.launch { pager.animateScrollToPage(index) } }) } }
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 5.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            dock.forEach { entry -> Column(Modifier.weight(1f).clickable { onOpen(entry.id) }.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(54.dp).background(panel, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { AppEntryIcon(entry, desktopOverrides, 24.dp) }; Spacer(Modifier.height(5.dp)); Text(if (entry.id == "app") "聊天" else if (entry.id == "personas") "联系人设定" else entry.title, color = ink, fontSize = 11.sp) } }
        }
        }
    }
    if (editSearches) androidx.compose.material3.AlertDialog(
        onDismissRequest = { editSearches = false },
        title = { Text("编辑最近搜索") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedTextField(recent1, { recent1 = it }, label = { Text("搜索记录 1") }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(recent2, { recent2 = it }, label = { Text("搜索记录 2") }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(recent3, { recent3 = it }, label = { Text("搜索记录 3") }, singleLine = true)
        } },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { homePrefs.edit().putString("recent_1", recent1).putString("recent_2", recent2).putString("recent_3", recent3).apply(); editSearches = false }) { Text("保存") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { editSearches = false }) { Text("取消") } }
    )
    if (editQuote) androidx.compose.material3.AlertDialog(
        onDismissRequest = { editQuote = false },
        title = { Text("编辑文案") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedTextField(quoteLine1, { quoteLine1 = it }, label = { Text("主文案") }, minLines = 2)
            androidx.compose.material3.OutlinedTextField(quoteLine2, { quoteLine2 = it }, label = { Text("副文案") }, minLines = 2)
        } },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { homePrefs.edit().putString("quote_line_1", quoteLine1).putString("quote_line_2", quoteLine2).apply(); editQuote = false }) { Text("保存") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { editQuote = false }) { Text("取消") } }
    )
    if (editIMessage) androidx.compose.material3.AlertDialog(
        onDismissRequest = { editIMessage = false },
        title = { Text("编辑 iMessage 预览") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            androidx.compose.material3.OutlinedTextField(imessageTitle, { imessageTitle = it }, label = { Text("标题") }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(imessageSubtitle, { imessageSubtitle = it }, label = { Text("副标题") }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(imessage1, { imessage1 = it }, label = { Text("对话 1") }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(imessage2, { imessage2 = it }, label = { Text("对话 2") }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(imessage3, { imessage3 = it }, label = { Text("对话 3") }, singleLine = true)
            androidx.compose.material3.OutlinedTextField(imessage4, { imessage4 = it }, label = { Text("对话 4") }, singleLine = true)
        } },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { homePrefs.edit().putString("imessage_title", imessageTitle).putString("imessage_subtitle", imessageSubtitle).putString("imessage_1", imessage1).putString("imessage_2", imessage2).putString("imessage_3", imessage3).putString("imessage_4", imessage4).apply(); editIMessage = false }) { Text("保存") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { editIMessage = false }) { Text("取消") } }
    )
    if (editLyrics) androidx.compose.material3.AlertDialog(
        onDismissRequest = { editLyrics = false },
        title = { Text("编辑音乐歌词组件") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedTextField(lyricLine1, { lyricLine1 = it }, label = { Text("歌曲 / 歌词") }, minLines = 2)
            androidx.compose.material3.OutlinedTextField(lyricLine2, { lyricLine2 = it }, label = { Text("第二行歌词") }, minLines = 2)
        } },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { musicPrefs.edit().putString("lyric_1", lyricLine1).putString("lyric_2", lyricLine2).apply(); editLyrics = false }) { Text("保存") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { editLyrics = false }) { Text("取消") } }
    )
}

@Composable
private fun AppEntryIcon(entry: AppEntry, overrides: Map<String, String>, size: androidx.compose.ui.unit.Dp) {
    val customImage = overrides[entry.id]
    if (customImage != null) {
        AsyncImage(
            model = customImage,
            contentDescription = entry.title,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Crop
        )
    } else {
        Icon(
            painter = painterResource(entry.icon),
            contentDescription = entry.title,
            tint = Color(0xFF0F1419),
            modifier = Modifier.size(size)
        )
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
