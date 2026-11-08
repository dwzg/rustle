package io.github.dwzg.rustle.ui.lesson

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.dwzg.rustle.core.Answer
import io.github.dwzg.rustle.core.Step
import io.github.dwzg.rustle.core.isCorrect
import io.github.dwzg.rustle.course.isExercise

data class Feedback(val correct: Boolean, val step: Step, val answer: Answer)

/**
 * Drives one run through a list of steps. Exercises answered wrongly are queued
 * again at the end (once), so every lesson ends on a correct answer.
 */
class LessonSession(steps: List<Step>) : ViewModel() {
    val queue = mutableStateListOf<Step>().apply { addAll(steps) }
    var index by mutableIntStateOf(0)
        private set
    var feedback by mutableStateOf<Feedback?>(null)
        private set
    var combo by mutableIntStateOf(0)
        private set
    var bestCombo by mutableIntStateOf(0)
        private set
    var finished by mutableStateOf(false)
        private set

    /** First-attempt result per exercise key. */
    val firstTry = linkedMapOf<String, Boolean>()
    private val requeued = mutableSetOf<String>()

    val current: Step? get() = queue.getOrNull(index)

    val progress: Float
        get() = if (queue.isEmpty()) 1f else (index + if (feedback != null) 1 else 0).toFloat() / queue.size

    fun check(answer: Answer): Boolean {
        val step = current?.takeIf { it.isExercise } ?: return false
        feedback?.let { return it.correct }
        val ok = isCorrect(step, answer)
        firstTry.putIfAbsent(step.key, ok)
        if (ok) {
            combo++
            bestCombo = maxOf(bestCombo, combo)
        } else {
            combo = 0
            if (requeued.add(step.key)) queue.add(step)
        }
        feedback = Feedback(ok, step, answer)
        return ok
    }

    fun next() {
        feedback = null
        if (index + 1 >= queue.size) finished = true else index++
    }
}
