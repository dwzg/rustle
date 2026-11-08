package io.github.dwzg.rustle.course

data class Course(val units: List<CourseUnit>) {
    val lessons: List<Lesson> = units.flatMap { it.lessons }

    fun lesson(id: String): Lesson? = lessons.firstOrNull { it.id == id }

    fun unitOf(lessonId: String): CourseUnit? =
        units.firstOrNull { u -> u.lessons.any { it.id == lessonId } }

    /** Every "will it compile?" card in the course, used by Compiler Rush. */
    val compileCards: List<CompilesStep> = lessons.flatMap { it.steps }.filterIsInstance<CompilesStep>()
}

data class CourseUnit(
    val id: String,
    val title: String,
    val tagline: String,
    val icon: String,
    val lessons: List<Lesson>,
)

data class Lesson(
    val id: String,
    val title: String,
    val steps: List<Step>,
) {
    val exerciseCount: Int get() = steps.count { it is Exercise }
}

sealed interface Step {
    /** Stable key (lessonId#index) used to remember mistakes for practice. */
    val key: String
}

sealed interface Exercise : Step {
    val prompt: String
    val explanation: String
}

data class ExplainStep(
    override val key: String,
    val title: String,
    val body: String,
) : Step

data class ChoiceStep(
    override val key: String,
    override val prompt: String,
    val code: String?,
    val answer: String,
    val wrong: List<String>,
    override val explanation: String,
) : Exercise

data class FillStep(
    override val key: String,
    override val prompt: String,
    /** Code containing [BLANK] markers. */
    val code: String,
    val answers: List<String>,
    val distractors: List<String>,
    override val explanation: String,
) : Exercise {
    companion object {
        const val BLANK = "___"
    }
}

data class OrderStep(
    override val key: String,
    override val prompt: String,
    /** Lines in the correct order, indentation preserved. */
    val lines: List<String>,
    override val explanation: String,
) : Exercise

data class CompilesStep(
    override val key: String,
    override val prompt: String,
    val code: String,
    val compiles: Boolean,
    override val explanation: String,
) : Exercise

data class BugStep(
    override val key: String,
    override val prompt: String,
    val code: String,
    /** 1-based line number inside [code]. */
    val bugLine: Int,
    override val explanation: String,
) : Exercise
