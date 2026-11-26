package com.water1014.miowatery.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import com.water1014.miowatery.ui.theme.LocalMioThemeMode
import com.water1014.miowatery.core.network.ApiConfigStore
import com.water1014.miowatery.core.network.SavedApiConfig
import kotlinx.coroutines.launch


@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onOpenBackup: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var section by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val themeMode = LocalMioThemeMode.current
    var profileName by remember { mutableStateOf("") }
    var profileHandle by remember { mutableStateOf("") }
    var profileBio by remember { mutableStateOf("") }
    var profileAvatar by remember { mutableStateOf("") }
    var profileSaved by remember { mutableStateOf(false) }
    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            profileAvatar = uri.toString()
            profileSaved = false
        }
    }
    BackHandler(enabled = section != null) { section = null }

    LaunchedEffect(section) {
        if (section == "profile") {
            val prefs = context.getSharedPreferences("miowatery_profile", 0)
            profileName = prefs.getString("name", "") ?: ""
            profileHandle = prefs.getString("handle", "") ?: ""
            profileBio = prefs.getString("bio", "") ?: ""
            profileAvatar = prefs.getString("avatar", "") ?: ""
            profileSaved = false
        }
    }

    val sections = listOf(
        "我的资料" to "profile",
        "外观" to "appearance",
        "心声" to "innervoice",
        "模型与 API" to "api",
        "生图 API" to "imageapi",
        "语音（MiniMax）" to "voice",
        "AI 角色扮演提示词" to "prompt",
        "记忆模式" to "memory",
        "系统（后台保活与通知）" to "system",
        "悬浮球" to "fball",
        "数据与备份" to "data"
    )

    if (section == "api") {
        Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { section = null }) { Text("‹ 设置") }
                Text("模型与 API", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(1.dp))
            }
            ApiSettingsScreen(modifier = Modifier.weight(1f), onOpenBackup = onOpenBackup)
        }
        return
    }

    if (section == "profile") {
        Column(
            modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { section = null }) { Text("‹ 设置") }
                Text("我的资料", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(1.dp))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            profilePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(68.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = profileAvatar,
                                    contentDescription = "个人头像",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(profileName.take(1).ifBlank { "我" }, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("更换头像", fontWeight = FontWeight.SemiBold)
                            Text("点击从相册选择", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it; profileSaved = false },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("名字") },
                        placeholder = { Text("我的名字") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = profileHandle,
                        onValueChange = { profileHandle = it; profileSaved = false },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("用户名（@）") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = profileBio,
                        onValueChange = { profileBio = it; profileSaved = false },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("个人简介") },
                        minLines = 3,
                        maxLines = 5
                    )
                    Text(
                        "个人简介用于个人资料展示，并可与角色共享。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (profileSaved) {
                        Text("资料已保存", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            context.getSharedPreferences("miowatery_profile", 0).edit()
                                .putString("name", profileName.trim())
                                .putString("handle", profileHandle.trim().removePrefix("@"))
                                .putString("bio", profileBio.trim())
                                .putString("avatar", profileAvatar)
                                .apply()
                            profileSaved = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("保存资料") }
                }
            }
        }
        return
    }

    if (section == "appearance") {
        val options = listOf("light" to "浅色", "dark" to "深色", "auto" to "跟随系统")
        Column(
            modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { section = null }) { Text("‹ 设置") }
                Text("外观", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(1.dp))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("外观模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { (value, label) ->
                            val selected = themeMode.value == value
                            if (selected) {
                                Button(onClick = {}) { Text(label) }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        themeMode.value = value
                                        context.getSharedPreferences("miowatery_appearance", 0)
                                            .edit().putString("theme_mode", value).apply()
                                    }
                                ) { Text(label) }
                            }
                        }
                    }
                    Text(
                        when (themeMode.value) {
                            "dark" -> "当前使用深色外观"
                            "auto" -> "外观会跟随 Android 系统的深浅色设置"
                            else -> "当前使用浅色外观"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text("主题选择会立即应用，并保存在本机。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    if (section == "data") {
        Column(
            modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { section = null }) { Text("‹ 设置") }
                Text("数据与备份", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(1.dp))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("数据与备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("检查旧版导出文件，并将原有资料迁移到原生 App。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onOpenBackup) { Text("备份与旧版数据迁移") }
                }
            }
        }
        return
    }

    if (section != null) {
        Column(
            modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { section = null }) { Text("‹ 设置") }
                Text(sections.firstOrNull { it.second == section }?.first ?: "设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(1.dp))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("尚未完成原生迁移", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("这个分类会按照 MioWatery 原版的设置项和行为继续迁移。目前先保留原版入口，不用相似功能代替，也不会擅自改写设置内容。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(1.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { section = "profile" }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("我的资料", fontWeight = FontWeight.SemiBold)
                    Text("头像、账号与个人简介", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                sections.drop(1).forEachIndexed { index, item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { section = item.second }.padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.first, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.second == "appearance") {
                                Text(
                                    when (themeMode.value) { "dark" -> "深色"; "auto" -> "跟随系统"; else -> "浅色" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index != sections.size - 2) {
                        androidx.compose.material3.Divider()
                    }
                }
            }
        }
        Text("选择一个分类来配置相关设置。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ApiSettingsScreen(
    modifier: Modifier = Modifier,
    onOpenBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { ApiConfigStore(context) }
    val saved by store.config.collectAsState(initial = SavedApiConfig())

    var baseUrl by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var headers by remember { mutableStateOf("") }
    var secondaryBaseUrl by remember { mutableStateOf("") }
    var secondaryModel by remember { mutableStateOf("") }
    var secondaryApiKey by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saved) {
        baseUrl = saved.baseUrl
        model = saved.model
        apiKey = saved.apiKey
        headers = saved.customHeadersJson
        secondaryBaseUrl = saved.secondaryBaseUrl
        secondaryModel = saved.secondaryModel
        secondaryApiKey = saved.secondaryApiKey
    }

    Column(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("模型与 API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "配置 OpenAI 兼容接口。保存后，聊天和记忆整理会共用这套连接配置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("快捷配置", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            baseUrl = "https://opencode.ai/zen/go"
                            savedMessage = null
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("OpenCode Go") }
                    OutlinedButton(
                        onClick = {
                            baseUrl = "https://api.deepseek.com/v1"
                            model = "deepseek-chat"
                            savedMessage = null
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("DeepSeek") }
                }
                OutlinedButton(
                    onClick = {
                        savedMessage = null
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("通用 OpenAI 兼容接口（保留当前地址和模型）") }
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; savedMessage = null; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API 地址") },
                    placeholder = { Text("https://example.com/v1") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; savedMessage = null; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模型名称") },
                    placeholder = { Text("填写服务商提供的模型 ID") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; savedMessage = null; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = headers,
                    onValueChange = { headers = it; savedMessage = null; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("自定义请求头（JSON，可留空）") },
                    placeholder = { Text("{\"X-Example\": \"value\"}") },
                    minLines = 3,
                    maxLines = 6
                )
                Text(
                    "请求头必须是 JSON 对象。OpenCode 地址会自动生成会话标识；其他服务商需要的额外请求头可在这里填写。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                savedMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = {
                        errorMessage = null
                        savedMessage = null
                        val rawHeaders = headers.trim()
                        if (rawHeaders.isNotEmpty()) {
                            val parsedHeaders = try {
                                JsonParser.parseString(rawHeaders)
                            } catch (_: Exception) {
                                errorMessage = "请求头 JSON 格式错误，请检查引号、逗号和括号"
                                return@Button
                            }
                            if (!parsedHeaders.isJsonObject) {
                                errorMessage = "自定义请求头必须是 JSON 对象"
                                return@Button
                            }
                            val invalidHeader = parsedHeaders.asJsonObject.entrySet().firstOrNull { entry ->
                                entry.key.isBlank() || !entry.value.isJsonPrimitive
                            }
                            if (invalidHeader != null) {
                                errorMessage = "请求头名称不能为空，值必须是字符串、数字或布尔值"
                                return@Button
                            }
                        }
                        saving = true
                        scope.launch {
                            try {
                                store.save(SavedApiConfig(
                                    baseUrl = baseUrl.trim(),
                                    model = model.trim(),
                                    apiKey = apiKey.trim(),
                                    customHeadersJson = headers.trim(),
                                    secondaryBaseUrl = secondaryBaseUrl.trim(),
                                    secondaryModel = secondaryModel.trim(),
                                    secondaryApiKey = secondaryApiKey.trim()
                                ))
                                savedMessage = "设置已保存"
                            } catch (failure: Exception) {
                                errorMessage = failure.message ?: "保存失败"
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving
                ) {
                    Text(if (saving) "正在保存…" else "保存 API 设置")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("副 API（可选）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "按照 MioWatery 原版逻辑，角色手机内容生成优先使用副 API。某一项留空时，会自动回退到主 API 对应配置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = secondaryBaseUrl,
                    onValueChange = { secondaryBaseUrl = it; savedMessage = null; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("副 API 地址") },
                    placeholder = { Text("留空则使用主 API 地址") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = secondaryModel,
                    onValueChange = { secondaryModel = it; savedMessage = null; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("副 API 模型 ID") },
                    placeholder = { Text("留空则使用主 API 模型") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = secondaryApiKey,
                    onValueChange = { secondaryApiKey = it; savedMessage = null; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("副 API Key") },
                    placeholder = { Text("留空则使用主 API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Text(
                    "副 API 密钥使用 Android Keystore 加密保存。自定义请求头沿用主 API 的请求头设置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("数据管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "导入旧版备份前会先检查文件内容。导入不会要求你先清空当前数据库。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpenBackup) { Text("备份与旧版数据迁移") }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("应用信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("MioWatery 原生版", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "当前为原生迁移版本，尚未迁移的原版模块会继续按原始功能逐项补齐。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
