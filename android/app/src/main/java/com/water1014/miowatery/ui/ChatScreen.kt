package com.water1014.miowatery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ChatPreviewMessage(
    val id: Long,
    val text: String,
    val fromUser: Boolean
)

/**
 * Native chat surface, following the original Page's monochrome chat language.
 * Sending is intentionally local-only until API profiles and Room conversation
 * persistence are connected; it never pretends that a model has replied.
 */
@Composable
fun ChatScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val messages = remember { mutableStateListOf<ChatPreviewMessage>() }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(top = 1.dp),
                color = Color(0xFF0F1419),
                fontSize = 34.sp,
                textAlign = TextAlign.Center
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("聊天", color = Color(0xFF0F1419), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("选择联系人后开始对话", color = Color(0xFF8A929B), fontSize = 10.sp)
            }
            Text("···", color = Color(0xFF0F1419), fontSize = 22.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFEAECEE))
        )

        if (messages.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFF3F4F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = Color(0xFF0F1419), fontSize = 27.sp, fontWeight = FontWeight.Light)
                }
                Spacer(Modifier.height(18.dp))
                Text("还没有消息", color = Color(0xFF0F1419), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                Text(
                    "从联系人中选择一位角色，开始你们的故事。",
                    color = Color(0xFF7B848E),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "今天",
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF9AA1A9),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
                items(messages, key = { it.id }) { message ->
                    ChatPreviewBubble(message)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF3F4F5))
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text("＋", color = Color(0xFF59616B), fontSize = 23.sp, modifier = Modifier.padding(bottom = 1.dp))
                Spacer(Modifier.width(9.dp))
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 7.dp),
                    textStyle = TextStyle(color = Color(0xFF0F1419), fontSize = 14.sp, lineHeight = 20.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0F1419)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        val value = draft.trim()
                        if (value.isNotEmpty()) {
                            messages.add(ChatPreviewMessage(System.currentTimeMillis(), value, true))
                            draft = ""
                        }
                    }),
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Text("发送消息…", color = Color(0xFF9AA1A9), fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (draft.isBlank()) Color(0xFFD9DDE1) else Color(0xFF111111))
                        .clickable {
                            val value = draft.trim()
                            if (value.isNotEmpty()) {
                                messages.add(ChatPreviewMessage(System.currentTimeMillis(), value, true))
                                draft = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↑", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(
                "本地界面预览 · AI 请求与历史记录接入中",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                color = Color(0xFFA3AAB1),
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChatPreviewBubble(message: ChatPreviewMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.fromUser) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F1F2)),
                contentAlignment = Alignment.Center
            ) { Text("M", color = Color(0xFF555D66), fontSize = 13.sp) }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(1.dp)
                .background(Color.Transparent)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (message.fromUser) Color(0xFF111111) else Color(0xFFECEEF0))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                message.text,
                color = if (message.fromUser) Color.White else Color(0xFF0F1419),
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}
