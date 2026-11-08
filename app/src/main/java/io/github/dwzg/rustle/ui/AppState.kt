package io.github.dwzg.rustle.ui

import io.github.dwzg.rustle.core.Completion
import io.github.dwzg.rustle.core.Course
import io.github.dwzg.rustle.core.Engine
import io.github.dwzg.rustle.core.Progress
import io.github.dwzg.rustle.core.Reward
import io.github.dwzg.rustle.core.Step
import io.github.dwzg.rustle.data.ProgressStore

/** Glue between the Rust [Engine] (rules) and [ProgressStore] (storage). */
class AppState(val engine: Engine, val store: ProgressStore) {
    val course: Course = engine.course()
    val progress get() = store.progress

    private fun apply(change: (Progress) -> Completion): Reward {
        lateinit var reward: Reward
        store.update { p -> change(p).also { reward = it.reward }.progress }
        return reward
    }

    fun completeLesson(lessonId: String, firstTry: Map<String, Boolean>, bestCombo: Int): Reward =
        apply { engine.completeLesson(it, lessonId, firstTry, bestCombo, store.today()) }

    fun completePractice(firstTry: Map<String, Boolean>, bestCombo: Int): Reward =
        apply { engine.completePractice(it, firstTry, bestCombo, store.today()) }

    fun completeRush(score: Int, correct: Int, answered: Int, bestCombo: Int): Reward =
        apply { engine.completeRush(it, score, correct, answered, bestCombo, store.today()) }

    fun recordPlaygroundRun() = store.update { it.copy(playgroundRuns = it.playgroundRuns + 1) }

    fun setUnlockAll(enabled: Boolean) = store.update { it.copy(unlockAll = enabled) }

    fun reset() = store.reset()

    fun practiceSet(p: Progress): List<Step> = engine.practiceSet(p, 10u, System.nanoTime())

    fun rushDeck(p: Progress): List<Step> = engine.rushDeck(p, System.nanoTime())
}
