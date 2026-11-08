package io.github.dwzg.rustle.data

import io.github.dwzg.rustle.course.Course

data class LevelInfo(
    val level: Int,
    val title: String,
    /** XP earned inside the current level. */
    val into: Int,
    /** XP needed to go from this level to the next. */
    val span: Int,
) {
    val fraction: Float get() = into.toFloat() / span
}

object Levels {
    private val titles = listOf(
        "Hatchling",
        "Shell Polisher",
        "Semicolon Wrangler",
        "Borrow Checker Apprentice",
        "Ownership Ace",
        "Pattern Matcher",
        "Trait Tamer",
        "Lifetime Whisperer",
        "Fearless Concurrent",
        "Senior Rustacean",
    )

    /** XP needed to go from [level] to level + 1. */
    fun span(level: Int) = 50 + 20 * (level - 1)

    fun of(xp: Int): LevelInfo {
        var level = 1
        var remaining = xp
        while (remaining >= span(level)) {
            remaining -= span(level)
            level++
        }
        return LevelInfo(level, titleFor(level), remaining, span(level))
    }

    fun titleFor(level: Int) = titles[(level - 1).coerceIn(0, titles.lastIndex)]
}

data class Achievement(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val unlocked: (Progress, Course) -> Boolean,
)

object Achievements {
    private fun unitDone(unitId: String) = { p: Progress, c: Course ->
        c.units.firstOrNull { it.id == unitId }?.lessons?.all { p.isCompleted(it.id) } == true
    }

    val all: List<Achievement> = listOf(
        Achievement("first", "🐣", "Hello, World", "Finish your first lesson") { p, _ -> p.completedCount >= 1 },
        Achievement("perfect", "💯", "Flawless", "Finish a lesson without a single mistake") { p, _ -> p.perfectCount >= 1 },
        Achievement("perfect5", "🏅", "Perfectionist", "Get 3 stars on 5 lessons") { p, _ -> p.perfectCount >= 5 },
        Achievement("owner", "📦", "Owner", "Complete the Ownership unit", unitDone("ownership")),
        Achievement("borrow", "🤝", "Borrow Checker's Friend", "Complete the Borrowing unit", unitDone("borrowing")),
        Achievement("matcher", "🧩", "Exhaustive", "Complete Enums & Pattern Matching", unitDone("enums-match")),
        Achievement("errors", "🛟", "Question Mark Master", "Complete Error Handling", unitDone("errors")),
        Achievement("lifetimes", "⏳", "Lifetime Whisperer", "Complete the Lifetimes unit", unitDone("lifetimes")),
        Achievement("graduate", "🎓", "Rustacean", "Complete the whole course") { p, c ->
            c.lessons.isNotEmpty() && c.lessons.all { p.isCompleted(it.id) }
        },
        Achievement("streak3", "🔥", "On Fire", "Reach a 3 day streak") { p, _ -> p.bestStreak >= 3 },
        Achievement("streak7", "☄️", "Unstoppable", "Reach a 7 day streak") { p, _ -> p.bestStreak >= 7 },
        Achievement("rush", "⚡", "Speed Compiler", "Score 1000+ in Compiler Rush") { p, _ -> p.rushBest >= 1000 },
        Achievement("rush3k", "🚀", "Zero-Cost Reflexes", "Score 3000+ in Compiler Rush") { p, _ -> p.rushBest >= 3000 },
        Achievement("play", "🛝", "Playground Kid", "Run code in the Playground") { p, _ -> p.playgroundRuns >= 1 },
        Achievement("practice", "🏋️", "Gym Crab", "Finish 5 practice sessions") { p, _ -> p.practiceSessions >= 5 },
        Achievement("xp500", "💎", "Crustacean Hoard", "Earn 500 XP") { p, _ -> p.xp >= 500 },
    )

    fun unlocked(p: Progress, c: Course): Set<String> = all.filter { it.unlocked(p, c) }.map { it.id }.toSet()
}
