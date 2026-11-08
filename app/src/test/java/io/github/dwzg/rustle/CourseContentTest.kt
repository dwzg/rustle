package io.github.dwzg.rustle

import io.github.dwzg.rustle.course.Answer
import io.github.dwzg.rustle.course.BugStep
import io.github.dwzg.rustle.course.ChoiceStep
import io.github.dwzg.rustle.course.CompilesStep
import io.github.dwzg.rustle.course.Course
import io.github.dwzg.rustle.course.CourseParser
import io.github.dwzg.rustle.course.ExplainStep
import io.github.dwzg.rustle.course.FillStep
import io.github.dwzg.rustle.course.Grader
import io.github.dwzg.rustle.course.OrderStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Loads the real course files and checks that the app's parser agrees with the verifier's rules. */
class CourseContentTest {
    companion object {
        fun loadCourse(): Course {
            val dir = File("src/main/assets/course")
            val files = dir.listFiles { f -> f.name.endsWith(".md") }!!.sortedBy { it.name }
            return Course(files.map { CourseParser.parseUnit(it.readText(), it.name) })
        }
    }

    private val course = loadCourse()

    @Test
    fun courseHasAllUnits() {
        assertEquals(13, course.units.size)
        assertTrue(course.lessons.size >= 39)
        assertTrue("Rush needs plenty of cards", course.compileCards.size >= 60)
    }

    @Test
    fun idsAndKeysAreUnique() {
        val ids = course.units.map { it.id } + course.lessons.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val keys = course.lessons.flatMap { l -> l.steps.map { it.key } }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun everyLessonTeachesAndTests() {
        for (l in course.lessons) {
            assertTrue("${l.id} has no explain", l.steps.any { it is ExplainStep })
            assertTrue("${l.id} has too few exercises", l.exerciseCount >= 3)
        }
    }

    @Test
    fun correctAnswersGradeAsCorrect() {
        for (step in course.lessons.flatMap { it.steps }) {
            val (right, wrong) = when (step) {
                is ExplainStep -> continue
                is ChoiceStep -> Answer.Choice(step.answer) to Answer.Choice(step.wrong.first())
                is FillStep -> {
                    assertEquals(step.key, step.answers.size, step.code.split(FillStep.BLANK).size - 1)
                    Answer.Blanks(step.answers) to Answer.Blanks(listOf(step.distractors.first()) + step.answers.drop(1))
                }
                is OrderStep -> Answer.Order(step.lines) to Answer.Order(step.lines.reversed())
                is CompilesStep -> Answer.Compiles(step.compiles) to Answer.Compiles(!step.compiles)
                is BugStep -> {
                    assertTrue(step.key, step.bugLine in 1..step.code.lines().size)
                    Answer.Line(step.bugLine) to Answer.Line(if (step.bugLine == 1) 2 else 1)
                }
            }
            assertTrue("${step.key} correct answer rejected", Grader.isCorrect(step, right))
            if (step is OrderStep && step.lines.map { it.trim() } == step.lines.reversed().map { it.trim() }) continue
            assertFalse("${step.key} wrong answer accepted", Grader.isCorrect(step, wrong))
            assertTrue(step.key, step.explanation.isNotBlank())
        }
    }

    @Test
    fun choiceOptionsAreDistinct() {
        for (s in course.lessons.flatMap { it.steps }.filterIsInstance<ChoiceStep>()) {
            val all = s.wrong + s.answer
            assertEquals(s.key, all.size, all.toSet().size)
        }
    }
}
