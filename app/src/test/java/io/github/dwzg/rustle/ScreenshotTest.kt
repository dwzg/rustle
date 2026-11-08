package io.github.dwzg.rustle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dwzg.rustle.course.ExplainStep
import io.github.dwzg.rustle.course.Step
import io.github.dwzg.rustle.data.Achievements
import io.github.dwzg.rustle.data.Levels
import io.github.dwzg.rustle.data.Progress
import io.github.dwzg.rustle.ui.Reward
import io.github.dwzg.rustle.ui.home.PathScreen
import io.github.dwzg.rustle.ui.lesson.LessonScreen
import io.github.dwzg.rustle.ui.lesson.LessonSession
import io.github.dwzg.rustle.ui.lesson.ResultScreen
import io.github.dwzg.rustle.ui.playground.PlaygroundExamples
import io.github.dwzg.rustle.ui.playground.PlaygroundScreen
import io.github.dwzg.rustle.ui.profile.ProfileScreen
import io.github.dwzg.rustle.ui.rush.RushGame
import io.github.dwzg.rustle.ui.rush.RushIntro
import io.github.dwzg.rustle.ui.theme.RustleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the main screens to PNGs under app/build/outputs/roborazzi.
 * Run with: ./gradlew :app:recordRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val course = CourseContentTest.loadCourse()
    private val today = 20_000L
    private val progress = Progress(
        xp = 260,
        stars = course.lessons.take(4).associate { it.id to 3 } + (course.lessons[4].id to 2),
        streak = 4, bestStreak = 6, lastActiveDay = today, xpToday = 18,
        rushBest = 1800, rushGames = 3, answered = 60, answeredCorrect = 51,
        mistakes = setOf(course.lessons[0].steps[1].key),
    )

    private fun shot(name: String, dark: Boolean = true, content: @Composable () -> Unit) {
        compose.setContent {
            RustleTheme(dark = dark) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
            }
        }
        compose.mainClock.advanceTimeBy(1200)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun stepOf(kind: Class<out Step>, lesson: Int = 2): Step =
        course.lessons.drop(lesson).flatMap { it.steps }.first { kind.isInstance(it) }

    private fun lessonShot(name: String, step: Step) = shot(name) {
        LessonScreen(LessonSession(listOf(step)), "Lesson", onExit = {}, onFinished = {})
    }

    @Test fun path() = shot("01_path") {
        PathScreen(course, progress, today, {}, {}, canPractice = true, contentPadding = PaddingValues())
    }

    @Test fun pathLight() = shot("02_path_light", dark = false) {
        PathScreen(course, progress, today, {}, {}, canPractice = true, contentPadding = PaddingValues())
    }

    @Test fun explain() = lessonShot("03_explain", course.lessons[6].steps.first { it is ExplainStep })
    @Test fun choice() = lessonShot("04_choice", stepOf(io.github.dwzg.rustle.course.ChoiceStep::class.java, 9))
    @Test fun fill() = lessonShot("05_fill", stepOf(io.github.dwzg.rustle.course.FillStep::class.java, 3))
    @Test fun order() = lessonShot("06_order", stepOf(io.github.dwzg.rustle.course.OrderStep::class.java, 12))
    @Test fun compiles() = lessonShot("07_compiles", stepOf(io.github.dwzg.rustle.course.CompilesStep::class.java, 13))
    @Test fun bug() = lessonShot("08_bug", stepOf(io.github.dwzg.rustle.course.BugStep::class.java, 15))

    @Test fun result() = shot("09_result") {
        ResultScreen(
            Reward(22, 3, 7, 7, 7, Levels.of(40), Levels.of(62), 5, Achievements.all.take(1)),
            "Lesson complete!",
            onDone = {},
        )
    }

    @Test fun rushIntro() = shot("10_rush_intro") { RushIntro(1800, 3, {}, PaddingValues()) }
    @Test fun rushGame() = shot("11_rush_game") { RushGame(course.compileCards.drop(20), {}, {}) }

    @Test fun playground() = shot("12_playground") {
        PlaygroundScreen(PlaygroundExamples.all[2].code, {}, {}, PaddingValues())
    }

    @Test fun profile() = shot("13_profile") {
        ProfileScreen(course, progress, today, {}, {}, PaddingValues())
    }
}
