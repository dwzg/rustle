package io.github.dwzg.rustle.ui.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dwzg.rustle.core.Answer
import io.github.dwzg.rustle.core.Step
import io.github.dwzg.rustle.core.StepContent
import io.github.dwzg.rustle.ui.code.CodeBlock
import io.github.dwzg.rustle.ui.code.CodeColors
import io.github.dwzg.rustle.ui.code.CodePanel
import io.github.dwzg.rustle.ui.code.FillCodeBlock
import io.github.dwzg.rustle.ui.code.RustHighlighter
import io.github.dwzg.rustle.ui.components.Ferris
import io.github.dwzg.rustle.ui.components.FerrisMood
import io.github.dwzg.rustle.ui.components.MarkdownBody
import io.github.dwzg.rustle.ui.components.OptionTile
import io.github.dwzg.rustle.ui.components.inlineMarkdown
import io.github.dwzg.rustle.ui.theme.CodeTextStyle
import io.github.dwzg.rustle.ui.theme.Mono
import io.github.dwzg.rustle.ui.theme.RustleTheme

private val codeish = Regex("""[;{}()\[\]=<>&:!]|^\s*(let|fn|const|struct|impl|use)\b""")

@Composable
private fun Prompt(kicker: String, text: String) {
    Column {
        Text(
            kicker.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(inlineMarkdown(text), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun ExplainView(step: Step, c: StepContent.Explain) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ferris(size = 56.dp, mood = FerrisMood.Thinking)
            Spacer(Modifier.width(12.dp))
            Text(step.prompt, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        MarkdownBody(c.body)
    }
}

@Composable
fun ChoiceView(step: Step, c: StepContent.Choice, feedback: Feedback?, onAnswer: (Answer?) -> Unit) {
    val options = remember(step) { (c.wrong + c.answer).shuffled() }
    var selected by remember(step) { mutableStateOf<String?>(null) }
    // Prose options may contain `inline code`; only bare code options get a monospace font.
    val mono = options.none { '`' in it } && options.any { codeish.containsMatchIn(it) }
    val ok = RustleTheme.colors.success
    val bad = RustleTheme.colors.error
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Prompt("Choose the answer", step.prompt)
        c.code?.let { CodeBlock(it) }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { opt ->
                val tint = when {
                    feedback == null -> null
                    opt == c.answer -> ok
                    opt == selected -> bad
                    else -> null
                }
                OptionTile(
                    selected = opt == selected,
                    enabled = feedback == null,
                    tint = tint,
                    onClick = { selected = opt; onAnswer(Answer.Choice(opt)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (mono) androidx.compose.ui.text.AnnotatedString(opt) else inlineMarkdown(opt),
                        fontFamily = if (mono) Mono else null,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private data class BankToken(val id: Int, val text: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillView(step: Step, c: StepContent.Fill, feedback: Feedback?, onAnswer: (Answer?) -> Unit) {
    val bank = remember(step) {
        (c.answers + c.distractors).mapIndexed { i, t -> BankToken(i, t) }.shuffled()
    }
    val slots = remember(step) { mutableStateListOf<BankToken?>().apply { repeat(c.answers.size) { add(null) } } }
    val active = slots.indexOfFirst { it == null }.takeIf { it >= 0 }

    LaunchedEffect(slots.toList()) {
        onAnswer(if (slots.all { it != null }) Answer.Blanks(slots.map { it!!.text }) else null)
    }
    val ok = RustleTheme.colors.success
    val bad = RustleTheme.colors.error

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Prompt("Fill in the blanks", step.prompt)
        FillCodeBlock(
            code = c.code,
            filled = slots.map { it?.text },
            active = if (feedback == null) active else null,
            onSlotClick = { i -> if (feedback == null) slots[i] = null },
            slotColor = { i ->
                if (feedback == null) null
                else if (slots.getOrNull(i)?.text == c.answers[i]) ok else bad
            },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            bank.forEach { token ->
                val used = slots.any { it?.id == token.id }
                TokenChip(token.text, used = used, enabled = feedback == null && !used && active != null) {
                    active?.let { slots[it] = token }
                }
            }
        }
    }
}

@Composable
private fun TokenChip(text: String, used: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val border = MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .clip(shape)
            .background(if (used) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(2.dp, if (used) Color.Transparent else border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            color = if (used) Color.Transparent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class PoolLine(val id: Int, val text: String)

@Composable
fun OrderView(step: Step, c: StepContent.Order, feedback: Feedback?, onAnswer: (Answer?) -> Unit) {
    val pool = remember(step) {
        val lines = c.lines.mapIndexed { i, l -> PoolLine(i, l) }
        var shuffled = lines.shuffled()
        var tries = 0
        while (shuffled.map { it.text.trim() } == lines.map { it.text.trim() } && tries++ < 10) shuffled = lines.shuffled()
        shuffled
    }
    val placed = remember(step) { mutableStateListOf<PoolLine>() }
    LaunchedEffect(placed.toList()) {
        onAnswer(if (placed.size == pool.size) Answer.Order(placed.map { it.text }) else null)
    }
    val ok = RustleTheme.colors.success
    val bad = RustleTheme.colors.error

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Prompt("Build the program", step.prompt)
        CodePanel(Modifier.heightIn(min = 96.dp)) {
            Column(Modifier.width(IntrinsicSize.Max)) {
                if (placed.isEmpty()) {
                    Text(
                        "  Tap lines below to add them here",
                        style = CodeTextStyle,
                        color = CodeColors.Gutter,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
                    )
                }
                placed.forEachIndexed { i, line ->
                    val tint = when {
                        feedback == null -> Color.Transparent
                        line.text.trim() == c.lines[i].trim() -> ok.copy(alpha = 0.18f)
                        else -> bad.copy(alpha = 0.22f)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(tint)
                            .clickable(enabled = feedback == null) { placed.remove(line) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${i + 1}",
                            style = CodeTextStyle,
                            color = CodeColors.Gutter,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(34.dp).padding(end = 10.dp),
                        )
                        Text(RustHighlighter.highlight(line.text), style = CodeTextStyle, softWrap = false, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pool.filter { it !in placed }.forEach { line ->
                OptionTile(
                    selected = false,
                    enabled = feedback == null,
                    onClick = { placed.add(line) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(RustHighlighterOnSurface(line.text.trim()), fontFamily = Mono, fontSize = 14.sp, softWrap = false)
                }
            }
        }
    }
}

/** Plain monospace text for line tiles (they sit on the app surface, not a dark code panel). */
@Composable
private fun RustHighlighterOnSurface(text: String) = androidx.compose.ui.text.AnnotatedString(text)

@Composable
fun CompilesView(step: Step, c: StepContent.Compiles, feedback: Feedback?, onAnswer: (Answer?) -> Unit) {
    var choice by remember(step) { mutableStateOf<Boolean?>(null) }
    val ok = RustleTheme.colors.success
    val bad = RustleTheme.colors.error
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Prompt("Compiler says…", step.prompt)
        CodeBlock(c.code)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(true to "✅  Compiles", false to "💥  Error").forEach { (value, label) ->
                val tint = when {
                    feedback == null -> null
                    value == c.compiles -> ok
                    value == choice -> bad
                    else -> null
                }
                OptionTile(
                    selected = choice == value,
                    enabled = feedback == null,
                    tint = tint,
                    onClick = { choice = value; onAnswer(Answer.Compiles(value)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun BugView(step: Step, c: StepContent.Bug, feedback: Feedback?, onAnswer: (Answer?) -> Unit) {
    var line by remember(step) { mutableStateOf<Int?>(null) }
    val ok = RustleTheme.colors.success
    val bad = RustleTheme.colors.error
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Prompt("Spot the bug", step.prompt)
        Text(
            "Tap a line",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CodeBlock(
            c.code,
            onLineClick = if (feedback == null) { n -> line = n; onAnswer(Answer.Line(n)) } else null,
            lineTint = { n ->
                when {
                    feedback != null && n == c.bugLine -> ok.copy(alpha = 0.25f)
                    feedback != null && n == line -> bad.copy(alpha = 0.25f)
                    n == line -> CodeColors.Selected
                    else -> Color.Transparent
                }
            },
        )
        AnimatedVisibility(line != null && feedback == null, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
            Text(
                "Line ${line ?: ""} selected",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
