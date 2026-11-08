package io.github.dwzg.rustle

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.dwzg.rustle.CourseContentTest.Companion.correctAnswer
import io.github.dwzg.rustle.CourseContentTest.Companion.lessons
import io.github.dwzg.rustle.core.Answer
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

/** UI-side logic. Parsing, grading and progress rules are tested in Rust (core/). */
class LogicTest {
    @Test
    fun wrongAnswersAreRepeatedOnce() {
        val steps = CourseContentTest.loadEngine().course().lessons.first().steps
        val s = LessonSession(steps)
        val firstExercise = steps.indexOfFirst { correctAnswer(it) != null }
        repeat(firstExercise) { s.next() }
        val missed = s.current!!
        assertFalse(s.check(Answer.Line(-1)))
        assertEquals(steps.size + 1, s.queue.size)
        s.next()
        while (!s.finished) {
            correctAnswer(s.current!!)?.let { assertTrue(s.check(it)) }
            s.next()
        }
        assertEquals(false, s.firstTry[missed.key])
        assertTrue(s.firstTry.values.count { it } == s.firstTry.size - 1)
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
