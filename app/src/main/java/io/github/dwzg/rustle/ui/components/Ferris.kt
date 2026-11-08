package io.github.dwzg.rustle.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class FerrisMood { Happy, Sad, Party, Thinking }

private val Shell = Color(0xFFF74C00)
private val ShellDark = Color(0xFFB83A00)
private val ShellLight = Color(0xFFFF8A4C)
private val Ink = Color(0xFF1A0E08)

/** Ferris the crab, drawn with vector shapes so it can wiggle. */
@Composable
fun Ferris(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    mood: FerrisMood = FerrisMood.Happy,
    animate: Boolean = true,
) {
    val t = rememberInfiniteTransition(label = "ferris")
    val bob by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (mood == FerrisMood.Party) 420 else 1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val blink by t.animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes {
            durationMillis = 3600
            1f at 3300
            0.1f at 3420
            1f at 3540
        }),
        label = "blink",
    )
    val b = if (animate) bob else 0.5f
    val eye = if (animate) blink else 1f
    Canvas(modifier.size(size, size * 0.85f)) {
        val s = this.size.width / 100f
        scale(s, s, pivot = Offset.Zero) {
            val lift = when (mood) {
                FerrisMood.Party -> -6f * b
                FerrisMood.Sad -> 1.5f * b
                else -> -2f * b
            }
            translate(top = lift + 4f) { drawFerris(mood, b, eye) }
        }
    }
}

private fun DrawScope.drawFerris(mood: FerrisMood, b: Float, eyeOpen: Float) {
    // legs
    val leg = Stroke(width = 4.5f, cap = StrokeCap.Round)
    for (side in listOf(-1f, 1f)) {
        for ((k, dx) in listOf(14f, 22f, 30f).withIndex()) {
            val x0 = 50f + side * dx
            val path = Path().apply {
                moveTo(x0, 60f)
                quadraticTo(x0 + side * (8f + k), 66f, x0 + side * (6f + k * 2), 74f + k * 0.5f)
            }
            drawPath(path, ShellDark, style = leg)
        }
    }

    // claws + arms
    val armUp = when (mood) {
        FerrisMood.Party -> 10f + 6f * b
        FerrisMood.Sad -> -8f
        FerrisMood.Thinking -> 2f
        FerrisMood.Happy -> 2f * b
    }
    for (side in listOf(-1f, 1f)) {
        val shoulder = Offset(50f + side * 30f, 50f)
        val hand = Offset(50f + side * 41f, 34f - armUp)
        drawLine(ShellDark, shoulder, hand, strokeWidth = 5f, cap = StrokeCap.Round)
        val angle = if (mood == FerrisMood.Party) side * 12f * (b - 0.5f) else 0f
        rotate(angle, pivot = hand) {
            val r = 9f
            // Opening of the pincer faces up and slightly inwards.
            val open = if (side < 0) 300f else 240f
            drawArc(
                Shell,
                startAngle = open + 25f,
                sweepAngle = 310f,
                useCenter = true,
                topLeft = Offset(hand.x - r, hand.y - r - 4f),
                size = Size(r * 2, r * 2),
            )
        }
    }

    // body with spiky shell
    val body = Path().apply {
        moveTo(16f, 56f)
        quadraticTo(13f, 42f, 24f, 38f)
        val spikes = 6
        val x0 = 24f
        val x1 = 76f
        val w = (x1 - x0) / spikes
        for (i in 0 until spikes) {
            lineTo(x0 + w * i + w / 2f, 29f + if (i == 0 || i == spikes - 1) 3f else 0f)
            lineTo(x0 + w * (i + 1), 38f)
        }
        quadraticTo(87f, 42f, 84f, 56f)
        quadraticTo(50f, 76f, 16f, 56f)
        close()
    }
    drawPath(body, Shell)
    // belly highlight
    drawOval(ShellLight.copy(alpha = 0.55f), topLeft = Offset(30f, 44f), size = Size(40f, 14f))
    drawPath(body, ShellDark, style = Stroke(width = 1.6f))

    // eyes on stalks
    val look = when (mood) {
        FerrisMood.Sad -> Offset(0f, 1.6f)
        FerrisMood.Thinking -> Offset(1.8f, -1.6f)
        else -> Offset(0f, 0.4f)
    }
    for (side in listOf(-1f, 1f)) {
        val base = Offset(50f + side * 8f, 36f)
        val top = Offset(50f + side * 10f, 24f)
        drawLine(ShellDark, base, top, strokeWidth = 3f, cap = StrokeCap.Round)
        scale(1f, eyeOpen, pivot = top) {
            drawCircle(Color.White, radius = 6.5f, center = top)
            drawCircle(Ink, radius = 3.4f, center = top + look)
            drawCircle(Color.White, radius = 1.1f, center = top + look + Offset(-1.2f, -1.2f))
        }
    }

    // cheeks
    drawCircle(Color(0xFFFF5E7E).copy(alpha = 0.45f), radius = 3.6f, center = Offset(34f, 50f))
    drawCircle(Color(0xFFFF5E7E).copy(alpha = 0.45f), radius = 3.6f, center = Offset(66f, 50f))

    // mouth
    val mouth = Stroke(width = 2.2f, cap = StrokeCap.Round)
    when (mood) {
        FerrisMood.Sad -> drawArc(Ink, 200f, 140f, false, Offset(44f, 50f), Size(12f, 8f), style = mouth)
        FerrisMood.Thinking -> drawLine(Ink, Offset(46f, 51f), Offset(54f, 50f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        FerrisMood.Party -> drawArc(Ink, 0f, 180f, true, Offset(43f, 44f), Size(14f, 10f))
        FerrisMood.Happy -> drawArc(Ink, 20f, 140f, false, Offset(44f, 43f), Size(12f, 9f), style = mouth)
    }
}
