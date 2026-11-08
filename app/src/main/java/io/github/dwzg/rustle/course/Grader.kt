package io.github.dwzg.rustle.course

/** What the learner submitted for an exercise. */
sealed interface Answer {
    data class Choice(val option: String) : Answer
    data class Blanks(val tokens: List<String>) : Answer
    data class Order(val lines: List<String>) : Answer
    data class Compiles(val compiles: Boolean) : Answer
    data class Line(val line: Int) : Answer
}

object Grader {
    fun isCorrect(step: Exercise, answer: Answer): Boolean = when (step) {
        is ChoiceStep -> answer is Answer.Choice && answer.option == step.answer
        is FillStep -> answer is Answer.Blanks && answer.tokens == step.answers
        // Compare by trimmed text so identical lines (like "}") are interchangeable.
        is OrderStep -> answer is Answer.Order && answer.lines.map { it.trim() } == step.lines.map { it.trim() }
        is CompilesStep -> answer is Answer.Compiles && answer.compiles == step.compiles
        is BugStep -> answer is Answer.Line && answer.line == step.bugLine
    }

    /** Human-readable correct answer, shown when the learner gets it wrong. */
    fun solution(step: Exercise): String = when (step) {
        is ChoiceStep -> step.answer
        is FillStep -> step.answers.joinToString("  ·  ")
        is OrderStep -> step.lines.joinToString("\n")
        is CompilesStep -> if (step.compiles) "It compiles ✅" else "Compile error ❌"
        is BugStep -> "Line ${step.bugLine}: ${step.code.lines()[step.bugLine - 1].trim()}"
    }
}
