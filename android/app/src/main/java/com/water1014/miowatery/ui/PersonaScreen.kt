package com.water1014.miowatery.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil3.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.water1014.miowatery.core.database.MioDatabase
import com.water1014.miowatery.core.database.PersonaEntity
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

private const val PERSONA_PROFILE_ID = "default"

@Composable
fun PersonaScreen(onBack: () -> Unit, onOpenChat: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember { MioDatabase.getInstance(context).mioDao() }
    val scope = rememberCoroutineScope()
    val personas by dao.observePersonas(PERSONA_PROFILE_ID).collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<PersonaEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var greet by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("") }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> avatar = uri?.toString().orEmpty() }

    LaunchedEffect(personas.isEmpty()) {
        if (personas.isEmpty()) {
            val now = System.currentTimeMillis()
            dao.upsertPersonas(listOf(PersonaEntity(
                profileId = PERSONA_PROFILE_ID, personaId = "default", name = "默认联系人",
                avatar = null, description = "你的聊天伙伴",
                rawLegacyJson = JSONObject().put("prompt", "你是一个温柔、简洁的聊天伙伴，用自然的口语回复。").put("greet", "").toString(),
                updatedAt = now
            )))
        }
    }
    fun newPersona() { editing = null; name = ""; bio = ""; prompt = ""; greet = ""; avatar = ""; showEditor = true }
    fun editPersona(p: PersonaEntity) {
        editing = p; name = p.name; bio = p.description.orEmpty(); avatar = p.avatar.orEmpty()
        val raw = runCatching { JSONObject(p.rawLegacyJson) }.getOrNull()
        prompt = raw?.optString("prompt").orEmpty(); greet = raw?.optString("greet").orEmpty()
        showEditor = true
    }

    Column(modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", Modifier.clip(CircleShape).clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 30.sp, color = Color(0xFF111111))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("联系人", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111111))
                Text("角色资料与聊天", fontSize = 10.sp, color = Color(0xFF9299A1))
            }
            Text("＋", Modifier.clip(CircleShape).clickable { newPersona() }.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 25.sp, color = Color(0xFF111111))
        }
        Text("我的联系人", Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp), fontSize = 12.sp, color = Color(0xFF9299A1))
        if (personas.isEmpty()) Text("正在准备联系人…", Modifier.padding(24.dp), color = Color(0xFF9299A1), fontSize = 13.sp)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            items(personas, key = { it.personaId }) { p ->
                Row(Modifier.fillMaxWidth().clickable { onOpenChat(p.personaId) }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F1F2)), contentAlignment = Alignment.Center) {
                        if (!p.avatar.isNullOrBlank()) AsyncImage(model = p.avatar, contentDescription = p.name, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
                        else Text(p.name.take(1), fontSize = 17.sp, color = Color(0xFF4D5660))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111111))
                        Text(p.description?.ifBlank { "还没有填写简介" } ?: "还没有填写简介", fontSize = 11.sp, color = Color(0xFF9299A1), maxLines = 1)
                    }
                    Text("编辑", Modifier.clip(RoundedCornerShape(12.dp)).clickable { editPersona(p) }.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 11.sp, color = Color(0xFF69727C))
                    Text("›", Modifier.padding(start = 5.dp), fontSize = 24.sp, color = Color(0xFFB0B6BC))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F7))) {
            Text("点选联系人即可进入独立聊天。每个角色的聊天记录会分别保存在本机。", Modifier.padding(16.dp), fontSize = 11.sp, lineHeight = 17.sp, color = Color(0xFF737B84))
        }
    }
    if (showEditor) AlertDialog(
        onDismissRequest = { showEditor = false },
        title = { Text(if (editing == null) "新增联系人" else "编辑联系人") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().clickable { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF0F1F2)), contentAlignment = Alignment.Center) {
                        if (avatar.isNotBlank()) AsyncImage(model = avatar, contentDescription = "人物头像", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)))
                        else Text(name.take(1).ifBlank { "＋" }, color = Color(0xFF59616B), fontSize = 20.sp)
                    }
                    Column {
                        Text("更换人物头像", color = Color(0xFF111111), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("从相册选择一张图片", color = Color(0xFF9299A1), fontSize = 10.sp)
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("名字") }, singleLine = true)
                OutlinedTextField(bio, { bio = it }, label = { Text("简介") }, singleLine = true)
                OutlinedTextField(prompt, { prompt = it }, label = { Text("角色人设") }, minLines = 4, maxLines = 7)
                OutlinedTextField(greet, { greet = it }, label = { Text("开场白（可选）") }, minLines = 2, maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val old = editing; val id = old?.personaId ?: UUID.randomUUID().toString(); val now = System.currentTimeMillis()
                val raw = runCatching { JSONObject(old?.rawLegacyJson ?: "{}") }.getOrElse { JSONObject() }
                    .put("prompt", prompt)
                    .put("greet", greet)
                    .toString()
                scope.launch {
                    dao.upsertPersonas(listOf(PersonaEntity(PERSONA_PROFILE_ID, id, name.trim().ifBlank { "未命名联系人" }, avatar.ifBlank { null }, bio.trim(), raw, now)))
                    showEditor = false
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
    )
}
