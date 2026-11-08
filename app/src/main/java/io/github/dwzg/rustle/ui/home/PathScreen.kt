package io.github.dwzg.rustle.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dwzg.rustle.core.Course
import io.github.dwzg.rustle.core.CourseUnit
import io.github.dwzg.rustle.core.Lesson
import io.github.dwzg.rustle.core.LessonState
import io.github.dwzg.rustle.core.LessonStatus
import io.github.dwzg.rustle.core.Stats
import io.github.dwzg.rustle.course.exerciseCount
import io.github.dwzg.rustle.course.fraction
import io.github.dwzg.rustle.ui.components.ChunkyButton
import io.github.dwzg.rustle.ui.components.Ferris
import io.github.dwzg.rustle.ui.components.FerrisMood
import io.github.dwzg.rustle.ui.components.StatPill
import io.github.dwzg.rustle.ui.components.XpBar
import io.github.dwzg.rustle.ui.theme.Brand
import io.github.dwzg.rustle.ui.theme.RustleTheme
import kotlin.math.sin

/** Two-tone gradients cycled through the units. */
val UnitPalettes = listOf(
    Color(0xFFF74C00) to Color(0xFFFF8A4C),
    Color(0xFF7C4DFF) to Color(0xFFB388FF),
    Color(0xFF00A6A6) to Color(0xFF4FD6BE),
    Color(0xFFE0284A) to Color(0xFFFF6F8A),
    Color(0xFF2F7DF6) to Color(0xFF6CD4FF),
    Color(0xFFD99A00) to Color(0xFFFFC23D),
    Color(0xFF1FA463) to Color(0xFF5FE09C),
)

private sealed interface PathItem {
    data class Header(val unit: CourseUnit, val index: Int) : PathItem
    data class Node(val lesson: Lesson, val unitIndex: Int, val position: Int) : PathItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathScreen(
    course: Course,
    stats: Stats,
    /** Lesson id -> state, computed by the Rust core. */
    statuses: Map<String, LessonStatus>,
    onStartLesson: (Lesson) -> Unit,
    onPractice: () -> Unit,
    canPractice: Boolean,
    contentPadding: PaddingValues,
) {
    val items = remember(course) {
        buildList {
            var pos = 0
            course.units.forEachIndexed { u, unit ->
                add(PathItem.Header(unit, u))
                unit.lessons.forEach { add(PathItem.Node(it, u, pos++)) }
            }
        }
    }
    val next = course.units.flatMap { it.lessons }.firstOrNull { statuses[it.id]?.state == LessonState.CURRENT }
    val listState = rememberLazyListState()
    var scrolled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(next?.id) {
        if (!scrolled && next != null) {
            val idx = items.indexOfFirst { it is PathItem.Node && it.lesson.id == next.id }
            if (idx > 2) listState.scrollToItem(idx - 2)
            scrolled = true
        }
    }
    var sheetLesson by remember { mutableStateOf<Lesson?>(null) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
    ) {
        item { StatsHeader(stats, canPractice, onPractice) }
        items(items, key = { if (it is PathItem.Header) "u-${it.unit.id}" else "l-${(it as PathItem.Node).lesson.id}" }) { item ->
            when (item) {
                is PathItem.Header -> UnitHeader(item.unit, item.index, statuses)
                is PathItem.Node -> {
                    val lesson = item.lesson
                    LessonNode(
                        lesson = lesson,
                        position = item.position,
                        palette = UnitPalettes[item.unitIndex % UnitPalettes.size],
                        state = statuses[lesson.id]?.state ?: LessonState.LOCKED,
                        stars = statuses[lesson.id]?.stars ?: 0,
                        onClick = { sheetLesson = lesson },
                    )
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Ferris(size = 90.dp, mood = if (next == null) FerrisMood.Party else FerrisMood.Thinking)
                Text(
                    if (next == null) "You finished the course. You're a Rustacean! 🎓" else "More adventures ahead…",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    sheetLesson?.let { lesson ->
        val unlocked = statuses[lesson.id]?.state != LessonState.LOCKED
        val unit = course.units.firstOrNull { u -> u.lessons.any { it.id == lesson.id } }
        ModalBottomSheet(
            onDismissRequest = { sheetLesson = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text(
                    "${unit?.icon ?: ""}  ${unit?.title ?: ""}".uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                )
                Text(lesson.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                val stars = statuses[lesson.id]?.stars?.takeIf { it > 0 }
                Text(
                    buildString {
                        append("${lesson.exerciseCount} exercises")
                        if (stars != null) append("  ·  " + "★".repeat(stars) + "☆".repeat(3 - stars))
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                if (unlocked) {
                    ChunkyButton(
                        if (stars != null) "Practice again" else "Start lesson",
                        onClick = { sheetLesson = null; onStartLesson(lesson) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Finish the previous lesson to unlock this one. (Or turn on Explorer mode in your profile.)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(stats: Stats, canPractice: Boolean, onPractice: () -> Unit) {
    val level = stats.level
    val streak = stats.streak
    val todayXp = stats.xpToday
    val goal = stats.dailyGoal
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ferris(size = 64.dp, mood = if (todayXp >= goal) FerrisMood.Party else FerrisMood.Happy)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Level ${level.level}", style = MaterialTheme.typography.titleLarge)
                Text(level.title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
            StatPill("🔥", "$streak", Brand.Flame)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            XpBar(level.fraction, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text("${level.into}/${level.span} XP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DailyGoalRing(todayXp.toFloat() / goal)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Daily goal", fontWeight = FontWeight.Bold)
                Text(
                    if (todayXp >= goal) "Done for today! 🎉" else "$todayXp / $goal XP",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canPractice) {
                val mistakes = stats.mistakes
                ChunkyButton(
                    if (mistakes > 0) "Practice ($mistakes)" else "Practice",
                    onClick = onPractice,
                    color = MaterialTheme.colorScheme.tertiary,
                    shadow = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                    contentColor = Color(0xFF00222F),
                    height = 40.dp,
                    modifier = Modifier.width(150.dp),
                )
            }
        }
    }
}

@Composable
private fun DailyGoalRing(fraction: Float) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(Modifier.size(36.dp)) {
        val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        drawArc(track, 0f, 360f, false, style = stroke)
        drawArc(Brand.Gold, -90f, 360f * fraction.coerceIn(0f, 1f), false, style = stroke)
    }
}

@Composable
private fun UnitHeader(unit: CourseUnit, index: Int, statuses: Map<String, LessonStatus>) {
    val (a, b) = UnitPalettes[index % UnitPalettes.size]
    val done = unit.lessons.count { statuses[it.id]?.state == LessonState.DONE }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(a, b)))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "UNIT ${index + 1}  ·  $done/${unit.lessons.size}",
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
            )
            Text(unit.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(unit.tagline, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) { Text(unit.icon, fontSize = 26.sp) }
    }
}

@Composable
private fun LessonNode(
    lesson: Lesson,
    position: Int,
    palette: Pair<Color, Color>,
    state: LessonState,
    stars: Int,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(1f, 1.18f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "ring")
    val locked = RustleTheme.colors.locked
    val face = when (state) {
        LessonState.LOCKED -> locked
        LessonState.DONE -> Brand.Gold
        else -> palette.first
    }
    val shadow = when (state) {
        LessonState.LOCKED -> MaterialTheme.colorScheme.outlineVariant
        LessonState.DONE -> Color(0xFFB98500)
        else -> Color(palette.first.red * 0.62f, palette.first.green * 0.62f, palette.first.blue * 0.62f)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(128.dp)) {
        // Winding path: nodes sway left and right like a crab's sidestep.
        val sway = sin(position * 0.9).toFloat()
        val x = maxWidth / 2 + (maxWidth * 0.22f) * sway - 60.dp
        Column(
            Modifier.offset(x = x).width(120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                if (state == LessonState.CURRENT) {
                    Box(
                        Modifier
                            .size(78.dp)
                            .scale(ring)
                            .border(4.dp, palette.second.copy(alpha = 0.6f), CircleShape),
                    )
                }
                Box(Modifier.size(68.dp).offset(y = 5.dp).clip(CircleShape).background(shadow))
                Box(
                    Modifier
                        .size(68.dp)
                        .offset(y = if (pressed) 5.dp else 0.dp)
                        .clip(CircleShape)
                        .background(face)
                        .clickable(interactionSource = source, indication = null, onClick = onClick)
                        .semantics { contentDescription = "Lesson: ${lesson.title}" },
                    contentAlignment = Alignment.Center,
                ) {
                    when (state) {
                        LessonState.LOCKED -> Icon(Icons.Rounded.Lock, "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        LessonState.DONE -> Icon(Icons.Rounded.Star, "Completed", tint = Color.White, modifier = Modifier.size(36.dp))
                        LessonState.CURRENT -> Text("🦀", fontSize = 30.sp)
                        LessonState.OPEN -> Text("▶", fontSize = 24.sp, color = Color.White)
                    }
                }
            }
            Text(
                lesson.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (state == LessonState.LOCKED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state == LessonState.DONE) {
                Text(
                    "★".repeat(stars) + "☆".repeat(3 - stars),
                    color = Brand.Gold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
