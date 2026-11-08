package io.github.dwzg.rustle

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dwzg.rustle.course.CourseParser
import io.github.dwzg.rustle.course.ExplainStep
import io.github.dwzg.rustle.course.FillStep
import io.github.dwzg.rustle.course.OrderStep
import io.github.dwzg.rustle.course.Answer
import io.github.dwzg.rustle.course.Grader
import io.github.dwzg.rustle.data.Levels
import io.github.dwzg.rustle.data.Progress
import io.github.dwzg.rustle.ui.code.RustHighlighter
import io.github.dwzg.rustle.ui.code.TokenKind
import io.github.dwzg.rustle.ui.components.MdBlock
import io.github.dwzg.rustle.ui.components.splitMarkdown
import io.github.dwzg.rustle.ui.lesson.LessonSession
import io.github.dwzg.rustle.ui.playground.Editing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private val sample = """
        @unit demo
        title: Demo
        tagline: t
        icon: 🦀

        @lesson demo-one
        title: One

        @explain Hi
        Some **text**.

        ```rust
        fn main() {}
        ```

        @fill Fill it
        !compiles
        ```rust
        let ___ x = 1;
        ```
        = mut
        ~ var
        ? Because.

        @order Order it
        ```
        a
            }
        b
        ```
        ? Order.
    """.trimIndent()

    @Test
    fun parsesSample() {
        val unit = CourseParser.parseUnit(sample)
        assertEquals("demo", unit.id)
        val steps = unit.lessons.single().steps
        assertEquals(3, steps.size)
        val explain = steps[0] as ExplainStep
        assertTrue(explain.body.contains("fn main() {}"))
        val fill = steps[1] as FillStep
        assertEquals(listOf("mut"), fill.answers)
        assertEquals(listOf("var"), fill.distractors)
        assertEquals("demo-one#1", fill.key)
        val order = steps[2] as OrderStep
        assertEquals(listOf("a", "    }", "b"), order.lines)
    }

    @Test
    fun orderGradingIgnoresIndentation() {
        val order = CourseParser.parseUnit(sample).lessons.single().steps[2] as OrderStep
        assertTrue(Grader.isCorrect(order, Answer.Order(listOf("a", "}", "b"))))
        assertFalse(Grader.isCorrect(order, Answer.Order(listOf("b", "}", "a"))))
    }

    @Test
    fun wrongAnswersAreRepeatedOnce() {
        val steps = CourseParser.parseUnit(sample).lessons.single().steps
        val s = LessonSession(steps)
        s.next() // explain
        assertFalse(s.check(Answer.Blanks(listOf("var"))))
        assertEquals(4, s.queue.size)
        s.next()
        assertTrue(s.check(Answer.Order(listOf("a", "}", "b"))))
        s.next()
        assertTrue(s.check(Answer.Blanks(listOf("mut")))) // the retry
        assertEquals(false, s.firstTry["demo-one#1"])
        s.next()
        assertTrue(s.finished)
        assertEquals(2, s.bestCombo)
    }

    @Test
    fun streakRules() {
        val p0 = Progress()
        val p1 = p0.withActivity(today = 100, earnedXp = 10)
        assertEquals(1, p1.streak)
        val p2 = p1.withActivity(100, 5)
        assertEquals(1, p2.streak)
        assertEquals(15, p2.xpToday)
        val p3 = p2.withActivity(101, 5)
        assertEquals(2, p3.streak)
        assertEquals(5, p3.xpToday)
        assertEquals(2, p3.currentStreak(102))
        assertEquals(0, p3.currentStreak(103))
        val p4 = p3.withActivity(105, 1)
        assertEquals(1, p4.streak)
        assertEquals(2, p4.bestStreak)
    }

    @Test
    fun mistakesAreTrackedAndCleared() {
        val p = Progress().withAnswers(mapOf("a" to false, "b" to true), clearFixed = false)
        assertEquals(setOf("a"), p.mistakes)
        val fixed = p.withAnswers(mapOf("a" to true), clearFixed = true)
        assertTrue(fixed.mistakes.isEmpty())
        assertEquals(3, fixed.answered)
        assertEquals(2, fixed.answeredCorrect)
    }

    @Test
    fun starsAndXp() {
        assertEquals(3, Progress.starsFor(7, 7))
        assertEquals(2, Progress.starsFor(5, 7))
        assertEquals(1, Progress.starsFor(2, 7))
        assertEquals(22, Progress.lessonXp(7, 7))
        assertEquals(15, Progress.lessonXp(5, 7))
    }

    @Test
    fun levels() {
        assertEquals(1, Levels.of(0).level)
        assertEquals(1, Levels.of(49).level)
        assertEquals(2, Levels.of(50).level)
        assertEquals(3, Levels.of(120).level)
        assertEquals(0, Levels.of(120).into)
    }

    @Test
    fun highlighter() {
        val code = "let s: String = format!(\"hi {}\", 'a'); // done\nfn f<'a>(x: &'a str) {}"
        val kinds = RustHighlighter.tokenize(code).associate { code.substring(it.start, it.end) to it.kind }
        assertEquals(TokenKind.Keyword, kinds["let"])
        assertEquals(TokenKind.Type, kinds["String"])
        assertEquals(TokenKind.Macro, kinds["format!"])
        assertEquals(TokenKind.String, kinds["\"hi {}\""])
        assertEquals(TokenKind.String, kinds["'a'"])
        assertEquals(TokenKind.Comment, kinds["// done"])
        assertEquals(TokenKind.Lifetime, kinds["'a"])
        assertEquals(TokenKind.Function, kinds["f"])
    }

    @Test
    fun markdown() {
        val blocks = splitMarkdown("Hello\nworld\n\n- one\n- two\n\n```rust\nfn x() {}\n```\nBye")
        assertEquals(
            listOf(MdBlock.Para("Hello world"), MdBlock.Bullets(listOf("one", "two")), MdBlock.Code("fn x() {}"), MdBlock.Para("Bye")),
            blocks,
        )
    }

    @Test
    fun autoIndent() {
        val old = TextFieldValue("fn main() {", TextRange(11))
        val typed = TextFieldValue("fn main() {\n", TextRange(12))
        val out = Editing.autoIndent(old, typed)
        assertEquals("fn main() {\n    ", out.text)
        assertEquals(16, out.selection.start)

        val o2 = TextFieldValue("    let x = 1;", TextRange(14))
        val t2 = TextFieldValue("    let x = 1;\n", TextRange(15))
        assertEquals("    let x = 1;\n    ", Editing.autoIndent(o2, t2).text)
    }
}
