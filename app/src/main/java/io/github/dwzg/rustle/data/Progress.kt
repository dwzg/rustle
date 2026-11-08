package io.github.dwzg.rustle.data

import io.github.dwzg.rustle.course.Course

data class Progress(
    val xp: Int = 0,
    /** lessonId -> best star rating (1..3). */
    val stars: Map<String, Int> = emptyMap(),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDay: Long = -1,
    val xpToday: Int = 0,
    val rushBest: Int = 0,
    val rushGames: Int = 0,
    val playgroundRuns: Int = 0,
    val practiceSessions: Int = 0,
    /** Step keys the learner got wrong and hasn't fixed in practice yet. */
    val mistakes: Set<String> = emptySet(),
    val answered: Int = 0,
    val answeredCorrect: Int = 0,
    val unlockAll: Boolean = false,
) {
    val completedCount: Int get() = stars.size
    val perfectCount: Int get() = stars.values.count { it == 3 }

    fun isCompleted(lessonId: String) = lessonId in stars

    /** Streak as it should be displayed today (a missed day means it's gone). */
    fun currentStreak(today: Long): Int = if (lastActiveDay >= today - 1) streak else 0

    fun xpTodayOn(today: Long): Int = if (lastActiveDay == today) xpToday else 0

    /** A lesson is unlocked when it's the first one, or the previous lesson is done. */
    fun isUnlocked(course: Course, lessonId: String): Boolean {
        if (unlockAll) return true
        val idx = course.lessons.indexOfFirst { it.id == lessonId }
        return idx == 0 || (idx > 0 && isCompleted(course.lessons[idx - 1].id))
    }

    /** The lesson the learner should do next, or null when the course is finished. */
    fun nextLesson(course: Course) = course.lessons.firstOrNull { !isCompleted(it.id) }

    /** Records activity on [today] (epoch day) and earned XP, updating the streak. */
    fun withActivity(today: Long, earnedXp: Int): Progress {
        val newStreak = when (lastActiveDay) {
            today -> streak.coerceAtLeast(1)
            today - 1 -> streak + 1
            else -> 1
        }
        return copy(
            xp = xp + earnedXp,
            streak = newStreak,
            bestStreak = maxOf(bestStreak, newStreak),
            xpToday = (if (lastActiveDay == today) xpToday else 0) + earnedXp,
            lastActiveDay = today,
        )
    }

    /**
     * Records first-try results (step key -> correct). Wrong answers are remembered
     * for practice; with [clearFixed], correctly answered ones are forgotten again.
     */
    fun withAnswers(results: Map<String, Boolean>, clearFixed: Boolean): Progress {
        val wrong = results.filterValues { !it }.keys
        val right = results.filterValues { it }.keys
        return copy(
            answered = answered + results.size,
            answeredCorrect = answeredCorrect + right.size,
            mistakes = (mistakes + wrong).let { if (clearFixed) it - right else it },
        )
    }

    fun withLessonStars(lessonId: String, earned: Int): Progress =
        copy(stars = stars + (lessonId to maxOf(earned, stars[lessonId] ?: 0)))

    companion object {
        const val DAILY_GOAL = 30

        fun starsFor(firstTryCorrect: Int, exercises: Int): Int {
            if (exercises == 0) return 3
            val ratio = firstTryCorrect.toDouble() / exercises
            return when {
                ratio >= 0.999 -> 3
                ratio >= 0.7 -> 2
                else -> 1
            }
        }

        fun lessonXp(firstTryCorrect: Int, exercises: Int): Int =
            10 + firstTryCorrect + if (firstTryCorrect == exercises) 5 else 0
    }
}
