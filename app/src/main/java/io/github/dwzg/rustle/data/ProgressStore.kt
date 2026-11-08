package io.github.dwzg.rustle.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** Persists [Progress] in SharedPreferences and exposes it as a flow. */
class ProgressStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rustle_progress", Context.MODE_PRIVATE)

    private val _progress = MutableStateFlow(read())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    fun today(): Long = LocalDate.now().toEpochDay()

    @Synchronized
    fun update(transform: (Progress) -> Progress) {
        val next = transform(_progress.value)
        write(next)
        _progress.value = next
    }

    fun reset() = update { Progress() }

    var playgroundCode: String?
        get() = prefs.getString(K_CODE, null)
        set(value) = prefs.edit { putString(K_CODE, value) }

    private fun read() = Progress(
        xp = prefs.getInt(K_XP, 0),
        stars = prefs.getStringSet(K_STARS, emptySet()).orEmpty().mapNotNull {
            val id = it.substringBeforeLast(':')
            val s = it.substringAfterLast(':').toIntOrNull() ?: return@mapNotNull null
            id to s
        }.toMap(),
        streak = prefs.getInt(K_STREAK, 0),
        bestStreak = prefs.getInt(K_BEST_STREAK, 0),
        lastActiveDay = prefs.getLong(K_LAST_DAY, -1),
        xpToday = prefs.getInt(K_XP_TODAY, 0),
        rushBest = prefs.getInt(K_RUSH_BEST, 0),
        rushGames = prefs.getInt(K_RUSH_GAMES, 0),
        playgroundRuns = prefs.getInt(K_RUNS, 0),
        practiceSessions = prefs.getInt(K_PRACTICE, 0),
        mistakes = prefs.getStringSet(K_MISTAKES, emptySet()).orEmpty().toSet(),
        answered = prefs.getInt(K_ANSWERED, 0),
        answeredCorrect = prefs.getInt(K_CORRECT, 0),
        unlockAll = prefs.getBoolean(K_UNLOCK_ALL, false),
    )

    private fun write(p: Progress) = prefs.edit {
        putInt(K_XP, p.xp)
        putStringSet(K_STARS, p.stars.map { (id, s) -> "$id:$s" }.toSet())
        putInt(K_STREAK, p.streak)
        putInt(K_BEST_STREAK, p.bestStreak)
        putLong(K_LAST_DAY, p.lastActiveDay)
        putInt(K_XP_TODAY, p.xpToday)
        putInt(K_RUSH_BEST, p.rushBest)
        putInt(K_RUSH_GAMES, p.rushGames)
        putInt(K_RUNS, p.playgroundRuns)
        putInt(K_PRACTICE, p.practiceSessions)
        putStringSet(K_MISTAKES, p.mistakes)
        putInt(K_ANSWERED, p.answered)
        putInt(K_CORRECT, p.answeredCorrect)
        putBoolean(K_UNLOCK_ALL, p.unlockAll)
    }

    private companion object {
        const val K_XP = "xp"
        const val K_STARS = "stars"
        const val K_STREAK = "streak"
        const val K_BEST_STREAK = "best_streak"
        const val K_LAST_DAY = "last_day"
        const val K_XP_TODAY = "xp_today"
        const val K_RUSH_BEST = "rush_best"
        const val K_RUSH_GAMES = "rush_games"
        const val K_RUNS = "playground_runs"
        const val K_PRACTICE = "practice_sessions"
        const val K_MISTAKES = "mistakes"
        const val K_ANSWERED = "answered"
        const val K_CORRECT = "answered_correct"
        const val K_UNLOCK_ALL = "unlock_all"
        const val K_CODE = "playground_code"
    }
}
