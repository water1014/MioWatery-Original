package com.water1014.miowatery.ui

import android.graphics.Paint as NativePaint
import android.graphics.Typeface
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sin

private val SplashInk = Color(0xFF101010)
private val SplashGhost = Color(0xFFE8E8E8)

@Composable
fun MioSplashScreen(onFinished: () -> Unit) {
    var elapsed by remember { mutableLongStateOf(0L) }
    var visible by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (elapsed >= 6800L) 900 else 350),
        label = "mio-splash-fade"
    )
    fun finishSplash() {
        if (finished) return
        finished = true
        visible = false
        scope.launch {
            delay(if (elapsed >= 6800L) 900 else 350)
            onFinished()
        }
    }
    LaunchedEffect(Unit) {
        val start = SystemClock.uptimeMillis()
        while (visible && !finished) {
            elapsed = SystemClock.uptimeMillis() - start
            if (elapsed >= 6800L) {
                finishSplash()
                break
            }
            delay(16)
        }
    }

    if (alpha > 0.001f) {
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .background(Color.White)
                .clickable { finishSplash() }
        ) {
            val u = min(maxWidth.value / 100f, maxHeight.value / 80f).coerceAtLeast(2.8f)
            val mioSize = (u * 14.8f).sp
            val cnSize = (u * 3.3f).sp
            val waterySize = (u * 4f).sp
            val poemSize = (u * 3.5f).sp
            val ghostAt = elapsed >= 250L
            val wipe = ((elapsed - 650L).toFloat() / 1250f).coerceIn(0f, 1f)
            val fill = ((elapsed - 650L).toFloat() / 3850f).coerceIn(0f, 1f)
            val poemVisible = elapsed >= 4000L

            Row(
                modifier = Modifier
                    .offset(x = maxWidth * 0.10f, y = maxHeight * 0.45f)
                    .height(mioSize.value.dp * 1.05f)
                    .wrapContentWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Box {
                    Text("Mio", fontSize = mioSize, fontWeight = FontWeight.Light, color = SplashGhost.copy(alpha = if (ghostAt) 1f else 0f), fontFamily = FontFamily.SansSerif)
                    Text(
                        "Mio",
                        fontSize = mioSize,
                        fontWeight = FontWeight.Light,
                        color = SplashInk,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.drawWithContent {
                            val reveal = fill
                            val path = Path().apply {
                                moveTo(0f, size.height)
                                lineTo(0f, size.height * (1f - reveal))
                                var x = 0f
                                while (x <= size.width) {
                                    val wave = sin(x * 0.055f + elapsed * 0.0028f) * 1.7f
                                    lineTo(x, size.height * (1f - reveal) + wave)
                                    x += 4f
                                }
                                lineTo(size.width, size.height)
                                close()
                            }
                            clipPath(path) { this@drawWithContent.drawContent() }
                        }
                    )
                }
                Spacer(Modifier.width((u * 2.6f).dp))
                Column(
                    modifier = Modifier.height(mioSize.value.dp * 0.92f).padding(top = (u * 1.3f).dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "文字是贫瘠的　我想你能读懂我的泪",
                        fontSize = cnSize,
                        fontWeight = FontWeight.Light,
                        color = SplashInk,
                        maxLines = 1,
                        modifier = Modifier.splashWipe(wipe)
                    )
                    Text(
                        "watery",
                        fontSize = waterySize,
                        fontWeight = FontWeight.Normal,
                        color = SplashInk,
                        modifier = Modifier.splashWipe(wipe)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .offset(x = maxWidth * 0.05f, y = maxHeight * 0.65f)
                    .fillMaxWidth(0.92f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SplashPoemLine("Most people are just raindrops", poemSize, poemVisible, elapsed, 0)
                SplashPoemLine("slowly sliding across the glass window of my life", poemSize, poemVisible, elapsed, 1)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -(maxWidth * 0.05f), y = maxHeight * 0.78f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SplashPoemLine("But if it were you, I would rather stay", poemSize, poemVisible, elapsed, 2)
                SplashPoemLine("in this rainy season for the rest of my life", poemSize, poemVisible, elapsed, 3)
            }

            Canvas(Modifier.fillMaxSize()) {
                val count = when {
                    elapsed < 650L -> 12
                    fill < 1f -> 58
                    else -> 16
                }
                val t = elapsed.toFloat()
                val native = drawContext.canvas.nativeCanvas
                val paint = NativePaint().apply {
                    color = android.graphics.Color.rgb(16, 16, 16)
                    strokeCap = NativePaint.Cap.ROUND
                    strokeWidth = 1.1f
                    setAlpha(95)
                }
                for (i in 0 until count) {
                    val x = ((i * 97.31f + (i % 5) * 23f) % size.width)
                    val speed = 0.16f + (i % 7) * 0.035f
                    val y = ((i * 131.7f + t * speed) % (size.height + 30f)) - 15f
                    val len = 7f + (i % 6) * 1.8f
                    native.drawLine(x, y, x - 1.1f, y + len, paint)
                }
            }
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 1f - alpha)))
        }
    }
}

private fun Modifier.splashWipe(progress: Float): Modifier = drawWithContent {
    val right = size.width * progress
    drawContext.canvas.save()
    drawContext.canvas.clipRect(0f, 0f, right, size.height)
    drawContent()
    drawContext.canvas.restore()
}

@Composable
private fun SplashPoemLine(text: String, size: TextUnit, visible: Boolean, elapsed: Long, index: Int) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(650, delayMillis = index * 50), label = "poem-alpha-$index")
    val lift = if (visible) 0.dp else 8.dp
    Text(
        text,
        fontSize = size,
        fontWeight = FontWeight.Normal,
        color = SplashInk.copy(alpha = alpha),
        modifier = Modifier.offset(y = lift)
    )
}
