package com.water1014.miowatery.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun CoupleScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("mio_couple_space", Context.MODE_PRIVATE) }
    var since by remember { mutableStateOf(prefs.getString("since", "") ?: "") }
    var intro by remember { mutableStateOf(prefs.getString("intro", "") ?: "") }
    var wishInput by remember { mutableStateOf("") }
    var wishes by remember { mutableStateOf(loadWishes(prefs)) }
    var dateDialog by remember { mutableStateOf(false) }
    var dateDraft by remember { mutableStateOf(since) }
    var introDialog by remember { mutableStateOf(false) }
    var introDraft by remember { mutableStateOf(intro) }
    val days = remember(since) { runCatching { ChronoUnit.DAYS.between(LocalDate.parse(since), LocalDate.now()).toInt() + 1 }.getOrNull() }

    Column(modifier.fillMaxSize().background(Color(0xFFF7F8FA)).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("情侣空间", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF20232A))
            TextButton(onClick = onBack) { Text("返回") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("在一起的日子", color = Color.Gray)
                Text(if (days == null) "我们的纪念日" else days.coerceAtLeast(1).toString() + " 天", fontSize = 36.sp, fontWeight = FontWeight.Light, color = Color(0xFF26364F))
                Text(if (since.isBlank()) "设置一个日期，开始记录共同时间" else "从 $since 开始", color = Color.Gray, fontSize = 12.sp)
                OutlinedButton(onClick = { dateDraft = since; dateDialog = true }) { Text(if (since.isBlank()) "设置纪念日" else "修改纪念日") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("我们的空间", fontWeight = FontWeight.SemiBold, color = Color(0xFF20232A))
                    TextButton(onClick = { introDraft = intro; introDialog = true }) { Text("编辑") }
                }
                Text(intro.ifBlank { "写下你们的约定、暗号和共同回忆。" }, color = Color(0xFF707986), fontSize = 14.sp, lineHeight = 22.sp)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("共同愿望", fontWeight = FontWeight.SemiBold, color = Color(0xFF20232A))
                    Text("${wishes.count { it.second }}/${wishes.size} 已完成", color = Color.Gray, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(wishInput, { wishInput = it }, Modifier.weight(1f), placeholder = { Text("想一起完成的事") }, singleLine = true)
                    Button(onClick = {
                        val value = wishInput.trim()
                        if (value.isNotEmpty()) { wishes = wishes + (value to false); saveWishes(prefs, wishes); wishInput = "" }
                    }, enabled = wishInput.isNotBlank()) { Text("添加") }
                }
                if (wishes.isEmpty()) Text("把想一起完成的小事放在这里吧。", color = Color.Gray, fontSize = 12.sp)
                wishes.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = item.second, onCheckedChange = { checked ->
                            wishes = wishes.toMutableList().also { it[index] = item.first to checked }
                            saveWishes(prefs, wishes)
                        })
                        Text(item.first, Modifier.weight(1f), color = Color(0xFF343B46), fontSize = 13.sp)
                        TextButton(onClick = { wishes = wishes.filterIndexed { i, _ -> i != index }; saveWishes(prefs, wishes) }) { Text("删除") }
                    }
                }
            }
        }
        Text("纪念日、介绍和愿望清单会保存在本机。原版中的树洞、每日回忆和互动卡片还需要继续逐项迁移。", color = Color.Gray, fontSize = 11.sp, lineHeight = 17.sp)
    }
    if (dateDialog) AlertDialog(
        onDismissRequest = { dateDialog = false }, title = { Text("设置纪念日") },
        text = { OutlinedTextField(dateDraft, { dateDraft = it }, label = { Text("日期 YYYY-MM-DD") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            if (runCatching { LocalDate.parse(dateDraft.trim()); true }.getOrDefault(false)) {
                since = dateDraft.trim(); prefs.edit().putString("since", since).apply(); dateDialog = false
            }
        }) { Text("保存") } },
        dismissButton = { TextButton(onClick = { dateDialog = false }) { Text("取消") } }
    )
    if (introDialog) AlertDialog(
        onDismissRequest = { introDialog = false }, title = { Text("编辑我们的空间") },
        text = { OutlinedTextField(introDraft, { introDraft = it }, minLines = 3) },
        confirmButton = { TextButton(onClick = { intro = introDraft; prefs.edit().putString("intro", intro).apply(); introDialog = false }) { Text("保存") } },
        dismissButton = { TextButton(onClick = { introDialog = false }) { Text("取消") } }
    )
}

private fun loadWishes(prefs: android.content.SharedPreferences): List<Pair<String, Boolean>> = runCatching {
    val array = JSONArray(prefs.getString("wishes", "[]") ?: "[]")
    (0 until array.length()).mapNotNull { i ->
        val item = array.optJSONObject(i) ?: return@mapNotNull null
        item.optString("text").takeIf { it.isNotBlank() }?.let { it to item.optBoolean("done") }
    }
}.getOrDefault(emptyList())

private fun saveWishes(prefs: android.content.SharedPreferences, wishes: List<Pair<String, Boolean>>) {
    val array = JSONArray()
    wishes.forEach { (text, done) -> array.put(JSONObject().put("text", text).put("done", done)) }
    prefs.edit().putString("wishes", array.toString()).apply()
}
