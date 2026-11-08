package io.github.dwzg.rustle.ui

import io.github.dwzg.rustle.course.CompilesStep
import io.github.dwzg.rustle.course.Course
import io.github.dwzg.rustle.course.Exercise
import io.github.dwzg.rustle.course.Step
import io.github.dwzg.rustle.data.Achievement
import io.github.dwzg.rustle.data.Achievements
import io.github.dwzg.rustle.data.LevelInfo
import io.github.dwzg.rustle.data.Levels
import io.github.dwzg.rustle.data.Progress
import io.github.dwzg.rustle.data.ProgressStore
import kotlin.random.Random

/** What happened when a session ended, for the results screen. */
data class Reward(
    val xp: Int,
    val stars: Int?,
    val firstTryCorrect: Int,
    val exercises: Int,
    val bestCombo: Int,
    val before: LevelInfo,
    val after: LevelInfo,
    val streak: Int,
    val newAchievements: List<Achievement>,
) {
    val leveledUp get() = after.level > before.level
}

class AppState(val course: Course, val store: ProgressStore) {
    val progress get() = store.progress

    private val stepsByKey: Map<String, Step> = course.lessons.flatMap { it.steps }.associateBy { it.key }

    private fun rewarded(change: (Progress) -> Progress, build: (Progress, Progress) -> Reward): Reward {
        val before = store.progress.value
        store.update(change)
        return build(before, store.progress.value)
    }

    private fun newAchievements(before: Progress, after: Progress): List<Achievement> {
        val had = Achievements.unlocked(before, course)
        val now = Achievements.unlocked(after, course)
        return Achievements.all.filter { it.id in now && it.id !in had }
    }

    fun completeLesson(lessonId: String, firstTry: Map<String, Boolean>, bestCombo: Int): Reward {
        val exercises = course.lesson(lessonId)?.exerciseCount ?: firstTry.size
        val correct = firstTry.values.count { it }
        val stars = Progress.starsFor(correct, exercises)
        val xp = Progress.lessonXp(correct, exercises)
        val today = store.today()
        return rewarded(
            change = {
                it.withAnswers(firstTry, clearFixed = false)
                    .withLessonStars(lessonId, stars)
                    .withActivity(today, xp)
            },
            build = { b, a ->
                Reward(xp, stars, correct, exercises, bestCombo, Levels.of(b.xp), Levels.of(a.xp), a.streak, newAchievements(b, a))
            },
        )
    }

    fun completePractice(firstTry: Map<String, Boolean>, bestCombo: Int): Reward {
        val correct = firstTry.values.count { it }
        val xp = 5 + correct
        val today = store.today()
        return rewarded(
            change = {
                it.withAnswers(firstTry, clearFixed = true)
                    .copy(practiceSessions = it.practiceSessions + 1)
                    .withActivity(today, xp)
            },
            build = { b, a ->
                Reward(xp, null, correct, firstTry.size, bestCombo, Levels.of(b.xp), Levels.of(a.xp), a.streak, newAchievements(b, a))
            },
        )
    }

    fun completeRush(score: Int, correct: Int, answered: Int, bestCombo: Int): Reward {
        val xp = correct
        val today = store.today()
        return rewarded(
            change = {
                it.copy(rushBest = maxOf(it.rushBest, score), rushGames = it.rushGames + 1)
                    .let { p -> if (xp > 0) p.withActivity(today, xp) else p }
            },
            build = { b, a ->
                Reward(xp, null, correct, answered, bestCombo, Levels.of(b.xp), Levels.of(a.xp), a.currentStreak(today), newAchievements(b, a))
            },
        )
    }

    fun recordPlaygroundRun() = store.update { it.copy(playgroundRuns = it.playgroundRuns + 1) }

    fun setUnlockAll(enabled: Boolean) = store.update { it.copy(unlockAll = enabled) }

    fun reset() = store.reset()

    /** Lessons whose content the learner has seen (completed, or unlocked by explorer mode). */
    private fun seenLessons(p: Progress) = course.lessons.filter { p.isCompleted(it.id) }

    fun canPractice(p: Progress) = seenLessons(p).isNotEmpty()

    /** Up to [size] exercises: past mistakes first, then a random mix from completed lessons. */
    fun practiceSet(p: Progress, size: Int = 10, random: Random = Random.Default): List<Exercise> {
        val mistakes = p.mistakes.mapNotNull { stepsByKey[it] as? Exercise }.shuffled(random)
        val pool = seenLessons(p).flatMap { it.steps }.filterIsInstance<Exercise>()
            .filter { it.key !in p.mistakes }
            .shuffled(random)
        return (mistakes + pool).take(size)
    }

    /** Cards for Compiler Rush: from lessons the learner has reached, with a sensible minimum. */
    fun rushDeck(p: Progress, random: Random = Random.Default): List<CompilesStep> {
        val reached = course.lessons.filter { p.isUnlocked(course, it.id) || p.isCompleted(it.id) }.map { it.id }.toSet()
        val fromReached = course.compileCards.filter { it.key.substringBefore('#') in reached }
        val deck = if (fromReached.size >= 12) fromReached else course.compileCards.take(maxOf(12, fromReached.size))
        return deck.shuffled(random)
    }
}
