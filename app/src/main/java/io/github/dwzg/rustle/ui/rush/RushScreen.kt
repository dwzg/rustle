package io.github.dwzg.rustle.ui.rush

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dwzg.rustle.core.Step
import io.github.dwzg.rustle.core.StepContent
import io.github.dwzg.rustle.core.rushMultiplier
import io.github.dwzg.rustle.core.rushPoints
import io.github.dwzg.rustle.ui.code.CodeBlock
import io.github.dwzg.rustle.ui.components.ChunkyButton
import io.github.dwzg.rustle.ui.components.Ferris
import io.github.dwzg.rustle.ui.components.FerrisMood
import io.github.dwzg.rustle.ui.components.XpBar
import io.github.dwzg.rustle.ui.components.inlineMarkdown
import io.github.dwzg.rustle.ui.theme.Brand
import io.github.dwzg.rustle.ui.theme.RustleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

const val RUSH_SECONDS = 45f
private const val PENALTY_SECONDS = 3f

data class RushResult(val score: Int, val correct: Int, val answered: Int, val bestCombo: Int, val missed: List<Step>)

/** Landing page of the Rush tab. */
@Composable
fun RushIntro(best: Int, games: Int, onStart: () -> Unit, contentPadding: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text("⚡", fontSize = 64.sp)
        Text("Compiler Rush", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "You are the compiler. Will it build or will it blow up? Decide fast!",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Rule("👉", "Swipe right or tap ✅ if it compiles")
        Rule("👈", "Swipe left or tap 💥 if rustc rejects it")
        Rule("🔥", "Streaks raise your multiplier up to ×4")
        Rule("⏱️", "${RUSH_SECONDS.toInt()} seconds. Mistakes cost ${PENALTY_SECONDS.toInt()} seconds")
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$best", style = MaterialTheme.typography.headlineMedium, color = Brand.Gold)
                Text("BEST", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$games", style = MaterialTheme.typography.headlineMedium)
                Text("GAMES", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(32.dp))
        ChunkyButton("Start rush", onClick = onStart, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Rule(icon: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RushGame(deck: List<Step>, onQuit: () -> Unit, onFinish: (RushResult) -> Unit) {
    var timeLeft by remember { mutableFloatStateOf(RUSH_SECONDS) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var cardIndex by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf<Boolean?>(null) }
    var penaltyShown by remember { mutableIntStateOf(0) }
    val missed = remember { mutableStateListOf<Step>() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }

    BackHandler(onBack = onQuit)

    LaunchedEffect(Unit) {
        while (timeLeft > 0f) {
            delay(50)
            timeLeft = (timeLeft - 0.05f).coerceAtLeast(0f)
        }
        done = true
        onFinish(RushResult(score, correct, answered, bestCombo, missed.distinct()))
    }

    val card = deck[cardIndex % deck.size]
    val cardContent = card.content as StepContent.Compiles

    fun answer(compiles: Boolean) {
        if (done || flash != null) return
        answered++
        val ok = compiles == cardContent.compiles
        if (ok) {
            score += rushPoints(combo)
            combo++
            bestCombo = maxOf(bestCombo, combo)
            correct++
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        } else {
            combo = 0
            timeLeft = (timeLeft - PENALTY_SECONDS).coerceAtLeast(0f)
            missed += card
            penaltyShown++
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
        }
        flash = ok
        scope.launch {
            dragX.animateTo(if (compiles) 1400f else -1400f, tween(180))
            cardIndex++
            dragX.snapTo(0f)
            flash = null
        }
    }

    val c = RustleTheme.colors
    val border by animateColorAsState(
        when (flash) {
            true -> c.success
            false -> c.error
            null -> when {
                dragX.value > 60f -> c.success.copy(alpha = (dragX.value / 300f).coerceIn(0f, 1f))
                dragX.value < -60f -> c.error.copy(alpha = (-dragX.value / 300f).coerceIn(0f, 1f))
                else -> Color.Transparent
            }
        },
        tween(100),
        label = "cardBorder",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQuit) { Icon(Icons.Rounded.Close, "Quit") }
            XpBar(
                timeLeft / RUSH_SECONDS,
                Modifier.weight(1f),
                color = if (timeLeft < 10f) c.error else Brand.Gold,
                height = 14.dp,
            )
            Text(
                "${timeLeft.toInt()}s",
                modifier = Modifier.padding(start = 10.dp).width(36.dp),
                fontWeight = FontWeight.Black,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(score, transitionSpec = { scaleIn(initialScale = 1.3f) + fadeIn() togetherWith fadeOut() }, label = "score") {
                Text("$it", style = MaterialTheme.typography.displaySmall)
            }
            Spacer(Modifier.weight(1f))
            if (combo >= 2) Text("🔥$combo  ", color = Brand.Flame, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(
                "×${rushMultiplier(combo)}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = dragX.value
                        rotationZ = dragX.value / 40f
                        alpha = 1f - (abs(dragX.value) / 1600f)
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(3.dp, border, RoundedCornerShape(24.dp))
                    .pointerInput(cardIndex) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    dragX.value > 220f -> answer(true)
                                    dragX.value < -220f -> answer(false)
                                    else -> scope.launch { dragX.animateTo(0f) }
                                }
                            },
                        ) { _, dx -> scope.launch { dragX.snapTo(dragX.value + dx) } }
                    }
                    .padding(14.dp),
            ) {
                Text(
                    "CARD ${cardIndex + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.verticalScroll(rememberScrollState())) { CodeBlock(cardContent.code) }
            }
            if (flash == false) {
                Text("−${PENALTY_SECONDS.toInt()}s", color = c.error, fontWeight = FontWeight.Black, fontSize = 28.sp, modifier = Modifier.align(Alignment.TopCenter))
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChunkyButton(
                "💥 Error",
                onClick = { answer(false) },
                color = c.error,
                shadow = Color(0xFF8E1328),
                modifier = Modifier.weight(1f),
            )
            ChunkyButton(
                "✅ Compiles",
                onClick = { answer(true) },
                color = c.success,
                shadow = Color(0xFF0E6B3D),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Review list of missed cards shown on the results screen. */
@Composable
fun MissedCards(missed: List<Step>) {
    if (missed.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ferris(size = 40.dp, mood = FerrisMood.Party)
            Spacer(Modifier.width(8.dp))
            Text("No mistakes. The borrow checker fears you.", fontWeight = FontWeight.Bold)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Review your misses", style = MaterialTheme.typography.titleMedium)
        missed.take(5).forEach { step ->
            val c = step.content as StepContent.Compiles
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CodeBlock(c.code)
                Text(
                    if (c.compiles) "✅ It compiles" else "💥 Compile error",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (c.compiles) RustleTheme.colors.success else RustleTheme.colors.error,
                )
                Text(inlineMarkdown(step.explanation), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
