package com.water1014.miowatery.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

private data class CoreadBook(val id:String,val title:String,val file:String,val chapter:Int,val progress:Int)
@Composable
fun CoreadScreen(onBack:()->Unit, modifier:Modifier=Modifier) {
    val ctx=LocalContext.current
    val prefs=remember(ctx){ctx.getSharedPreferences("mio_coreader",Context.MODE_PRIVATE)}
    var books by remember { mutableStateOf(readCoreadBooks(prefs)) }
    var selected by remember { mutableStateOf<String?>(null) }
    var active by remember { mutableStateOf<CoreadBook?>(null) }
    var chapters by remember { mutableStateOf<List<Pair<String,String>>>(emptyList()) }
    var chapter by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var goalDialog by remember { mutableStateOf(false) }
    var goalText by remember { mutableStateOf(prefs.getInt("goal",20).toString()) }
    var fontSize by remember { mutableIntStateOf(prefs.getInt("font",18)) }
    val todayKey="minutes_"+LocalDate.now().toString()
    var minutes by remember { mutableIntStateOf(prefs.getInt(todayKey,0)) }
    val goal=prefs.getInt("goal",20)
    val scroll=rememberScrollState()
    fun persist(list:List<CoreadBook>){
        books=list
        val a=JSONArray()
        list.forEach{b->a.put(JSONObject().put("id",b.id).put("title",b.title).put("file",b.file).put("chapter",b.chapter).put("progress",b.progress))}
        prefs.edit().putString("books",a.toString()).apply()
    }
    fun updateProgress(index:Int){
        val b=active?:return
        val percent=((index+1).toFloat()/chapters.size.coerceAtLeast(1)*100).toInt()
        val updated=b.copy(chapter=index,progress=percent)
        active=updated
        persist(books.map{if(it.id==b.id)updated else it})
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri? ->
        if(uri!=null){
            busy=true;importError=null
            runCatching{
                val raw=ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use{it.readText()} ?: throw IllegalStateException("无法读取文件")
                val text=raw.removePrefix("\uFEFF").replace("\u0000","").trim()
                if(text.isBlank()) throw IllegalStateException("文件中没有可读取的文字")
                val name=uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.takeIf{it.isNotBlank()}?:"未命名书籍"
                val id=System.currentTimeMillis().toString()
                val file="coread_"+id+".txt"
                File(ctx.filesDir,file).writeText(text,Charsets.UTF_8)
                persist(listOf(CoreadBook(id,name,file,0,0))+books)
            }.onFailure{importError=it.message?:"导入失败"}
            busy=false
        }
    }
    LaunchedEffect(selected){
        if(selected==null){active=null;chapters=emptyList()}
        else{
            busy=true
            val b=books.firstOrNull{it.id==selected}
            if(b==null){selected=null;busy=false}
            else{
                active=b
                val raw=runCatching{File(ctx.filesDir,b.file).readText(Charsets.UTF_8)}.getOrElse{importError="书籍文件读取失败";""}
                chapters=splitCoreadText(raw)
                chapter=b.chapter.coerceIn(0,(chapters.size-1).coerceAtLeast(0))
                busy=false
            }
        }
    }
    LaunchedEffect(selected){
        if(selected!=null) while(true){delay(60_000);minutes=prefs.getInt(todayKey,0)+1;prefs.edit().putInt(todayKey,minutes).apply()}
    }
    Column(modifier.fillMaxSize().background(Color(0xFFF7F8FA)).padding(horizontal=16.dp,vertical=12.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick={if(selected!=null)selected=null else onBack()}){Text("‹ 返回")}
            Text(if(selected==null)"共读" else active?.title?:"正在打开",Modifier.weight(1f),fontSize=19.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
            if(selected==null) TextButton(onClick={picker.launch(arrayOf("text/plain","text/markdown","text/html","application/xhtml+xml"))}){Text("导入")}
            else TextButton(onClick={fontSize=if(fontSize>=24)16 else fontSize+2;prefs.edit().putInt("font",fontSize).apply()}){Text("Aa")}
        }
        if(selected==null){
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)){
                    Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                        Text("阅读目标",fontWeight=FontWeight.SemiBold)
                        Text("今日已阅读 "+minutes+" 分钟",color=Color.Gray,fontSize=12.sp)
                        LinearProgressIndicator(progress={(minutes.toFloat()/goal.coerceAtLeast(1)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                        TextButton(onClick={goalText=goal.toString();goalDialog=true}){Text("每日目标："+goal+" 分钟 · 修改")}
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("我的书架",fontWeight=FontWeight.SemiBold);Text(books.size.toString()+" 本",color=Color.Gray,fontSize=12.sp)}
                if(books.isEmpty()) Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)){
                    Column(Modifier.fillMaxWidth().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
                        Text("还没有加入书籍",fontSize=17.sp,fontWeight=FontWeight.Medium)
                        Text("目前支持导入 TXT、Markdown 和 HTML 文本。",color=Color.Gray,fontSize=12.sp)
                        Button(onClick={picker.launch(arrayOf("text/plain","text/markdown","text/html","application/xhtml+xml"))}){Text("导入一本书")}
                    }
                }
                books.forEach{b->
                    Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().clickable{selected=b.id}){
                        Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            Box(Modifier.size(48.dp,64.dp).background(Color(0xFFEAF0FA),RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Text("书",fontSize=22.sp,color=Color(0xFF6E86AD))}
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                Text(b.title,fontWeight=FontWeight.Medium,maxLines=1)
                                LinearProgressIndicator(progress={b.progress/100f},modifier=Modifier.fillMaxWidth())
                                Text("阅读进度 "+b.progress+"%",color=Color.Gray,fontSize=10.sp)
                            }
                            TextButton(onClick={File(ctx.filesDir,b.file).delete();persist(books.filterNot{it.id==b.id})}){Text("删除")}
                        }
                    }
                }
                importError?.let{Text(it,color=Color(0xFFB04444),fontSize=12.sp)}
                if(busy)CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
        }else{
            val item=chapters.getOrNull(chapter)
            if(busy)Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
            else if(item==null)Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text(importError?:"没有可显示的内容",color=Color.Gray)}
            else{
                Text(item.first,Modifier.fillMaxWidth().padding(top=8.dp,bottom=10.dp),fontWeight=FontWeight.SemiBold,color=Color(0xFF39485F))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal=3.dp)){Text(item.second,fontSize=fontSize.sp,lineHeight=(fontSize*1.75).sp,color=Color(0xFF303744))}
                LinearProgressIndicator(progress={(active?.progress?:0)/100f},modifier=Modifier.fillMaxWidth().padding(top=8.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    TextButton(enabled=chapter>0,onClick={chapter--;updateProgress(chapter)}){Text("上一章")}
                    Text((chapter+1).toString()+" / "+chapters.size,color=Color.Gray,fontSize=12.sp)
                    TextButton(enabled=chapter<chapters.lastIndex,onClick={chapter++;updateProgress(chapter);scroll.scrollToTop()}){Text("下一章")}
                }
            }
        }
    }
    if(goalDialog)AlertDialog(onDismissRequest={goalDialog=false},title={Text("每日阅读目标")},text={OutlinedTextField(goalText,{goalText=it.filter(Char::isDigit).take(3)},label={Text("分钟（1～600）")},singleLine=true)},confirmButton={TextButton(onClick={goalText.toIntOrNull()?.let{prefs.edit().putInt("goal",it.coerceIn(1,600)).apply()};goalDialog=false}){Text("保存")}},dismissButton={TextButton(onClick={goalDialog=false}){Text("取消")}})
}

private fun readCoreadBooks(prefs:android.content.SharedPreferences):List<CoreadBook> = runCatching{
    val a=JSONArray(prefs.getString("books","[]")?:"[]")
    (0 until a.length()).mapNotNull{i->a.optJSONObject(i)?.let{o->CoreadBook(o.optString("id"),o.optString("title"),o.optString("file"),o.optInt("chapter"),o.optInt("progress"))}}
}.getOrDefault(emptyList())

private fun splitCoreadText(raw:String):List<Pair<String,String>>{
    val text=raw.replace("\r\n","\n").replace('\r','\n')
    val re=Regex("(?m)^\\s*((?:第[0-9零〇一二三四五六七八九十百千两]+[章节回卷部集篇幕]|Chapter\\s+[0-9IVXLC]+|序章|楔子|尾声|后记|番外)[^\\n]{0,40})\\s*$",RegexOption.IGNORE_CASE)
    val matches=re.findAll(text).toList()
    if(matches.size>=2){
        val out=mutableListOf<Pair<String,String>>()
        if(matches.first().range.first>0)out.add("前言" to text.substring(0,matches.first().range.first).trim())
        matches.forEachIndexed{i,m->val end=matches.getOrNull(i+1)?.range?.first?:text.length;out.add(m.groupValues[1].trim() to text.substring(m.range.first,end).trim())}
        return out.filter{it.second.isNotBlank()}
    }
    return text.chunked(7000).mapIndexed{i,v->("第 "+(i+1)+" 部分") to v}.filter{it.second.isNotBlank()}
}
