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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var searchQuery by remember { mutableStateOf("") }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            avatar = uri.toString()
        }
    }

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

    val pageBackground = MaterialTheme.colorScheme.background
    val primaryText = MaterialTheme.colorScheme.onBackground
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val controlSurface = MaterialTheme.colorScheme.surfaceVariant
    val visiblePersonas = personas.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, true) ||
            it.description.orEmpty().contains(searchQuery, true)
    }

    Box(modifier.fillMaxSize().background(pageBackground)) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth().height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("联系人", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = primaryText)
                Box(
                    Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                        .size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = primaryText, fontSize = 30.sp, textAlign = TextAlign.Center)
                }
            }
            if (visiblePersonas.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (searchQuery.isBlank()) "还没有联系人" else "没有找到联系人", color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(7.dp))
                        Text(if (searchQuery.isBlank()) "点击下方 ＋ 添加联系人" else "试试其他名字或简介", color = secondaryText, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 76.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(visiblePersonas, key = { it.personaId }) { p ->
                        Row(
                            Modifier.fillMaxWidth().height(60.dp)
                                .clickable { onOpenChat(p.personaId) }
                                .padding(start = 18.dp, end = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(controlSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!p.avatar.isNullOrBlank()) {
                                    AsyncImage(
                                        model = p.avatar,
                                        contentDescription = p.name,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Text(p.name.take(1), fontSize = 18.sp, color = primaryText, fontWeight = FontWeight.Medium)
                                }
                            }
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(p.name, fontSize = 17.sp, color = primaryText, maxLines = 1)
                                if (!p.description.isNullOrBlank()) {
                                    Text(p.description.orEmpty(), fontSize = 11.sp, color = secondaryText, maxLines = 1)
                                }
                            }
                            Text(
                                "›",
                                Modifier.clickable { editPersona(p) }.padding(start = 12.dp, end = 2.dp),
                                fontSize = 23.sp,
                                color = secondaryText
                            )
                        }
                        Box(
                            Modifier.fillMaxWidth().padding(start = 68.dp, end = 13.dp).height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(start = 28.dp, end = 24.dp, bottom = 12.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                    .background(controlSurface).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⌕", color = secondaryText, fontSize = 23.sp)
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = primaryText, fontSize = 16.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(primaryText),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) Text("搜索", color = secondaryText, fontSize = 16.sp)
                            inner()
                        }
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Text("×", Modifier.clickable { searchQuery = "" }.padding(start = 8.dp), color = secondaryText, fontSize = 20.sp)
                }
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(controlSurface).clickable { newPersona() },
                contentAlignment = Alignment.Center
            ) {
                Text("＋", color = primaryText, fontSize = 25.sp)
            }
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
