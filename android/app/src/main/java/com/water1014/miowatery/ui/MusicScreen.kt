package com.water1014.miowatery.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private const val MUSIC_PREFS = "mio_music_player"

@Composable
fun MusicScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(MUSIC_PREFS, android.content.Context.MODE_PRIVATE) }
    var trackUri by remember { mutableStateOf(prefs.getString("track_uri", null)) }
    var trackName by remember { mutableStateOf(prefs.getString("track_name", "尚未选择歌曲") ?: "尚未选择歌曲") }
    var coverUri by remember { mutableStateOf(prefs.getString("cover_uri", null)) }
    var lyric1 by remember { mutableStateOf(prefs.getString("lyric_1", "") ?: "") }
    var lyric2 by remember { mutableStateOf(prefs.getString("lyric_2", "") ?: "") }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val player = remember(context) { ExoPlayer.Builder(context).build() }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            trackUri = uri.toString()
            trackName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment ?: "本地音乐"
            prefs.edit().putString("track_uri", trackUri).putString("track_name", trackName).putBoolean("is_playing", false).apply()
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.pause()
            isPlaying = false
            position = 0L
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            coverUri = uri.toString()
            prefs.edit().putString("cover_uri", coverUri).apply()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                prefs.edit().putBoolean("is_playing", playing).apply()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = player.duration.coerceAtLeast(0L)
            }
        }
        player.addListener(listener)
        onDispose {
            prefs.edit().putBoolean("is_playing", false).apply()
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            if (duration > 0L) seekValue = position.toFloat() / duration.toFloat()
            delay(500)
        }
    }

    Column(modifier.fillMaxSize().background(Color(0xFFF7F7F8)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 28.sp, color = Color(0xFF171719))
            Text("音乐", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF171719))
            TextButton(onClick = onBack) { Text("返回") }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(218.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFECEDEF)), contentAlignment = Alignment.Center) {
                    if (!coverUri.isNullOrBlank()) AsyncImage(model = coverUri, contentDescription = "专辑封面", modifier = Modifier.fillMaxSize())
                    else Text("♫", fontSize = 66.sp, color = Color(0xFF8D9299))
                }
                Text(trackName, color = Color(0xFF171719), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(if (isPlaying) "正在播放" else "已暂停 / 未播放", color = Color(0xFF92949A), fontSize = 12.sp)
                Slider(
                    value = seekValue.coerceIn(0f, 1f),
                    onValueChange = { seekValue = it },
                    onValueChangeFinished = { if (duration > 0L) player.seekTo((seekValue * duration).toLong()) },
                    enabled = duration > 0L
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMusicTime(position), color = Color(0xFF92949A), fontSize = 11.sp)
                    Text(formatMusicTime(duration), color = Color(0xFF92949A), fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { player.seekTo(0L) }, enabled = trackUri != null) { Text("重播") }
                    Button(
                        onClick = {
                            val uri = trackUri?.let(Uri::parse) ?: return@Button
                            if (player.mediaItemCount == 0) {
                                player.setMediaItem(MediaItem.fromUri(uri))
                                player.prepare()
                            }
                            if (player.isPlaying) player.pause() else player.play()
                        },
                        enabled = trackUri != null
                    ) { Text(if (isPlaying) "暂停" else "播放") }
                    TextButton(onClick = { audioPicker.launch(arrayOf("audio/*")) }) { Text("选择歌曲") }
                }
                TextButton(onClick = { coverPicker.launch(arrayOf("image/*")) }) { Text("更换封面") }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("歌词", color = Color(0xFF171719), fontWeight = FontWeight.SemiBold)
                OutlinedTextField(lyric1, { lyric1 = it }, Modifier.fillMaxWidth(), label = { Text("第一行") }, minLines = 2)
                OutlinedTextField(lyric2, { lyric2 = it }, Modifier.fillMaxWidth(), label = { Text("第二行") }, minLines = 2)
                Button(onClick = {
                    prefs.edit().putString("lyric_1", lyric1).putString("lyric_2", lyric2).apply()
                }, modifier = Modifier.align(Alignment.End)) { Text("保存歌词") }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text("支持从本机选择音频文件。退出音乐页面后会停止播放，歌曲、封面和歌词会保留。", color = Color(0xFF92949A), fontSize = 11.sp)
    }
}

private fun formatMusicTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
