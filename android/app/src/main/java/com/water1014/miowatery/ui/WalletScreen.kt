package com.water1014.miowatery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.water1014.miowatery.core.database.MioDatabase
import com.water1014.miowatery.core.database.WalletStateEntity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.UUID

private const val WALLET_PROFILE = "default"
private val walletCategories = listOf("银行卡", "收入", "房产", "车辆", "账单")
private fun walletJson(json: String?, owner: String) = runCatching {
    JSONObject(json ?: "{}").also { root ->
        listOf("cards", "incomes", "houses", "cars", "records").forEach { if (root.optJSONArray(it) == null) root.put(it, JSONArray()) }
        if (!root.has("balance")) root.put("balance", if (owner == "me") 1000 else 20000)
    }
}.getOrElse { JSONObject().put("balance", if (owner == "me") 1000 else 20000).put("cards", JSONArray()).put("incomes", JSONArray()).put("houses", JSONArray()).put("cars", JSONArray()).put("records", JSONArray()) }
private fun walletYuan(n: Double) = "¥ " + DecimalFormat("#,##0.00").format(n)

@Composable
fun WalletScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember(context) { MioDatabase.getInstance(context).mioDao() }
    val scope = rememberCoroutineScope()
    val wallets by dao.observeWalletStates(WALLET_PROFILE).collectAsState(initial = emptyList())
    val personas by dao.observePersonas(WALLET_PROFILE).collectAsState(initial = emptyList())
    var owner by remember { mutableStateOf("me") }
    var tab by remember { mutableIntStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dialog by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf("topup") }
    var assetDialog by remember { mutableStateOf(false) }
    var editIndex by remember { mutableIntStateOf(-1) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    val row = wallets.firstOrNull { it.ownerId == owner }
    val data = remember(row?.dataJson, owner) { walletJson(row?.dataJson, owner) }
    val balance = data.optDouble("balance")
    LaunchedEffect(row, owner) {
        if (row == null) dao.upsertWalletState(WalletStateEntity(WALLET_PROFILE, owner, walletJson(null, owner).toString(), System.currentTimeMillis()))
    }
    fun save(next: JSONObject = data) {
        scope.launch { dao.upsertWalletState(WalletStateEntity(WALLET_PROFILE, owner, next.toString(), System.currentTimeMillis())) }
    }
    fun editAsset(index: Int = -1) {
        editIndex = index
        val key = listOf("cards", "incomes", "houses", "cars")[tab.coerceIn(0, 3)]
        val old = data.optJSONArray(key)?.optJSONObject(index)
        title = old?.optString(if (tab == 0) "bank" else "name").orEmpty()
        detail = old?.optString(when(tab) { 0 -> "last4"; 1 -> "period"; 2 -> "area"; else -> "plate" }).orEmpty()
        value = old?.optString(when(tab) { 0 -> "balance"; 1 -> "amount"; else -> "value" }).orEmpty()
        extra = old?.optString("note").orEmpty()
        assetDialog = true
    }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("钱包", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold); Text("余额、资产与收支记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = onBack) { Text("返回") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(owner == "me", { owner = "me" }, label = { Text("我") })
            personas.forEach { p -> FilterChip(owner == p.personaId, { owner = p.personaId }, label = { Text(p.name, maxLines = 1) }) }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (owner == "me") "我的余额" else (personas.firstOrNull { it.personaId == owner }?.name ?: "角色") + "的余额")
                Text(walletYuan(balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                val total = balance + (data.optJSONArray("cards")?.sumWallet("balance") ?: 0.0) + (data.optJSONArray("houses")?.sumWallet("value") ?: 0.0) + (data.optJSONArray("cars")?.sumWallet("value") ?: 0.0)
                Text("总资产约 " + walletYuan(total), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (owner == "me") {
                        Button(onClick = { action = "topup"; amount = ""; note = ""; dialog = true }) { Text("充值") }
                        OutlinedButton(onClick = { action = "out"; amount = ""; note = ""; dialog = true }) { Text("提现") }
                    } else OutlinedButton(onClick = { action = "edit"; amount = balance.toString(); note = ""; dialog = true }) { Text("修改余额") }
                }
            }
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
            walletCategories.forEachIndexed { i, label -> Tab(tab == i, { tab = i }, text = { Text(label) }) }
        }
        if (tab < 4) {
            val key = listOf("cards", "incomes", "houses", "cars")[tab]
            val list = data.optJSONArray(key) ?: JSONArray()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(list.length().toString() + " 项", style = MaterialTheme.typography.labelMedium)
                Button(onClick = { editAsset() }) { Text("添加") }
            }
            if (list.length() == 0) Text("还没有" + walletCategories[tab] + "，点击添加开始记录。", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0 until list.length()).toList()) { i ->
                    val item = list.optJSONObject(i) ?: JSONObject()
                    Card(Modifier.fillMaxWidth().clickable { editAsset(i) }, shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.optString(if (tab == 0) "bank" else "name").ifBlank { walletCategories[tab] }, fontWeight = FontWeight.SemiBold)
                                Text(when(tab) { 0 -> "尾号 " + item.optString("last4"); 1 -> item.optString("period", "每月"); 2 -> item.optString("area") + " ㎡"; else -> item.optString("plate") } + if (item.optString("note").isNotBlank()) " · " + item.optString("note") else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(walletYuan(item.optDouble(when(tab) { 0 -> "balance"; 1 -> "amount"; else -> "value" })), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            val records = data.optJSONArray("records") ?: JSONArray()
            if (records.length() == 0) Text("暂时没有账单记录。", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items((0 until records.length()).toList()) { i ->
                    val r = records.optJSONObject(i) ?: JSONObject()
                    val income = r.optString("type") in listOf("topup", "recv", "refund")
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(r.optString("note").ifBlank { r.optString("type") }); Text(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(r.optLong("t"))), style = MaterialTheme.typography.labelSmall) }
                            Text((if (income) "+" else "-") + walletYuan(r.optDouble("amount")), color = if (income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    if (dialog) AlertDialog(onDismissRequest = { dialog = false }, title = { Text(when(action) { "topup" -> "充值"; "out" -> "提现"; else -> "修改余额" }) },
        text = { Column { OutlinedTextField(amount, { amount = it }, label = { Text("金额（元）") }); OutlinedTextField(note, { note = it }, label = { Text("备注") }) } },
        confirmButton = { TextButton(onClick = {
            val n = amount.toDoubleOrNull() ?: return@TextButton
            if (n < 0 || (action != "edit" && n == 0.0) || (action == "out" && n > balance)) return@TextButton
            val next = JSONObject(data.toString())
            if (action == "edit") next.put("balance", n) else {
                next.put("balance", if (action == "out") balance - n else balance + n)
                val records = JSONArray(next.optJSONArray("records").toString())
                records.put(0, JSONObject().put("id", UUID.randomUUID().toString()).put("t", System.currentTimeMillis()).put("type", action).put("amount", n).put("note", note.ifBlank { if (action == "out") "提现" else "充值" }))
                next.put("records", records)
            }
            save(next); dialog = false
        }) { Text("确认") } }, dismissButton = { TextButton(onClick = { dialog = false }) { Text("取消") } })
    if (assetDialog) AlertDialog(onDismissRequest = { assetDialog = false }, title = { Text((if (editIndex < 0) "添加" else "编辑") + walletCategories[tab]) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text(if (tab == 0) "银行名称" else "名称") })
            OutlinedTextField(detail, { detail = it }, label = { Text(when(tab) { 0 -> "卡号尾号"; 1 -> "周期"; 2 -> "面积（㎡）"; else -> "车牌" }) })
            OutlinedTextField(value, { value = it }, label = { Text(when(tab) { 0 -> "余额 / 额度"; 1 -> "金额"; else -> "估值（元）" }) })
            OutlinedTextField(extra, { extra = it }, label = { Text("备注") })
        } },
        confirmButton = { TextButton(onClick = {
            if (title.isBlank()) return@TextButton
            val next = JSONObject(data.toString()); val key = listOf("cards", "incomes", "houses", "cars")[tab]
            val old = JSONArray(next.optJSONArray(key).toString()); val arr = JSONArray(); val previous = old.optJSONObject(editIndex)
            val item = JSONObject().put("id", previous?.optString("id") ?: UUID.randomUUID().toString()).put(if (tab == 0) "bank" else "name", title.trim()).put("note", extra.trim())
            when(tab) {
                0 -> item.put("type", "储蓄卡").put("last4", detail.filter { it.isDigit() }.takeLast(4)).put("balance", value.toDoubleOrNull() ?: 0.0)
                1 -> item.put("period", detail.ifBlank { "每月" }).put("amount", value.toDoubleOrNull() ?: 0.0)
                2 -> item.put("area", detail).put("value", value.toDoubleOrNull() ?: 0.0)
                else -> item.put("plate", detail).put("value", value.toDoubleOrNull() ?: 0.0)
            }
            for (i in 0 until old.length()) { if (i == editIndex) arr.put(item) else arr.put(old.opt(i)) }
            if (editIndex < 0) arr.put(item)
            next.put(key, arr); save(next); assetDialog = false
        }) { Text("保存") } },
        dismissButton = { Row {
            if (editIndex >= 0) TextButton(onClick = {
                val next = JSONObject(data.toString()); val key = listOf("cards", "incomes", "houses", "cars")[tab]
                val old = next.optJSONArray(key) ?: JSONArray(); val arr = JSONArray()
                for (i in 0 until old.length()) if (i != editIndex) arr.put(old.opt(i))
                next.put(key, arr); save(next); assetDialog = false
            }) { Text("删除") }
            TextButton(onClick = { assetDialog = false }) { Text("取消") }
        } })
}

private fun JSONArray.sumWallet(key: String): Double {
    var total = 0.0
    for (i in 0 until length()) total += optJSONObject(i)?.optDouble(key, 0.0) ?: 0.0
    return total
}
