package io.github.dwzg.rustle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.test.core.app.ApplicationProvider
import io.github.dwzg.rustle.CourseContentTest.Companion.correctAnswer
import io.github.dwzg.rustle.CourseContentTest.Companion.lessons
import io.github.dwzg.rustle.core.StepContent
import io.github.dwzg.rustle.course.exerciseCount
import io.github.dwzg.rustle.data.ProgressStore
import io.github.dwzg.rustle.ui.AppState
import io.github.dwzg.rustle.ui.RustleApp
import io.github.dwzg.rustle.ui.lesson.LessonScreen
import io.github.dwzg.rustle.ui.lesson.LessonSession
import io.github.dwzg.rustle.ui.theme.RustleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class FlowTest {
    @get:Rule
    val compose = createComposeRule()

    private val engine = CourseContentTest.loadEngine()
    private val course = engine.course()

    /** Renders every step of every lesson (plus its feedback panel) to catch UI crashes on any content. */
    @Test
    fun everyStepRenders() {
        var session by mutableStateOf(LessonSession(course.lessons.first().steps))
        compose.setContent {
            RustleTheme { LessonScreen(session, "test", onExit = {}, onFinished = {}) }
        }
        var rendered = 0
        for (lesson in course.lessons) {
            compose.runOnIdle { session = LessonSession(lesson.steps) }
            compose.waitForIdle()
            while (!session.finished) {
                val step = session.current!!
                val answer = correctAnswer(step)
                if (answer != null) {
                    compose.runOnIdle { assertTrue(step.key, session.check(answer)) }
                    compose.waitForIdle()
                    compose.onNodeWithText("CONTINUE").assertIsDisplayed()
                }
                compose.runOnIdle { session.next() }
                compose.waitForIdle()
                rendered++
            }
            assertEquals(lesson.exerciseCount, session.firstTry.size)
        }
        assertEquals(course.lessons.sumOf { it.steps.size }, rendered)
    }

    /** Opens the app, starts the first lesson and visits every tab. */
    @Test
    fun appSmoke() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val app = AppState(engine, ProgressStore(ctx).also { it.reset() })
        compose.setContent { RustleTheme { RustleApp(app) } }

        compose.onNodeWithText("Level 1").assertIsDisplayed()
        val first = course.lessons.first()
        compose.onNodeWithContentDescription("Lesson: ${first.title}").performClick()
        compose.onNodeWithText("START LESSON").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("GOT IT").assertIsDisplayed().performClick()
        compose.waitForIdle()
        val choice = first.steps[1].content as StepContent.Choice
        compose.onNodeWithText(choice.answer).performClick()
        compose.onNodeWithText("CHECK").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("CONTINUE").assertIsDisplayed()

        // Leave the lesson again.
        compose.onNodeWithContentDescription("Close lesson").performClick()
        compose.onNodeWithText("Leave").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Rush").performClick()
        compose.onNodeWithText("START RUSH").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("✅ COMPILES").onFirst().performClick()
        compose.onNodeWithContentDescription("Quit").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Playground").performClick()
        compose.onNodeWithText("Run").assertIsDisplayed()
        compose.onNodeWithText("Profile").performClick()
        compose.onNodeWithText("Achievements", substring = true).assertIsDisplayed()
    }
}
