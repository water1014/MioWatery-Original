package com.water1014.miowatery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.water1014.miowatery.core.network.ImageApiConfig
import com.water1014.miowatery.core.network.ImageApiConfigStore

@Composable
fun ImageApiSettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember(context) { ImageApiConfigStore(context) }
    var config by remember { mutableStateOf(ImageApiConfig()) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { config = store.load() }

    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(Modifier.fillMaxWidth(), shape = CardDefaults.shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("生图 API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("关闭后，角色不能自动发送 AI 生成的图片，但仍可在聊天中上传图片。生图配置与聊天模型 API 分开保存。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.enabled) Button(onClick = { config = config.copy(enabled = true); saved = false }, modifier = Modifier.weight(1f)) { Text("允许角色发图：开") }
                    else OutlinedButton(onClick = { config = config.copy(enabled = true); saved = false }, modifier = Modifier.weight(1f)) { Text("允许角色发图：开") }
                    if (!config.enabled) Button(onClick = { config = config.copy(enabled = false); saved = false }, modifier = Modifier.weight(1f)) { Text("关闭") }
                    else OutlinedButton(onClick = { config = config.copy(enabled = false); saved = false }, modifier = Modifier.weight(1f)) { Text("关闭") }
                }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = CardDefaults.shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("生图服务提供方", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.provider == "gpt") Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("GPT／OpenAI 兼容") }
                    else OutlinedButton(onClick = { config = config.copy(provider = "gpt"); saved = false }, modifier = Modifier.weight(1f)) { Text("GPT／OpenAI 兼容") }
                    if (config.provider == "novelai") Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("NovelAI") }
                    else OutlinedButton(onClick = { config = config.copy(provider = "novelai"); saved = false }, modifier = Modifier.weight(1f)) { Text("NovelAI") }
                }
            }
        }
        if (config.provider == "gpt") {
            SettingsCard("GPT Image") {
                Field("GPT Image Proxy / API 地址", config.gptBase, { config = config.copy(gptBase = it); saved = false }, "https://api.openai.com/v1")
                Field("GPT 生图 API 密钥", config.gptKey, { config = config.copy(gptKey = it); saved = false }, "填写独立的生图 Key", secret = true)
                Field("GPT 生图模型 ID", config.gptModel, { config = config.copy(gptModel = it); saved = false }, "gpt-image-1")
                Field("正向提示词前缀", config.gptPositive, { config = config.copy(gptPositive = it); saved = false }, "例如：高质量插画，细节丰富，电影级光影", lines = 3)
                Field("反向提示词", config.gptNegative, { config = config.copy(gptNegative = it); saved = false }, "例如：模糊、低清晰度、文字、水印", lines = 3)
                Field("补充细节提示词", config.gptDetail, { config = config.copy(gptDetail = it); saved = false }, "画风、构图、镜头、光线等固定要求", lines = 3)
                ChoiceRow("图片尺寸", listOf("1024x1024" to "正方形", "1024x1536" to "竖图", "1536x1024" to "横图", "auto" to "自动"), config.gptSize) { config = config.copy(gptSize = it); saved = false }
                ChoiceRow("质量", listOf("auto" to "自动", "low" to "低", "medium" to "中", "high" to "高"), config.gptQuality) { config = config.copy(gptQuality = it); saved = false }
                ChoiceRow("风格", listOf("" to "默认", "vivid" to "鲜明", "natural" to "自然"), config.gptStyle) { config = config.copy(gptStyle = it); saved = false }
                Field("每次生成张数（1～4）", config.gptCount.toString(), { config = config.copy(gptCount = it.toIntOrNull()?.coerceIn(1, 4) ?: 1); saved = false }, "1")
                Text("标准 GPT Images API 没有通用的 negative_prompt 参数，反向提示词会并入请求文本。不同中转站支持的尺寸、质量和风格参数可能不同。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            SettingsCard("NovelAI") {
                Field("NovelAI 中转站 / API 地址", config.naiBase, { config = config.copy(naiBase = it); saved = false }, "https://image.novelai.net")
                Field("NovelAI API 密钥 / Token", config.naiKey, { config = config.copy(naiKey = it); saved = false }, "NovelAI Token 或中转站 Key", secret = true)
                ChoiceRow("NovelAI 模型", listOf(
                    "nai-diffusion-4-5-full" to "NAI Diffusion 4.5 Full",
                    "nai-diffusion-4-5-curated" to "NAI Diffusion 4.5 Curated",
                    "nai-diffusion-4-full" to "NAI Diffusion 4 Full",
                    "nai-diffusion-4-curated-preview" to "NAI Diffusion 4 Curated",
                    "nai-diffusion-3" to "NAI Diffusion 3",
                    "nai-diffusion-furry-3" to "NAI Diffusion Furry 3"
                ), config.naiModel) { config = config.copy(naiModel = it); saved = false }
                Field("正向提示词前缀", config.naiPositive, { config = config.copy(naiPositive = it); saved = false }, "masterpiece, best quality, detailed illustration", lines = 3)
                Field("反向提示词（Undesired Content）", config.naiNegative, { config = config.copy(naiNegative = it); saved = false }, "lowres, blurry, bad anatomy, watermark", lines = 3)
                Field("深度 / 补充提示词", config.naiDetail, { config = config.copy(naiDetail = it); saved = false }, "额外细节、构图、画风、角色特征", lines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("宽度", config.naiWidth.toString(), { config = config.copy(naiWidth = it.toIntOrNull() ?: config.naiWidth); saved = false }, "832", modifier = Modifier.weight(1f))
                    Field("高度", config.naiHeight.toString(), { config = config.copy(naiHeight = it.toIntOrNull() ?: config.naiHeight); saved = false }, "1216", modifier = Modifier.weight(1f))
                }
                Field("提示词引导强度（Scale）", config.naiScale.toString(), { config = config.copy(naiScale = it.toFloatOrNull() ?: config.naiScale); saved = false }, "5")
                Field("步数（Steps）", config.naiSteps.toString(), { config = config.copy(naiSteps = it.toIntOrNull() ?: config.naiSteps); saved = false }, "28")
                ChoiceRow("采样器（Sampler）", listOf(
                    "k_euler_ancestral" to "Euler Ancestral", "k_euler" to "Euler",
                    "k_dpmpp_2s_ancestral" to "DPM++ 2S Ancestral",
                    "k_dpmpp_2m" to "DPM++ 2M", "k_dpmpp_sde" to "DPM++ SDE"
                ), config.naiSampler) { config = config.copy(naiSampler = it); saved = false }
                Field("CFG 重缩放", config.naiCfgRescale.toString(), { config = config.copy(naiCfgRescale = it.toFloatOrNull() ?: config.naiCfgRescale); saved = false }, "0")
                Field("随机种子（留空为随机）", config.naiSeed, { config = config.copy(naiSeed = it); saved = false }, "随机")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChoice("启用 SM", config.naiSm, Modifier.weight(1f)) { config = config.copy(naiSm = it); saved = false }
                    ToggleChoice("启用 SM Dynamic", config.naiSmDynamic, Modifier.weight(1f)) { config = config.copy(naiSmDynamic = it); saved = false }
                }
                Text("参数会发送给兼容的 NovelAI 接口；不同模型和中转站对参数的支持可能不同。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = { store.save(config); saved = true }, modifier = Modifier.fillMaxWidth()) { Text("保存生图设置") }
        if (saved) Text("生图设置已保存到本机。API 密钥使用 Android Keystore 加密保存。", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        Text("此页先完成原版配置项和本机持久化。生图请求、配置测试和预设应用会在接下来的原生迁移中接通，不会把未接通的测试按钮伪装成可用。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = CardDefaults.shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun Field(
    label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "",
    secret: Boolean = false, lines: Int = 1, modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label) }, placeholder = { Text(placeholder) },
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        minLines = lines, maxLines = if (lines == 1) 1 else 5
    )
}

@Composable
private fun ChoiceRow(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        options.chunked(2).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.forEach { (value, label) ->
                    if (selected == value) Button(onClick = {}, modifier = Modifier.weight(1f)) { Text(label) }
                    else OutlinedButton(onClick = { onSelect(value) }, modifier = Modifier.weight(1f)) { Text(label) }
                }
                if (group.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ToggleChoice(title: String, checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    if (checked) Button(onClick = { onChange(false) }, modifier = modifier) { Text("$title：开") }
    else OutlinedButton(onClick = { onChange(true) }, modifier = modifier) { Text("$title：关") }
}
