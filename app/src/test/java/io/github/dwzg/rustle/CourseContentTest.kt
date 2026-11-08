package io.github.dwzg.rustle

import io.github.dwzg.rustle.core.Answer
import io.github.dwzg.rustle.core.Course
import io.github.dwzg.rustle.core.CourseFile
import io.github.dwzg.rustle.core.Engine
import io.github.dwzg.rustle.core.Step
import io.github.dwzg.rustle.core.StepContent
import io.github.dwzg.rustle.core.isCorrect
import io.github.dwzg.rustle.course.exerciseCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Loads the real course through the Rust core (via the generated bindings and
 * the host build of the native library) and checks it end to end.
 */
class CourseContentTest {
    companion object {
        fun loadEngine(): Engine {
            val files = File("src/main/assets/course").listFiles { f -> f.name.endsWith(".md") }!!
            return Engine(files.map { CourseFile(it.name, it.readText()) })
        }

        val Course.lessons get() = units.flatMap { it.lessons }
        val Course.steps get() = lessons.flatMap { it.steps }

        fun correctAnswer(step: Step): Answer? = when (val c = step.content) {
            is StepContent.Explain -> null
            is StepContent.Choice -> Answer.Choice(c.answer)
            is StepContent.Fill -> Answer.Blanks(c.answers)
            is StepContent.Order -> Answer.Order(c.lines)
            is StepContent.Compiles -> Answer.Compiles(c.compiles)
            is StepContent.Bug -> Answer.Line(c.bugLine)
        }
    }

    private val course = loadEngine().course()

    @Test
    fun courseHasAllUnitsInOrder() {
        assertEquals(13, course.units.size)
        assertEquals("hello-rust", course.units.first().id)
        assertTrue(course.lessons.size >= 39)
        assertTrue("Rush needs plenty of cards", course.steps.count { it.content is StepContent.Compiles } >= 60)
        assertTrue(course.lessons.all { it.exerciseCount >= 3 })
    }

    @Test
    fun keysAreUnique() {
        val keys = course.steps.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun gradingAcrossTheBridge() {
        for (step in course.steps) {
            val right = correctAnswer(step) ?: continue
            assertTrue("${step.key} correct answer rejected", isCorrect(step, right))
            val wrong = when (val c = step.content) {
                is StepContent.Choice -> Answer.Choice(c.wrong.first())
                is StepContent.Fill -> Answer.Blanks(listOf(c.distractors.first()) + c.answers.drop(1))
                is StepContent.Compiles -> Answer.Compiles(!c.compiles)
                is StepContent.Bug -> Answer.Line(if (c.bugLine == 1) 2 else 1)
                else -> continue
            }
            assertFalse("${step.key} wrong answer accepted", isCorrect(step, wrong))
        }
    }
}
