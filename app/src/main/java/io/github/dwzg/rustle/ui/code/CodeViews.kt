package io.github.dwzg.rustle.ui.code

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dwzg.rustle.ui.theme.CodeTextStyle

/** Marks a blank in fill-in-the-blank code (same marker as the course files). */
const val BLANK = "___"

private val PanelShape = RoundedCornerShape(16.dp)

/** Splits highlighted code into per-line annotated strings (keeps multi-line tokens right). */
private fun highlightedLines(code: String): List<AnnotatedString> {
    val full = RustHighlighter.highlight(code)
    val out = mutableListOf<AnnotatedString>()
    var start = 0
    for (line in code.split('\n')) {
        out += full.subSequence(start, start + line.length)
        start += line.length + 1
    }
    return out
}

@Composable
fun CodePanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(CodeColors.Background)
            .border(1.dp, Color.White.copy(alpha = 0.06f), PanelShape)
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
    ) { content() }
}

/**
 * Read-only highlighted code. When [onLineClick] is set, lines become tappable
 * (used by "spot the bug"), and [lineTint] can colour individual lines.
 */
@Composable
fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    lineNumbers: Boolean = true,
    onLineClick: ((Int) -> Unit)? = null,
    lineTint: (Int) -> Color = { Color.Transparent },
) {
    val lines = remember(code) { highlightedLines(code) }
    CodePanel(modifier) {
        Column(Modifier.width(IntrinsicSize.Max)) {
            lines.forEachIndexed { idx, line ->
                val number = idx + 1
                val tint by animateColorAsState(lineTint(number), label = "lineTint")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(tint)
                        .let { if (onLineClick != null) it.clickable { onLineClick(number) } else it }
                        .padding(vertical = if (onLineClick != null) 3.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (lineNumbers) {
                        Text(
                            "$number",
                            style = CodeTextStyle,
                            color = CodeColors.Gutter,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(34.dp).padding(end = 10.dp),
                        )
                    } else {
                        Box(Modifier.width(14.dp))
                    }
                    Text(line, style = CodeTextStyle, softWrap = false, modifier = Modifier.padding(end = 16.dp))
                }
            }
        }
    }
}

/**
 * Code with tappable blank slots. [filled] holds the token in each slot (or null);
 * [active] is the slot the next tapped token goes into.
 */
@Composable
fun FillCodeBlock(
    code: String,
    filled: List<String?>,
    active: Int?,
    onSlotClick: (Int) -> Unit,
    slotColor: (Int) -> Color? = { null },
) {
    var slotIndex = 0
    CodePanel {
        Column(Modifier.width(IntrinsicSize.Max)) {
            code.split('\n').forEachIndexed { idx, line ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        "${idx + 1}",
                        style = CodeTextStyle,
                        color = CodeColors.Gutter,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(34.dp).padding(end = 10.dp),
                    )
                    val parts = line.split(BLANK)
                    parts.forEachIndexed { p, part ->
                        if (part.isNotEmpty()) {
                            Text(RustHighlighter.highlight(part), style = CodeTextStyle, softWrap = false)
                        }
                        if (p < parts.lastIndex) {
                            val slot = slotIndex++
                            Slot(
                                token = filled.getOrNull(slot),
                                active = slot == active,
                                tint = slotColor(slot),
                                onClick = { onSlotClick(slot) },
                            )
                        }
                    }
                    Box(Modifier.width(16.dp))
                }
            }
        }
    }
}

@Composable
private fun Slot(token: String?, active: Boolean, tint: Color?, onClick: () -> Unit) {
    val border by animateColorAsState(
        tint ?: if (active) CodeColors.Keyword else CodeColors.Gutter,
        label = "slotBorder",
    )
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .widthIn(min = 44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (token == null) CodeColors.Blank else border.copy(alpha = 0.18f))
            .border(1.5.dp, border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            token ?: " ",
            style = CodeTextStyle,
            color = if (token == null) CodeColors.Gutter else CodeColors.Plain,
            fontWeight = FontWeight.Bold,
            softWrap = false,
        )
    }
}
