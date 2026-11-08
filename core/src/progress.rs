//! The learner's progress and the rules for XP, stars, streaks and levels.

use std::collections::HashMap;

pub const DAILY_GOAL: i32 = 30;

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct Progress {
    pub xp: i32,
    /// lessonId -> best star rating (1..=3).
    pub stars: HashMap<String, i32>,
    pub streak: i32,
    pub best_streak: i32,
    /// Epoch day of the last activity, -1 if none.
    pub last_active_day: i64,
    pub xp_today: i32,
    pub rush_best: i32,
    pub rush_games: i32,
    pub playground_runs: i32,
    pub practice_sessions: i32,
    /// Step keys answered wrongly and not yet fixed in practice.
    pub mistakes: Vec<String>,
    pub answered: i32,
    pub answered_correct: i32,
    pub unlock_all: bool,
}

impl Default for Progress {
    fn default() -> Self {
        Self {
            xp: 0,
            stars: HashMap::new(),
            streak: 0,
            best_streak: 0,
            last_active_day: -1,
            xp_today: 0,
            rush_best: 0,
            rush_games: 0,
            playground_runs: 0,
            practice_sessions: 0,
            mistakes: Vec::new(),
            answered: 0,
            answered_correct: 0,
            unlock_all: false,
        }
    }
}

impl Progress {
    pub fn is_completed(&self, lesson_id: &str) -> bool {
        self.stars.contains_key(lesson_id)
    }

    pub fn perfect_count(&self) -> usize {
        self.stars.values().filter(|&&s| s == 3).count()
    }

    /// The streak as shown on `today`: a missed day means it's gone.
    pub fn current_streak(&self, today: i64) -> i32 {
        if self.last_active_day >= today - 1 {
            self.streak
        } else {
            0
        }
    }

    pub fn xp_today_on(&self, today: i64) -> i32 {
        if self.last_active_day == today {
            self.xp_today
        } else {
            0
        }
    }

    /// Records activity on `today` and earned XP, updating the streak.
    pub fn with_activity(mut self, today: i64, earned_xp: i32) -> Self {
        let streak = if self.last_active_day == today {
            self.streak.max(1)
        } else if self.last_active_day == today - 1 {
            self.streak + 1
        } else {
            1
        };
        self.xp_today = self.xp_today_on(today) + earned_xp;
        self.xp += earned_xp;
        self.streak = streak;
        self.best_streak = self.best_streak.max(streak);
        self.last_active_day = today;
        self
    }

    /// Records first-try results. Wrong answers are remembered for practice;
    /// with `clear_fixed`, correctly answered ones are forgotten again.
    pub fn with_answers(mut self, results: &HashMap<String, bool>, clear_fixed: bool) -> Self {
        let mut keys: Vec<&String> = results.keys().collect();
        keys.sort();
        for key in keys {
            let right = results[key];
            self.answered += 1;
            if right {
                self.answered_correct += 1;
                if clear_fixed {
                    self.mistakes.retain(|m| m != key);
                }
            } else if !self.mistakes.contains(key) {
                self.mistakes.push(key.clone());
            }
        }
        self
    }

    pub fn with_lesson_stars(mut self, lesson_id: &str, earned: i32) -> Self {
        let best = self.stars.get(lesson_id).copied().unwrap_or(0).max(earned);
        self.stars.insert(lesson_id.to_string(), best);
        self
    }
}

pub fn stars_for(first_try_correct: i32, exercises: i32) -> i32 {
    if exercises == 0 || first_try_correct >= exercises {
        3
    } else if first_try_correct * 10 >= exercises * 7 {
        2
    } else {
        1
    }
}

pub fn lesson_xp(first_try_correct: i32, exercises: i32) -> i32 {
    10 + first_try_correct + if first_try_correct == exercises { 5 } else { 0 }
}

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct LevelInfo {
    pub level: i32,
    pub title: String,
    /// XP earned inside the current level.
    pub into: i32,
    /// XP needed to go from this level to the next.
    pub span: i32,
}

const LEVEL_TITLES: [&str; 10] = [
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
];

fn level_span(level: i32) -> i32 {
    50 + 20 * (level - 1)
}

#[uniffi::export]
pub fn level_of(xp: i32) -> LevelInfo {
    let mut level = 1;
    let mut remaining = xp.max(0);
    while remaining >= level_span(level) {
        remaining -= level_span(level);
        level += 1;
    }
    let title = LEVEL_TITLES[(level as usize - 1).min(LEVEL_TITLES.len() - 1)];
    LevelInfo {
        level,
        title: title.to_string(),
        into: remaining,
        span: level_span(level),
    }
}

#[uniffi::export]
pub fn new_progress() -> Progress {
    Progress::default()
}

#[uniffi::export]
pub fn daily_goal() -> i32 {
    DAILY_GOAL
}

/// Compiler Rush: score multiplier for the current streak (x1..x4).
#[uniffi::export]
pub fn rush_multiplier(combo: i32) -> i32 {
    (1 + combo / 3).min(4)
}

#[uniffi::export]
pub fn rush_points(combo: i32) -> i32 {
    100 * rush_multiplier(combo)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn streaks() {
        let p = Progress::default().with_activity(100, 10);
        assert_eq!(p.streak, 1);
        let p = p.with_activity(100, 5);
        assert_eq!((p.streak, p.xp_today, p.xp), (1, 15, 15));
        let p = p.with_activity(101, 5);
        assert_eq!((p.streak, p.xp_today), (2, 5));
        assert_eq!(p.current_streak(102), 2);
        assert_eq!(p.current_streak(103), 0);
        assert_eq!(p.xp_today_on(102), 0);
        let p = p.with_activity(105, 1);
        assert_eq!((p.streak, p.best_streak), (1, 2));
    }

    #[test]
    fn mistakes() {
        let r = HashMap::from([("a".to_string(), false), ("b".to_string(), true)]);
        let p = Progress::default().with_answers(&r, false);
        assert_eq!(p.mistakes, vec!["a"]);
        let p = p.with_answers(&HashMap::from([("a".to_string(), true)]), true);
        assert!(p.mistakes.is_empty());
        assert_eq!((p.answered, p.answered_correct), (3, 2));
    }

    #[test]
    fn stars_xp_levels() {
        assert_eq!(stars_for(7, 7), 3);
        assert_eq!(stars_for(5, 7), 2);
        assert_eq!(stars_for(2, 7), 1);
        assert_eq!(lesson_xp(7, 7), 22);
        assert_eq!(lesson_xp(5, 7), 15);
        let p = Progress::default()
            .with_lesson_stars("l", 2)
            .with_lesson_stars("l", 1);
        assert_eq!(p.stars["l"], 2);
        assert_eq!(level_of(0).level, 1);
        assert_eq!(level_of(49).level, 1);
        assert_eq!(level_of(50).level, 2);
        assert_eq!((level_of(120).level, level_of(120).into), (3, 0));
        assert_eq!(level_of(100_000).title, "Senior Rustacean");
        assert_eq!(
            (rush_multiplier(0), rush_multiplier(3), rush_multiplier(99)),
            (1, 2, 4)
        );
    }
}
