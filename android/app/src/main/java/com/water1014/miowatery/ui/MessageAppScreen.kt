package com.water1014.miowatery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water1014.miowatery.core.database.MessageAppEntryEntity
import com.water1014.miowatery.core.database.MioDatabase
import com.water1014.miowatery.core.network.ApiConfigStore
import com.water1014.miowatery.core.network.ApiConnectionConfig
import com.water1014.miowatery.core.network.ApiHeaderResolver
import com.water1014.miowatery.core.network.ApiSessionStore
import com.water1014.miowatery.core.network.ChatCompletionClient
import com.water1014.miowatery.core.network.ChatCompletionRequest
import com.water1014.miowatery.core.network.ChatMessagePayload
import com.water1014.miowatery.core.network.DataStoreApiSessionStore
import com.water1014.miowatery.core.network.SavedApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

private const val MESSAGE_PROFILE_ID = "default"

@Composable
fun MessageAppScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val scope = rememberCoroutineScope()
    val personas by dao.observePersonas(MESSAGE_PROFILE_ID).collectAsState(initial = emptyList())
    var selectedPersonaId by remember { mutableStateOf("") }
    val selectedPersona = personas.firstOrNull { it.personaId == selectedPersonaId } ?: personas.firstOrNull()
    val entries by dao.observeMessageAppEntries(MESSAGE_PROFILE_ID, selectedPersona?.personaId ?: "").collectAsState(initial = emptyList())
    val config by remember { ApiConfigStore(context).config }.collectAsState(initial = SavedApiConfig())
    var tab by remember { mutableStateOf("sms") }
    var draft by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var choosePersona by remember { mutableStateOf(false) }

    fun sendAction() {
        if (busy) return
        val persona = selectedPersona
        if (persona == null) {
            error = "请先在联系人设定中添加角色。"
            return
        }
        if (config.baseUrl.isBlank() || config.model.isBlank() || config.apiKey.isBlank()) {
            error = "请先在聊天设置中填写 API 地址、模型和密钥。"
            return
        }
        val note = draft.trim()
        if (tab == "qa" && note.isBlank()) {
            error = "先写下想问的问题。"
            return
        }
        draft = ""
        error = null
        busy = true
        scope.launch {
            try {
                val prompt = runCatching { JSONObject(persona.rawLegacyJson).optString("prompt") }.getOrDefault("")
                val conversationId = "message-app-${persona.personaId}-${tab}"
                val system = if (tab == "sms") {
                    "【Mio 讯息 App】你现在用老式功能机短信，主动给用户发一条符合你人设和关系的话。可以是想念、碎碎念、关心或约定。不要 emoji、颜文字、Markdown，不要出戏。限制 80 字。只输出 JSON：{\"text\":\"正文\"}。\n角色设定：$prompt"
                } else {
                    "【Mio 匿名问答】你有一个匿名提问箱。用你的人设回答问题，1-3 句话，不要 emoji，不要出戏，不要复述问题。只输出回答正文。\n角色设定：$prompt"
                }
                val user = when {
                    tab == "sms" && note.isBlank() -> "请主动给用户发一条短信。"
                    tab == "sms" -> "用户刚刚回复你：$note。请再回一条短信。"
                    else -> "请回答这个问题：$note"
                }
                val result = withContext(Dispatchers.IO) {
                    val sessions: ApiSessionStore = DataStoreApiSessionStore(context)
                    ChatCompletionClient(ApiHeaderResolver(sessions)).complete(
                        ApiConnectionConfig(
                            baseUrl = config.baseUrl,
                            apiKey = config.apiKey,
                            customHeadersJson = config.customHeadersJson,
                            userProfileId = MESSAGE_PROFILE_ID,
                            conversationId = conversationId
                        ),
                        ChatCompletionRequest(
                            model = config.model,
                            messages = listOf(ChatMessagePayload.text("system", system), ChatMessagePayload.text("user", user)),
                            stream = false
                        )
                    )
                }
                val output = result.content.trim()
                if (output.isBlank()) throw IllegalStateException("模型没有返回内容。")
                val now = System.currentTimeMillis()
                if (tab == "sms") {
                    val smsText = runCatching { JSONObject(output).optString("text").ifBlank { output } }.getOrDefault(output)
                    dao.upsertMessageAppEntry(
                        MessageAppEntryEntity(
                            id = UUID.randomUUID().toString(),
                            profileId = MESSAGE_PROFILE_ID,
                            personaId = persona.personaId,
                            kind = "sms",
                            text = smsText,
                            reply = note,
                            author = persona.name,
                            createdAt = now
                        )
                    )
                } else {
                    dao.upsertMessageAppEntry(
                        MessageAppEntryEntity(
                            id = UUID.randomUUID().toString(),
                            profileId = MESSAGE_PROFILE_ID,
                            personaId = persona.personaId,
                            kind = "qa",
                            text = note,
                            reply = output,
                            author = if (anonymous) "匿名" else "我",
                            createdAt = now
                        )
                    )
                }
            } catch (failure: Exception) {
                error = failure.message ?: "请求失败，请检查网络和 API 配置。"
            } finally {
                busy = false
            }
        }
    }

    fun receiveAnonymousBatch() {
        val persona = selectedPersona
        if (persona == null || busy) {
            if (persona == null) error = "请先添加角色。"
            return
        }
        if (config.baseUrl.isBlank() || config.model.isBlank() || config.apiKey.isBlank()) {
            error = "请先在聊天设置中填写 API 地址、模型和密钥。"
            return
        }
        busy = true
        error = null
        scope.launch {
            try {
                val prompt = runCatching { JSONObject(persona.rawLegacyJson).optString("prompt") }.getOrDefault("")
                val result = withContext(Dispatchers.IO) {
                    val sessions: ApiSessionStore = DataStoreApiSessionStore(context)
                    ChatCompletionClient(ApiHeaderResolver(sessions)).complete(
                        ApiConnectionConfig(config.baseUrl, config.apiKey, config.customHeadersJson, MESSAGE_PROFILE_ID, "message-app-${persona.personaId}-pass"),
                        ChatCompletionRequest(
                            model = config.model,
                            stream = false,
                            messages = listOf(
                                ChatMessagePayload.text("system", "【匿名问答】请模拟3位互不相识的路人匿名投来的问题，问题贴合角色身份和近况，并由角色选择回答。回答1-3句话，不要emoji。只输出 JSON：{\"items\":[{\"q\":\"问题\",\"a\":\"回答\"}]}。角色设定：$prompt"),
                                ChatMessagePayload.text("user", "请生成这一批匿名提问和回答。")
                            )
                        )
                    )
                }
                val raw = result.content.trim()
                val json = JSONObject(raw.substring(raw.indexOf('{').coerceAtLeast(0), raw.lastIndexOf('}') + 1))
                val items = json.optJSONArray("items") ?: throw IllegalStateException("模型没有返回有效的问题列表。")
                val now = System.currentTimeMillis()
                val batch = (0 until minOf(items.length(), 5)).mapNotNull { index ->
                    val item = items.optJSONObject(index) ?: return@mapNotNull null
                    val question = item.optString("q").trim()
                    if (question.isBlank()) return@mapNotNull null
                    MessageAppEntryEntity(
                        id = UUID.randomUUID().toString(),
                        profileId = MESSAGE_PROFILE_ID,
                        personaId = persona.personaId,
                        kind = "pass",
                        text = question,
                        reply = item.optString("a"),
                        author = "匿名路人",
                        createdAt = now + index
                    )
                }
                if (batch.isEmpty()) throw IllegalStateException("没有生成有效问题。")
                dao.upsertMessageAppEntries(batch)
            } catch (failure: Exception) {
                error = failure.message ?: "收取失败，请检查 API 配置。"
            } finally {
                busy = false
            }
        }
    }

    Column(modifier.fillMaxSize().background(Color(0xFFF7F7F8)).imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", Modifier.clip(CircleShape).clickable(onClick = onBack).padding(horizontal = 8.dp), fontSize = 30.sp, color = Color(0xFF171719))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("没话讲 我们在互相假装…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF171719))
                Text(selectedPersona?.name ?: "选择角色", fontSize = 11.sp, color = Color(0xFF92949A))
            }
            Text("•••", Modifier.clickable { choosePersona = true }.padding(8.dp), color = Color(0xFF777B82))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("sms" to "传讯", "qa" to "匿名问答").forEach { (id, label) ->
                val active = tab == id
                Text(
                    label,
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .background(if (active) Color(0xFF171719) else Color(0xFFE9EAEC))
                        .clickable { tab = id; error = null }.padding(vertical = 11.dp),
                    color = if (active) Color.White else Color(0xFF555A61),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
        if (selectedPersona == null) {
            Text("还没有联系人，请先添加一个角色。", Modifier.padding(24.dp), color = Color(0xFF777B82))
        } else {
            if (tab == "sms") {
                Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("功能机短信", color = Color(0xFF777B82), fontSize = 11.sp)
                        Text(entries.firstOrNull { it.kind == "sms" }?.text ?: "暂无短信", color = Color(0xFF171719), fontSize = 15.sp, lineHeight = 23.sp)
                        entries.firstOrNull { it.kind == "sms" }?.reply?.takeIf { it.isNotBlank() }?.let { Text("你的回复：$it", color = Color(0xFF777B82), fontSize = 12.sp) }
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries.filter { it.kind == "sms" }, key = { it.id }) { entry ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(13.dp)) {
                                Text(java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(entry.createdAt)), fontSize = 10.sp, color = Color(0xFF92949A))
                                Text(entry.text, fontSize = 13.sp, color = Color(0xFF171719))
                                if (entry.reply.isNotBlank()) Text("回复：${entry.reply}", fontSize = 11.sp, color = Color(0xFF777B82))
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("匿名提问箱", Modifier.weight(1f), color = Color(0xFF171719), fontWeight = FontWeight.SemiBold)
                    Text(if (anonymous) "匿名" else "实名", Modifier.clip(RoundedCornerShape(14.dp)).clickable { anonymous = !anonymous }.background(Color(0xFFE9EAEC)).padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp, color = Color(0xFF555A61))
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(entries.filter { it.kind != "sms" }, key = { it.id }) { entry ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(entry.author, fontSize = 10.sp, color = Color(0xFF92949A))
                                Text(entry.text, fontSize = 14.sp, color = Color(0xFF171719))
                                Text(entry.reply.ifBlank { "暂未回答" }, fontSize = 13.sp, color = Color(0xFF626971), lineHeight = 20.sp)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { scope.launch { dao.deleteMessageAppEntry(entry.id, MESSAGE_PROFILE_ID) } }) { Text("删除") }
                                }
                            }
                        }
                    }
                }
            }
        }
        error?.let { Text(it, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
        if (busy) Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("正在生成…", Modifier.padding(start = 9.dp), fontSize = 12.sp, color = Color(0xFF777B82))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (tab == "sms") "给 TA 回一句，留空则让 TA 主动发" else "写下想问 TA 的问题") },
                maxLines = 3
            )
            Button(onClick = { sendAction() }, enabled = !busy && (tab == "sms" || draft.isNotBlank())) {
                Text(if (tab == "sms") "发送" else "提问")
            }
        }
        if (tab == "qa") {
            TextButton(onClick = { receiveAnonymousBatch() }, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("收取一批匿名提问")
            }
        }
    }

    if (choosePersona) AlertDialog(
        onDismissRequest = { choosePersona = false },
        title = { Text("选择角色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                personas.forEach { p ->
                    Text(p.name, Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { selectedPersonaId = p.personaId; choosePersona = false }.padding(12.dp), color = Color(0xFF171719))
                }
            }
        },
        confirmButton = { TextButton(onClick = { choosePersona = false }) { Text("关闭") } }
    )
}
