package io.github.dwzg.rustle.ui.playground

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dwzg.rustle.ui.code.CodeColors
import io.github.dwzg.rustle.ui.code.RustHighlighter
import io.github.dwzg.rustle.ui.theme.Brand
import io.github.dwzg.rustle.ui.theme.CodeTextStyle
import io.github.dwzg.rustle.ui.theme.Mono
import io.github.dwzg.rustle.ui.theme.RustleTheme
import kotlinx.coroutines.launch

private object HighlightTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) =
        TransformedText(RustHighlighter.highlight(text.text), OffsetMapping.Identity)
}

private val symbolKeys = listOf("⇥", "{", "}", "(", ")", ";", ":", "&", "*", "\"", "'", "!", "<", ">", "=", "|", "[", "]", "_", "#", "?", ".")

object Editing {
    /** Inserts [s] at the cursor, replacing any selection. */
    fun insert(v: TextFieldValue, s: String): TextFieldValue {
        val start = minOf(v.selection.start, v.selection.end)
        val end = maxOf(v.selection.start, v.selection.end)
        val text = v.text.replaceRange(start, end, s)
        return TextFieldValue(text, TextRange(start + s.length))
    }

    /**
     * When the user just typed a newline, carries over the previous line's
     * indentation (one level deeper after an opening brace).
     */
    fun autoIndent(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val typedNewline = new.text.length == old.text.length + 1 &&
            new.selection.collapsed && new.selection.start > 0 &&
            new.text[new.selection.start - 1] == '\n'
        if (!typedNewline) return new
        val cursor = new.selection.start
        val prevLineStart = new.text.lastIndexOf('\n', cursor - 2) + 1
        val prevLine = new.text.substring(prevLineStart, cursor - 1)
        var indent = prevLine.takeWhile { it == ' ' }
        if (prevLine.trimEnd().endsWith("{") || prevLine.trimEnd().endsWith("(")) indent += "    "
        return if (indent.isEmpty()) new else insert(new, indent)
    }
}

@Composable
fun PlaygroundScreen(
    initialCode: String,
    onCodeChange: (String) -> Unit,
    onRun: () -> Unit,
    contentPadding: PaddingValues,
) {
    var value by remember { mutableStateOf(TextFieldValue(initialCode)) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RunResult?>(null) }
    var examplesOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun update(v: TextFieldValue) {
        value = v
        onCodeChange(v.text)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Playground", style = MaterialTheme.typography.headlineMedium)
                Text("Write Rust, run it for real", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                TextButton(onClick = { examplesOpen = true }) {
                    Icon(Icons.Rounded.MenuBook, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Examples")
                }
                DropdownMenu(expanded = examplesOpen, onDismissRequest = { examplesOpen = false }) {
                    PlaygroundExamples.all.forEach { ex ->
                        DropdownMenuItem(
                            text = { Text(ex.title) },
                            onClick = {
                                examplesOpen = false
                                update(TextFieldValue(ex.code))
                                result = null
                            },
                        )
                    }
                }
            }
        }

        // Editor
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CodeColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            val lines = value.text.count { it == '\n' } + 1
            Text(
                (1..lines).joinToString("\n"),
                style = CodeTextStyle,
                color = CodeColors.Gutter,
                textAlign = TextAlign.End,
                modifier = Modifier.width(36.dp).padding(end = 8.dp),
            )
            BoxWithConstraints(Modifier.weight(1f)) {
              val minWidth = maxWidth
              Box(Modifier.horizontalScroll(rememberScrollState())) {
                BasicTextField(
                    value = value,
                    onValueChange = { update(Editing.autoIndent(value, it)) },
                    textStyle = CodeTextStyle.copy(color = CodeColors.Plain),
                    cursorBrush = SolidColor(Brand.RustLight),
                    visualTransformation = HighlightTransformation,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                    ),
                    modifier = Modifier.widthIn(min = minWidth).heightIn(min = 200.dp).padding(end = 16.dp),
                )
              }
            }
        }

        // Symbol row: the characters Rust needs that phone keyboards hide.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            symbolKeys.forEach { k ->
                Box(
                    Modifier
                        .size(width = 40.dp, height = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { update(Editing.insert(value, if (k == "⇥") "    " else k)) },
                    contentAlignment = Alignment.Center,
                ) { Text(k, fontFamily = Mono, fontWeight = FontWeight.Bold) }
            }
        }

        AnimatedVisibility(result != null) {
            val r = result
            if (r != null) OutputPanel(r)
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Runs on play.rust-lang.org (needs internet)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (running) MaterialTheme.colorScheme.surfaceContainerHighest else RustleTheme.colors.success)
                    .clickable(enabled = !running) {
                        running = true
                        onRun()
                        scope.launch {
                            result = try {
                                PlaygroundClient.run(value.text)
                            } catch (e: Exception) {
                                RunResult(false, "", "Couldn't reach the playground: ${e.message ?: e::class.simpleName}")
                            }
                            running = false
                        }
                    }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (running) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
                } else {
                    Icon(Icons.Rounded.PlayArrow, null, tint = androidx.compose.ui.graphics.Color.White)
                }
                Spacer(Modifier.width(6.dp))
                Text(if (running) "Running…" else "Run", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun OutputPanel(r: RunResult) {
    val c = RustleTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .heightIn(max = 220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CodeColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        Text(
            if (r.success) "✅ Success" else "💥 Failed",
            color = if (r.success) c.success else c.error,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(6.dp))
        if (r.stdout.isNotEmpty()) Text(r.stdout.trimEnd(), style = CodeTextStyle, color = CodeColors.Plain)
        if (r.stderr.isNotEmpty()) {
            if (r.stdout.isNotEmpty()) Spacer(Modifier.height(8.dp))
            Text(r.stderr, style = CodeTextStyle, color = if (r.success) CodeColors.Comment else c.onErrorContainer)
        }
        if (r.stdout.isEmpty() && r.stderr.isEmpty()) Text("(no output)", style = CodeTextStyle, color = CodeColors.Comment)
    }
}
