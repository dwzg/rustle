//! Achievements and the rules that unlock them.

use crate::model::Course;
use crate::progress::Progress;

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct Achievement {
    pub id: String,
    pub icon: String,
    pub title: String,
    pub description: String,
    pub unlocked: bool,
}

enum Rule {
    Completed(usize),
    Perfect(usize),
    UnitDone(&'static str),
    CourseDone,
    BestStreak(i32),
    RushBest(i32),
    PlaygroundRuns(i32),
    PracticeSessions(i32),
    Xp(i32),
}

struct Def {
    id: &'static str,
    icon: &'static str,
    title: &'static str,
    description: &'static str,
    rule: Rule,
}

const DEFS: &[Def] = &[
    Def {
        id: "first",
        icon: "🐣",
        title: "Hello, World",
        description: "Finish your first lesson",
        rule: Rule::Completed(1),
    },
    Def {
        id: "perfect",
        icon: "💯",
        title: "Flawless",
        description: "Finish a lesson without a single mistake",
        rule: Rule::Perfect(1),
    },
    Def {
        id: "perfect5",
        icon: "🏅",
        title: "Perfectionist",
        description: "Get 3 stars on 5 lessons",
        rule: Rule::Perfect(5),
    },
    Def {
        id: "owner",
        icon: "📦",
        title: "Owner",
        description: "Complete the Ownership unit",
        rule: Rule::UnitDone("ownership"),
    },
    Def {
        id: "borrow",
        icon: "🤝",
        title: "Borrow Checker's Friend",
        description: "Complete the Borrowing unit",
        rule: Rule::UnitDone("borrowing"),
    },
    Def {
        id: "matcher",
        icon: "🧩",
        title: "Exhaustive",
        description: "Complete Enums & Pattern Matching",
        rule: Rule::UnitDone("enums-match"),
    },
    Def {
        id: "errors",
        icon: "🛟",
        title: "Question Mark Master",
        description: "Complete Error Handling",
        rule: Rule::UnitDone("errors"),
    },
    Def {
        id: "lifetimes",
        icon: "⏳",
        title: "Lifetime Whisperer",
        description: "Complete the Lifetimes unit",
        rule: Rule::UnitDone("lifetimes"),
    },
    Def {
        id: "graduate",
        icon: "🎓",
        title: "Rustacean",
        description: "Complete the whole course",
        rule: Rule::CourseDone,
    },
    Def {
        id: "streak3",
        icon: "🔥",
        title: "On Fire",
        description: "Reach a 3 day streak",
        rule: Rule::BestStreak(3),
    },
    Def {
        id: "streak7",
        icon: "☄️",
        title: "Unstoppable",
        description: "Reach a 7 day streak",
        rule: Rule::BestStreak(7),
    },
    Def {
        id: "rush",
        icon: "⚡",
        title: "Speed Compiler",
        description: "Score 1000+ in Compiler Rush",
        rule: Rule::RushBest(1000),
    },
    Def {
        id: "rush3k",
        icon: "🚀",
        title: "Zero-Cost Reflexes",
        description: "Score 3000+ in Compiler Rush",
        rule: Rule::RushBest(3000),
    },
    Def {
        id: "play",
        icon: "🛝",
        title: "Playground Kid",
        description: "Run code in the Playground",
        rule: Rule::PlaygroundRuns(1),
    },
    Def {
        id: "practice",
        icon: "🏋️",
        title: "Gym Crab",
        description: "Finish 5 practice sessions",
        rule: Rule::PracticeSessions(5),
    },
    Def {
        id: "xp500",
        icon: "💎",
        title: "Crustacean Hoard",
        description: "Earn 500 XP",
        rule: Rule::Xp(500),
    },
];

fn unlocked(rule: &Rule, p: &Progress, c: &Course) -> bool {
    match *rule {
        Rule::Completed(n) => p.stars.len() >= n,
        Rule::Perfect(n) => p.perfect_count() >= n,
        Rule::UnitDone(id) => c
            .units
            .iter()
            .find(|u| u.id == id)
            .is_some_and(|u| u.lessons.iter().all(|l| p.is_completed(&l.id))),
        Rule::CourseDone => {
            c.lessons().next().is_some() && c.lessons().all(|l| p.is_completed(&l.id))
        }
        Rule::BestStreak(n) => p.best_streak >= n,
        Rule::RushBest(n) => p.rush_best >= n,
        Rule::PlaygroundRuns(n) => p.playground_runs >= n,
        Rule::PracticeSessions(n) => p.practice_sessions >= n,
        Rule::Xp(n) => p.xp >= n,
    }
}

/// Every achievement with its current unlock state.
pub fn achievements(p: &Progress, c: &Course) -> Vec<Achievement> {
    DEFS.iter()
        .map(|d| Achievement {
            id: d.id.into(),
            icon: d.icon.into(),
            title: d.title.into(),
            description: d.description.into(),
            unlocked: unlocked(&d.rule, p, c),
        })
        .collect()
}

/// Achievements unlocked by going from `before` to `after`.
pub fn newly_unlocked(before: &Progress, after: &Progress, c: &Course) -> Vec<Achievement> {
    let had = achievements(before, c);
    achievements(after, c)
        .into_iter()
        .zip(had)
        .filter(|(now, was)| now.unlocked && !was.unlocked)
        .map(|(a, _)| a)
        .collect()
}
