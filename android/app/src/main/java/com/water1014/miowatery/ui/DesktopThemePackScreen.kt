package com.water1014.miowatery.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.water1014.miowatery.core.desktop.DesktopThemePackInspector
import com.water1014.miowatery.core.desktop.DesktopThemePackPreview
import com.water1014.miowatery.core.desktop.DesktopThemePackStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DesktopThemePackScreen(
    onBack: () -> Unit,
    onApplied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<DesktopThemePackPreview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showApplyConfirm by remember { mutableStateOf(false) }
    var savedName by remember { mutableStateOf<String?>(null) }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) readPack(context, uri, scope, { busy = it }) { p, e ->
            preview = p
            error = e
        }
    }
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = preview?.let(DesktopThemePackStore::serialize)
        if (uri != null && json != null) {
            scope.launch {
                busy = true
                error = null
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) }
                            ?: throw IllegalArgumentException("无法创建美化包文件")
                    }
                    savedName = preview?.name
                } catch (failure: Exception) {
                    error = failure.message ?: "导出失败"
                } finally {
                    busy = false
                }
            }
        }
    }

    if (showApplyConfirm && preview != null) {
        AlertDialog(
            onDismissRequest = { showApplyConfirm = false },
            title = { Text("应用这套桌面美化？") },
            text = {
                Text("将保存壁纸、\${preview!!.iconCount} 个图标、\${preview!!.widgets.size} 个受支持的小组件及摆放信息。不会修改聊天、角色、世界书或 API 配置。网页自定义代码不会被执行。")
            },
            confirmButton = {
                TextButton(onClick = {
                    val safeJson = preview?.let(DesktopThemePackStore::serialize) ?: return@TextButton
                    DesktopThemePackStore.save(context, safeJson)
                    showApplyConfirm = false
                    onApplied(safeJson)
                }) { Text("确认应用") }
            },
            dismissButton = {
                TextButton(onClick = { showApplyConfirm = false }) { Text("再检查一下") }
            }
        )
    }

    Column(
        modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("桌面美化包", style = MaterialTheme.typography.titleLarge)
                Text(
                    "导入 MioWatery 网页版美化包，先检查内容，再决定是否应用到原生桌面配置。原生版只读取受支持的图片和组件数据，不运行网页脚本。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { openDocument.launch(arrayOf("application/json", "text/plain", "*/*")) }, enabled = !busy) {
                        Text("选择美化包")
                    }
                    OutlinedButton(onClick = onBack) { Text("返回") }
                }
            }
        }

        if (busy) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Text("正在处理美化包…")
            }
        }

        error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        preview?.let { data ->
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(data.name, style = MaterialTheme.typography.titleMedium)
                    Text("文件已通过格式检查，可以预览。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CountPill("壁纸", if (data.wallpaper == null) "无" else "有")
                        CountPill("图标", data.iconCount.toString())
                        CountPill("组件", data.widgets.size.toString())
                    }
                    Text("组件预览", style = MaterialTheme.typography.titleSmall)
                    if (data.widgets.isEmpty()) {
                        Text("这个美化包没有可迁移的原生组件。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    } else {
                        data.widgets.forEach { widget ->
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(widget.title.ifBlank { widget.kind }, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    when (widget.kind) {
                                        "clock" -> "时钟组件"
                                        "text" -> widget.text.orEmpty().take(160)
                                        "image" -> "图片组件 · 图片地址已通过安全检查"
                                        else -> widget.kind
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (data.hiddenWidgetIds.isNotEmpty()) {
                        Text("隐藏的内置组件：\${data.hiddenWidgetIds.size} 项", style = MaterialTheme.typography.bodySmall)
                    }
                    if (data.placementGroups.isNotEmpty()) {
                        Text("检测到 \${data.placementGroups.values.sumOf { it.size }} 个摆放位置记录", style = MaterialTheme.typography.bodySmall)
                    }
                    if (data.unsupportedCustomWidgetCount > 0 || data.warnings.isNotEmpty()) {
                        Text("安全与兼容提示", style = MaterialTheme.typography.titleSmall)
                        data.warnings.forEach { warning ->
                            Text("• $warning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { showApplyConfirm = true }, enabled = !busy) { Text("确认应用") }
                        OutlinedButton(
                            onClick = { createDocument.launch("\${data.name.replace(Regex("[\\\\\\\\/:*?\\\"<>|]"), "_")}.json") },
                            enabled = !busy
                        ) { Text("导出安全版") }
                    }
                }
            }
        }

        savedName?.let {
            Text("已导出：$it.json", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "安全说明：网页自定义组件可能包含 HTML、CSS 和 JavaScript。为保护原生 App 和本地资料，这些代码不会导入或执行；只迁移原生版明确支持的壁纸、图标、时钟、文字和图片组件。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CountPill(label: String, value: String) {
    Column(
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun readPack(
    context: Context,
    uri: Uri,
    scope: kotlinx.coroutines.CoroutineScope,
    onBusy: (Boolean) -> Unit,
    onResult: (DesktopThemePackPreview?, String?) -> Unit
) {
    onBusy(true)
    scope.launch {
        try {
            val json = withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("无法打开所选文件")
                input.use { stream ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 32 * 1024 * 1024) { "文件超过原生版 32 MB 的安全读取上限" }
                        output.write(buffer, 0, count)
                    }
                    output.toString(Charsets.UTF_8.name())
                }
            }
            val parsed = withContext(Dispatchers.Default) { DesktopThemePackInspector.inspect(json) }
            onResult(parsed, null)
        } catch (failure: Exception) {
            onResult(null, failure.message ?: "读取美化包失败")
        } finally {
            onBusy(false)
        }
    }
}
