package io.github.dwzg.rustle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Piece(
    val x: Float, val vx: Float, val vy: Float, val spin: Float,
    val color: Color, val w: Float, val h: Float, val delay: Float,
)

private val ConfettiColors = listOf(
    Color(0xFFF74C00), Color(0xFFFFC23D), Color(0xFF34C77B),
    Color(0xFF6CD4FF), Color(0xFFC792EA), Color(0xFFFF5E7E),
)

/** A one-shot confetti burst falling from the top of its bounds. */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 90, seed: Int = 0) {
    val pieces = remember(seed) {
        val r = Random(seed)
        List(count) {
            Piece(
                x = r.nextFloat(),
                vx = (r.nextFloat() - 0.5f) * 0.25f,
                vy = 0.25f + r.nextFloat() * 0.35f,
                spin = (r.nextFloat() - 0.5f) * 720f,
                color = ConfettiColors[r.nextInt(ConfettiColors.size)],
                w = 6f + r.nextFloat() * 8f,
                h = 4f + r.nextFloat() * 6f,
                delay = r.nextFloat() * 0.6f,
            )
        }
    }
    var time by remember(seed) { mutableFloatStateOf(0f) }
    LaunchedEffect(seed) {
        val start = withFrameNanos { it }
        while (time < 5f) {
            time = (withFrameNanos { it } - start) / 1e9f
        }
    }
    Canvas(modifier) {
        for (p in pieces) {
            val t = time - p.delay
            if (t < 0f) continue
            val y = -0.05f + p.vy * t + 0.12f * t * t
            if (y > 1.1f) continue
            val x = p.x + p.vx * t + 0.03f * sin(t * 3f + p.x * 10f)
            val center = Offset(x * size.width, y * size.height)
            rotate(p.spin * t, pivot = center) {
                val squash = 0.4f + 0.6f * kotlin.math.abs(cos(t * 6f + p.x * 7f))
                drawRect(
                    p.color,
                    topLeft = center - Offset(p.w / 2, p.h * squash / 2),
                    size = Size(p.w, p.h * squash),
                )
            }
        }
    }
}
