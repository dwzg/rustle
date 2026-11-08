//! The object the app talks to: holds the parsed course and applies the rules.

use std::collections::{HashMap, HashSet};
use std::sync::Arc;

use crate::achievements::{Achievement, achievements, newly_unlocked};
use crate::model::*;
use crate::parser::parse_course;
use crate::progress::*;

#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum LessonState {
    Locked,
    Open,
    /// The next lesson to do.
    Current,
    Done,
}

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct LessonStatus {
    pub lesson_id: String,
    pub state: LessonState,
    /// Best star rating, 0 if not completed.
    pub stars: i32,
}

/// Everything the stats header and profile show.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct Stats {
    pub level: LevelInfo,
    pub streak: i32,
    pub best_streak: i32,
    pub xp: i32,
    pub xp_today: i32,
    pub daily_goal: i32,
    /// First-try accuracy in percent, None before the first answer.
    pub accuracy: Option<i32>,
    pub lessons_done: i32,
    pub lessons_total: i32,
    pub perfect: i32,
    pub mistakes: i32,
    pub rush_best: i32,
    pub rush_games: i32,
}

/// What a finished session earned, for the results screen.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct Reward {
    pub xp: i32,
    /// Star rating for lessons, None for practice and rush.
    pub stars: Option<i32>,
    pub first_try_correct: i32,
    pub exercises: i32,
    pub best_combo: i32,
    pub before: LevelInfo,
    pub after: LevelInfo,
    pub leveled_up: bool,
    pub streak: i32,
    pub new_achievements: Vec<Achievement>,
}

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct Completion {
    pub progress: Progress,
    pub reward: Reward,
}

#[derive(uniffi::Object)]
pub struct Engine {
    course: Course,
    steps: HashMap<String, Step>,
}

/// What one session earned, before it is turned into a [`Reward`].
struct Session {
    xp: i32,
    stars: Option<i32>,
    correct: i32,
    exercises: i32,
    best_combo: i32,
}

/// Small deterministic RNG (SplitMix64) so shuffles are reproducible in tests.
struct Rng(u64);

impl Rng {
    fn next(&mut self) -> u64 {
        self.0 = self.0.wrapping_add(0x9E37_79B9_7F4A_7C15);
        let mut z = self.0;
        z = (z ^ (z >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
        z = (z ^ (z >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
        z ^ (z >> 31)
    }

    fn shuffle<T>(&mut self, items: &mut [T]) {
        for i in (1..items.len()).rev() {
            let j = (self.next() % (i as u64 + 1)) as usize;
            items.swap(i, j);
        }
    }
}

impl Engine {
    pub fn from_course(course: Course) -> Self {
        let steps = course.steps().map(|s| (s.key.clone(), s.clone())).collect();
        Self { course, steps }
    }

    fn lesson_exercises(&self, lesson_id: &str) -> Option<i32> {
        self.course
            .lessons()
            .find(|l| l.id == lesson_id)
            .map(|l| l.exercise_count() as i32)
    }

    fn unlocked(&self, p: &Progress, index: usize) -> bool {
        let lessons: Vec<&Lesson> = self.course.lessons().collect();
        p.unlock_all
            || index == 0
            || lessons
                .get(index - 1)
                .is_some_and(|prev| p.is_completed(&prev.id))
    }

    fn finish(&self, before: Progress, after: Progress, today: i64, s: Session) -> Completion {
        let (lb, la) = (level_of(before.xp), level_of(after.xp));
        let reward = Reward {
            xp: s.xp,
            stars: s.stars,
            first_try_correct: s.correct,
            exercises: s.exercises,
            best_combo: s.best_combo,
            leveled_up: la.level > lb.level,
            before: lb,
            after: la,
            streak: after.current_streak(today),
            new_achievements: newly_unlocked(&before, &after, &self.course),
        };
        Completion {
            progress: after,
            reward,
        }
    }
}

#[uniffi::export]
impl Engine {
    #[uniffi::constructor]
    pub fn new(files: Vec<CourseFile>) -> Result<Arc<Self>, CourseError> {
        Ok(Arc::new(Self::from_course(parse_course(&files)?)))
    }

    pub fn course(&self) -> Course {
        self.course.clone()
    }

    pub fn lesson_statuses(&self, progress: &Progress) -> Vec<LessonStatus> {
        let next = self.next_lesson_id(progress);
        self.course
            .lessons()
            .enumerate()
            .map(|(i, l)| LessonStatus {
                lesson_id: l.id.clone(),
                state: if progress.is_completed(&l.id) {
                    LessonState::Done
                } else if !self.unlocked(progress, i) {
                    LessonState::Locked
                } else if next.as_deref() == Some(l.id.as_str()) {
                    LessonState::Current
                } else {
                    LessonState::Open
                },
                stars: progress.stars.get(&l.id).copied().unwrap_or(0),
            })
            .collect()
    }

    /// The first lesson not completed yet, or None when the course is done.
    pub fn next_lesson_id(&self, progress: &Progress) -> Option<String> {
        self.course
            .lessons()
            .find(|l| !progress.is_completed(&l.id))
            .map(|l| l.id.clone())
    }

    pub fn stats(&self, progress: &Progress, today: i64) -> Stats {
        let total = self.course.lessons().count() as i32;
        Stats {
            level: level_of(progress.xp),
            streak: progress.current_streak(today),
            best_streak: progress.best_streak,
            xp: progress.xp,
            xp_today: progress.xp_today_on(today),
            daily_goal: DAILY_GOAL,
            accuracy: (progress.answered > 0)
                .then(|| progress.answered_correct * 100 / progress.answered),
            lessons_done: progress.stars.len() as i32,
            lessons_total: total,
            perfect: progress.perfect_count() as i32,
            mistakes: progress.mistakes.len() as i32,
            rush_best: progress.rush_best,
            rush_games: progress.rush_games,
        }
    }

    pub fn achievements(&self, progress: &Progress) -> Vec<Achievement> {
        achievements(progress, &self.course)
    }

    pub fn can_practice(&self, progress: &Progress) -> bool {
        self.course.lessons().any(|l| progress.is_completed(&l.id))
    }

    /// Up to `size` exercises: past mistakes first, then a mix from completed lessons.
    pub fn practice_set(&self, progress: &Progress, size: u32, seed: i64) -> Vec<Step> {
        let mut rng = Rng(seed as u64);
        let mut mistakes: Vec<Step> = progress
            .mistakes
            .iter()
            .filter_map(|k| self.steps.get(k))
            .filter(|s| s.is_exercise())
            .cloned()
            .collect();
        rng.shuffle(&mut mistakes);
        let known: HashSet<&String> = progress.mistakes.iter().collect();
        let mut pool: Vec<Step> = self
            .course
            .lessons()
            .filter(|l| progress.is_completed(&l.id))
            .flat_map(|l| l.steps.iter())
            .filter(|s| s.is_exercise() && !known.contains(&s.key))
            .cloned()
            .collect();
        rng.shuffle(&mut pool);
        mistakes
            .into_iter()
            .chain(pool)
            .take(size as usize)
            .collect()
    }

    /// "Will it compile?" cards from lessons the learner has reached (at least 12).
    pub fn rush_deck(&self, progress: &Progress, seed: i64) -> Vec<Step> {
        let reached: HashSet<&str> = self
            .course
            .lessons()
            .enumerate()
            .filter(|(i, l)| progress.is_completed(&l.id) || self.unlocked(progress, *i))
            .map(|(_, l)| l.id.as_str())
            .collect();
        let cards: Vec<&Step> = self
            .course
            .steps()
            .filter(|s| matches!(s.content, StepContent::Compiles { .. }))
            .collect();
        let from_reached: Vec<&Step> = cards
            .iter()
            .copied()
            .filter(|s| reached.contains(s.key.split('#').next().unwrap_or("")))
            .collect();
        let mut deck: Vec<Step> = if from_reached.len() >= 12 {
            from_reached.into_iter().cloned().collect()
        } else {
            cards
                .into_iter()
                .take(12.max(from_reached.len()))
                .cloned()
                .collect()
        };
        Rng(seed as u64).shuffle(&mut deck);
        deck
    }

    pub fn complete_lesson(
        &self,
        progress: Progress,
        lesson_id: String,
        first_try: HashMap<String, bool>,
        best_combo: i32,
        today: i64,
    ) -> Completion {
        let exercises = self
            .lesson_exercises(&lesson_id)
            .unwrap_or(first_try.len() as i32);
        let correct = first_try.values().filter(|&&ok| ok).count() as i32;
        let stars = stars_for(correct, exercises);
        let xp = lesson_xp(correct, exercises);
        let after = progress
            .clone()
            .with_answers(&first_try, false)
            .with_lesson_stars(&lesson_id, stars)
            .with_activity(today, xp);
        let session = Session {
            xp,
            stars: Some(stars),
            correct,
            exercises,
            best_combo,
        };
        self.finish(progress, after, today, session)
    }

    pub fn complete_practice(
        &self,
        progress: Progress,
        first_try: HashMap<String, bool>,
        best_combo: i32,
        today: i64,
    ) -> Completion {
        let correct = first_try.values().filter(|&&ok| ok).count() as i32;
        let xp = 5 + correct;
        let mut after = progress.clone().with_answers(&first_try, true);
        after.practice_sessions += 1;
        let after = after.with_activity(today, xp);
        let exercises = first_try.len() as i32;
        let session = Session {
            xp,
            stars: None,
            correct,
            exercises,
            best_combo,
        };
        self.finish(progress, after, today, session)
    }

    pub fn complete_rush(
        &self,
        progress: Progress,
        score: i32,
        correct: i32,
        answered: i32,
        best_combo: i32,
        today: i64,
    ) -> Completion {
        let mut after = progress.clone();
        after.rush_best = after.rush_best.max(score);
        after.rush_games += 1;
        if correct > 0 {
            after = after.with_activity(today, correct);
        }
        let session = Session {
            xp: correct,
            stars: None,
            correct,
            exercises: answered,
            best_combo,
        };
        self.finish(progress, after, today, session)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::parser::parse_unit;

    fn engine() -> Engine {
        let mut text = String::from("@unit u\ntitle: U\ntagline: t\nicon: x\n");
        for l in 0..3 {
            text += &format!("@lesson l{l}\ntitle: L{l}\n@explain E\nbody\n");
            for c in 0..5 {
                text += &format!("@compiles\n```\nfn main() {{ {c} }}\n```\n= yes\n? y\n");
            }
        }
        Engine::from_course(Course {
            units: vec![parse_unit(&text, "u.md").unwrap()],
        })
    }

    #[test]
    fn unlocking_and_statuses() {
        let e = engine();
        let p = Progress::default();
        let s: Vec<LessonState> = e.lesson_statuses(&p).iter().map(|s| s.state).collect();
        assert_eq!(
            s,
            [
                LessonState::Current,
                LessonState::Locked,
                LessonState::Locked
            ]
        );
        let p = p.with_lesson_stars("l0", 2);
        let s = e.lesson_statuses(&p);
        assert_eq!(
            (s[0].state, s[0].stars, s[1].state),
            (LessonState::Done, 2, LessonState::Current)
        );
        let all = Progress {
            unlock_all: true,
            ..Default::default()
        };
        assert_eq!(e.lesson_statuses(&all)[2].state, LessonState::Open);
    }

    #[test]
    fn completing_a_lesson() {
        let e = engine();
        let first_try: HashMap<String, bool> =
            (1..=5).map(|i| (format!("l0#{i}"), i != 3)).collect();
        let done = e.complete_lesson(Progress::default(), "l0".into(), first_try, 2, 500);
        assert_eq!(done.reward.stars, Some(2));
        assert_eq!(done.reward.xp, 14);
        assert_eq!(done.progress.mistakes, vec!["l0#3"]);
        assert_eq!(done.progress.streak, 1);
        assert!(done.reward.new_achievements.iter().any(|a| a.id == "first"));
        assert!(e.can_practice(&done.progress));

        let practice = e.practice_set(&done.progress, 10, 7);
        assert_eq!(practice[0].key, "l0#3", "mistakes come first");
        assert_eq!(practice.len(), 5);
        assert!(practice.iter().all(|s| s.is_exercise()));

        let fixed = e.complete_practice(
            done.progress,
            HashMap::from([("l0#3".into(), true)]),
            1,
            501,
        );
        assert!(fixed.progress.mistakes.is_empty());
        assert_eq!(fixed.progress.streak, 2);
    }

    #[test]
    fn rush_deck_and_stats() {
        let e = engine();
        let deck = e.rush_deck(&Progress::default(), 1);
        assert_eq!(deck.len(), 12, "padded to the minimum size");
        assert_eq!(
            e.rush_deck(&Progress::default(), 1),
            deck,
            "same seed, same deck"
        );
        let done = e.complete_rush(Progress::default(), 1200, 9, 11, 6, 10);
        assert_eq!(done.progress.rush_best, 1200);
        let stats = e.stats(&done.progress, 10);
        assert_eq!(
            (stats.xp, stats.streak, stats.lessons_total, stats.accuracy),
            (9, 1, 3, None)
        );
    }
}
