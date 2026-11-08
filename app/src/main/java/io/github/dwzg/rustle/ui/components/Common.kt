package io.github.dwzg.rustle.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.dwzg.rustle.ui.code.CodeBlock
import io.github.dwzg.rustle.ui.theme.Brand
import io.github.dwzg.rustle.ui.theme.Mono

/** A chunky, tactile button that sinks into its shadow when pressed. */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Brand.Rust,
    shadow: Color = Brand.RustDeep,
    contentColor: Color = Color.White,
    height: Dp = 54.dp,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val depth = 4.dp
    val sink by animateDpAsState(if (pressed && enabled) depth else 0.dp, label = "sink")
    val face by animateColorAsState(if (enabled) color else MaterialTheme.colorScheme.surfaceContainerHighest, label = "face")
    val base by animateColorAsState(if (enabled) shadow else MaterialTheme.colorScheme.outlineVariant, label = "base")
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(height + depth)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = depth)
                .clip(shape)
                .background(base),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = sink)
                .clip(shape)
                .background(face),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 16.sp,
                color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A selectable answer tile with a sunken border look. */
@Composable
fun OptionTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    val accent = tint ?: if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val border by animateColorAsState(accent, label = "tileBorder")
    val bg by animateColorAsState(
        if (selected || tint != null) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer,
        label = "tileBg",
    )
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .border(2.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) { content() }
}

@Composable
fun XpBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Brand.Gold, height: Dp = 12.dp) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier.height(height).clip(RoundedCornerShape(50)),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun StatPill(icon: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

/** Inline markdown: `code` and **bold**. */
@Composable
fun inlineMarkdown(text: String): AnnotatedString {
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val codeFg = MaterialTheme.colorScheme.primary
    return remember(text, codeBg, codeFg) {
        buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                when {
                    text.startsWith("**", i) && text.indexOf("**", i + 2) > 0 -> {
                        val end = text.indexOf("**", i + 2)
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(text.substring(i + 2, end)) }
                        i = end + 2
                    }
                    text[i] == '`' && text.indexOf('`', i + 1) > 0 -> {
                        val end = text.indexOf('`', i + 1)
                        withStyle(SpanStyle(fontFamily = Mono, background = codeBg, color = codeFg, fontSize = 0.92.em)) {
                            append(" " + text.substring(i + 1, end) + " ")
                        }
                        i = end + 1
                    }
                    else -> {
                        append(text[i]); i++
                    }
                }
            }
        }
    }
}

/** Renders the light markdown used by explain cards: paragraphs, bullets, code fences. */
@Composable
fun MarkdownBody(body: String, modifier: Modifier = Modifier) {
    val blocks = remember(body) { splitMarkdown(body) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (b in blocks) {
            when (b) {
                is MdBlock.Code -> CodeBlock(b.code, lineNumbers = false)
                is MdBlock.Para -> Text(
                    inlineMarkdown(b.text),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                )
                is MdBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (item in b.items) {
                        Row {
                            Text("🦀", fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp, end = 8.dp))
                            Text(inlineMarkdown(item), style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

sealed interface MdBlock {
    data class Para(val text: String) : MdBlock
    data class Bullets(val items: List<String>) : MdBlock
    data class Code(val code: String) : MdBlock
}

fun splitMarkdown(body: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val para = mutableListOf<String>()
    val bullets = mutableListOf<String>()
    val code = mutableListOf<String>()
    var inFence = false
    fun flush() {
        if (para.isNotEmpty()) out += MdBlock.Para(para.joinToString(" ")).also { para.clear() }
        if (bullets.isNotEmpty()) out += MdBlock.Bullets(bullets.toList()).also { bullets.clear() }
    }
    for (line in body.lines()) {
        if (line.startsWith("```")) {
            if (inFence) {
                out += MdBlock.Code(code.joinToString("\n")); code.clear()
            } else flush()
            inFence = !inFence
            continue
        }
        when {
            inFence -> code += line
            line.isBlank() -> flush()
            line.trimStart().startsWith("- ") -> {
                if (para.isNotEmpty()) flush()
                bullets += line.trimStart().drop(2)
            }
            else -> {
                if (bullets.isNotEmpty()) flush()
                para += line.trim()
            }
        }
    }
    flush()
    return out
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier,
        textAlign = TextAlign.Start,
    )
}
