package io.github.dwzg.rustle.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dwzg.rustle.course.CompilesStep
import io.github.dwzg.rustle.course.Step
import io.github.dwzg.rustle.ui.lesson.LessonScreen
import io.github.dwzg.rustle.ui.lesson.LessonSession
import io.github.dwzg.rustle.ui.lesson.ResultScreen
import io.github.dwzg.rustle.ui.home.PathScreen
import io.github.dwzg.rustle.ui.playground.PlaygroundExamples
import io.github.dwzg.rustle.ui.playground.PlaygroundScreen
import io.github.dwzg.rustle.ui.profile.ProfileScreen
import io.github.dwzg.rustle.ui.rush.MissedCards
import io.github.dwzg.rustle.ui.rush.RushGame
import io.github.dwzg.rustle.ui.rush.RushIntro

enum class Tab(val label: String, val icon: ImageVector) {
    Learn("Learn", Icons.Rounded.School),
    Rush("Rush", Icons.Rounded.Bolt),
    Playground("Playground", Icons.Rounded.Code),
    Profile("Profile", Icons.Rounded.Person),
}

/** Full-screen flows shown on top of the tabs. */
sealed interface Overlay {
    data class Session(val runId: Long, val title: String, val steps: List<Step>, val lessonId: String?) : Overlay
    data class Rush(val runId: Long, val deck: List<CompilesStep>) : Overlay
    data class Result(val reward: Reward, val headline: String, val missed: List<CompilesStep>? = null) : Overlay
}

/** Navigation state, kept in a ViewModel so it survives rotation. */
class NavState : ViewModel() {
    var tab by mutableStateOf(Tab.Learn)
    var overlay by mutableStateOf<Overlay?>(null)
}

@Composable
fun RustleApp(app: AppState) {
    val nav: NavState = viewModel { NavState() }
    val progress by app.progress.collectAsStateWithLifecycle()
    val today = app.store.today()

    AnimatedContent(
        targetState = nav.overlay,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut() },
        contentKey = { o ->
            when (o) {
                null -> "tabs"
                is Overlay.Session -> "s${o.runId}"
                is Overlay.Rush -> "r${o.runId}"
                is Overlay.Result -> "result"
            }
        },
        label = "overlay",
    ) { overlay ->
        when (overlay) {
            null -> Tabs(app, nav, progress, today)
            is Overlay.Session -> {
                val session: LessonSession = viewModel(key = "session-${overlay.runId}") { LessonSession(overlay.steps) }
                LessonScreen(
                    session = session,
                    title = overlay.title,
                    onExit = { nav.overlay = null },
                    onFinished = {
                        val reward = if (overlay.lessonId != null) {
                            app.completeLesson(overlay.lessonId, session.firstTry, session.bestCombo)
                        } else {
                            app.completePractice(session.firstTry, session.bestCombo)
                        }
                        nav.overlay = Overlay.Result(
                            reward,
                            if (overlay.lessonId != null) "Lesson complete!" else "Practice done!",
                        )
                    },
                )
            }
            is Overlay.Rush -> RushGame(
                deck = overlay.deck,
                onQuit = { nav.overlay = null },
                onFinish = { r ->
                    val reward = app.completeRush(r.score, r.correct, r.answered, r.bestCombo)
                    nav.overlay = Overlay.Result(reward, "Score: ${r.score}", r.missed)
                },
            )
            is Overlay.Result -> ResultScreen(
                reward = overlay.reward,
                headline = overlay.headline,
                onDone = { nav.overlay = null },
                extra = overlay.missed?.let { missed -> { MissedCards(missed) } },
            )
        }
    }
}

@Composable
private fun Tabs(app: AppState, nav: NavState, progress: io.github.dwzg.rustle.data.Progress, today: Long) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = nav.tab == tab,
                        onClick = { nav.tab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (nav.tab) {
            Tab.Learn -> PathScreen(
                course = app.course,
                progress = progress,
                today = today,
                canPractice = app.canPractice(progress),
                onStartLesson = { lesson ->
                    nav.overlay = Overlay.Session(System.nanoTime(), lesson.title, lesson.steps, lesson.id)
                },
                onPractice = {
                    val set = app.practiceSet(progress)
                    if (set.isNotEmpty()) nav.overlay = Overlay.Session(System.nanoTime(), "practice", set, null)
                },
                contentPadding = padding,
            )
            Tab.Rush -> RushIntro(
                best = progress.rushBest,
                games = progress.rushGames,
                onStart = { nav.overlay = Overlay.Rush(System.nanoTime(), app.rushDeck(progress)) },
                contentPadding = padding,
            )
            Tab.Playground -> PlaygroundScreen(
                initialCode = app.store.playgroundCode ?: PlaygroundExamples.all.first().code,
                onCodeChange = { app.store.playgroundCode = it },
                onRun = { app.recordPlaygroundRun() },
                contentPadding = padding,
            )
            Tab.Profile -> ProfileScreen(
                course = app.course,
                progress = progress,
                today = today,
                onUnlockAll = app::setUnlockAll,
                onReset = app::reset,
                contentPadding = padding,
            )
        }
    }
}
