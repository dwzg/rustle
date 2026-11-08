package io.github.dwzg.rustle.ui.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dwzg.rustle.core.Answer
import io.github.dwzg.rustle.core.Step
import io.github.dwzg.rustle.core.StepContent
import io.github.dwzg.rustle.core.solution
import io.github.dwzg.rustle.course.isExercise
import io.github.dwzg.rustle.ui.components.ChunkyButton
import io.github.dwzg.rustle.ui.components.Ferris
import io.github.dwzg.rustle.ui.components.FerrisMood
import io.github.dwzg.rustle.ui.components.XpBar
import io.github.dwzg.rustle.ui.components.inlineMarkdown
import io.github.dwzg.rustle.ui.theme.Brand
import io.github.dwzg.rustle.ui.theme.RustleTheme

private val praise = listOf("Nice!", "Correct!", "Ferris approves!", "Zero-cost win!", "Compiles in your head!", "Crabtastic!")
private val comfort = listOf("Not quite", "Close one", "The borrow checker disagrees", "Almost!")

@Composable
fun LessonScreen(
    session: LessonSession,
    title: String,
    onExit: () -> Unit,
    onFinished: () -> Unit,
) {
    var confirmExit by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<Answer?>(null) }
    val haptics = LocalHapticFeedback.current

    BackHandler { confirmExit = true }
    LaunchedEffect(session.finished) { if (session.finished) onFinished() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopBar(session, onClose = { confirmExit = true })

        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = session.index,
                transitionSpec = {
                    (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut())
                },
                label = "step",
            ) { index ->
                val step = session.queue.getOrNull(index) ?: return@AnimatedContent
                // Each queue position gets fresh UI state, even when a step is repeated.
                androidx.compose.runtime.key(index) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        val isRetry = step.isExercise && session.queue.take(index).any { it.key == step.key }
                        if (isRetry) {
                            RetryBadge()
                            Spacer(Modifier.height(10.dp))
                        }
                        StepContent(step, session.feedback?.takeIf { it.step === step }) { pending = it }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }

        BottomArea(
            step = session.current,
            feedback = session.feedback,
            canCheck = pending != null,
            onCheck = {
                pending?.let {
                    val ok = session.check(it)
                    haptics.performHapticFeedback(if (ok) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
                }
            },
            onContinue = {
                pending = null
                session.next()
            },
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            icon = { Ferris(size = 64.dp, mood = FerrisMood.Sad) },
            title = { Text("Leave $title?") },
            text = { Text("Your progress in this lesson will be lost.") },
            confirmButton = { TextButton(onClick = onExit) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Keep learning") } },
        )
    }
}

@Composable
private fun RetryBadge() {
    Text(
        "↻  Let's try that one again",
        color = Brand.Flame,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brand.Flame.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun TopBar(session: LessonSession, onClose: () -> Unit) {
    val progress by animateFloatAsState(session.progress, spring(stiffness = 120f), label = "progress")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close lesson") }
        XpBar(progress, Modifier.weight(1f), color = Brand.Success, height = 16.dp)
        AnimatedVisibility(session.combo >= 2, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 18.sp)
                Text("${session.combo}", color = Brand.Flame, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun StepContent(step: Step, feedback: Feedback?, onAnswer: (Answer?) -> Unit) {
    when (val c = step.content) {
        is StepContent.Explain -> ExplainView(step, c)
        is StepContent.Choice -> ChoiceView(step, c, feedback, onAnswer)
        is StepContent.Fill -> FillView(step, c, feedback, onAnswer)
        is StepContent.Order -> OrderView(step, c, feedback, onAnswer)
        is StepContent.Compiles -> CompilesView(step, c, feedback, onAnswer)
        is StepContent.Bug -> BugView(step, c, feedback, onAnswer)
    }
}

@Composable
private fun BottomArea(
    step: Step?,
    feedback: Feedback?,
    canCheck: Boolean,
    onCheck: () -> Unit,
    onContinue: () -> Unit,
) {
    AnimatedContent(
        targetState = feedback,
        transitionSpec = { slideInVertically { it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut() },
        label = "bottom",
    ) { fb ->
        if (fb == null) {
            Box(Modifier.fillMaxWidth().padding(20.dp)) {
                if (step != null && !step.isExercise) {
                    ChunkyButton("Got it", onClick = onContinue, modifier = Modifier.fillMaxWidth())
                } else {
                    ChunkyButton("Check", onClick = onCheck, enabled = canCheck, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            FeedbackPanel(fb, onContinue)
        }
    }
}

@Composable
private fun FeedbackPanel(fb: Feedback, onContinue: () -> Unit) {
    val c = RustleTheme.colors
    val bg = if (fb.correct) c.successContainer else c.errorContainer
    val fg = if (fb.correct) c.onSuccessContainer else c.onErrorContainer
    val accent = if (fb.correct) c.success else c.error
    val headline = remember(fb) { if (fb.correct) praise.random() else comfort.random() }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(bg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ferris(size = 44.dp, mood = if (fb.correct) FerrisMood.Party else FerrisMood.Sad)
            Spacer(Modifier.width(10.dp))
            Text(headline, style = MaterialTheme.typography.titleLarge, color = accent)
        }
        if (!fb.correct && fb.step.content !is StepContent.Order) {
            Text(
                "Answer: " + solution(fb.step),
                color = fg,
                fontWeight = FontWeight.Bold,
                fontFamily = if (fb.step.content is StepContent.Fill || fb.step.content is StepContent.Bug) FontFamily.Monospace else null,
            )
        }
        Column(Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)) {
            Text(inlineMarkdown(fb.step.explanation), color = fg, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
        }
        Spacer(Modifier.height(4.dp))
        ChunkyButton(
            "Continue",
            onClick = onContinue,
            color = accent,
            shadow = accent.copy(alpha = 0.6f).compositeOverDark(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun androidx.compose.ui.graphics.Color.compositeOverDark() =
    androidx.compose.ui.graphics.Color(red * 0.62f, green * 0.62f, blue * 0.62f, 1f)
