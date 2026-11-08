//! Content rules for course authors, checked by `verify-course`.

use std::collections::HashSet;

use crate::model::*;

/// Code lines longer than this get a warning (phone screens are narrow)...
pub const WARN_WIDTH: usize = 38;
/// ...and longer than this an error.
pub const MAX_WIDTH: usize = 46;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Severity {
    Error,
    Warning,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Issue {
    pub severity: Severity,
    pub file: String,
    pub line: u32,
    pub message: String,
}

fn is_id(s: &str) -> bool {
    !s.is_empty()
        && s.split('-').all(|part| {
            !part.is_empty()
                && part
                    .chars()
                    .all(|c| c.is_ascii_lowercase() || c.is_ascii_digit())
        })
}

/// Code blocks inside an explain card's markdown body.
fn fenced_blocks(body: &str) -> Vec<String> {
    let mut out = Vec::new();
    let mut current: Option<Vec<&str>> = None;
    for line in body.lines() {
        if line.starts_with("```") {
            match current.take() {
                Some(lines) => out.push(lines.join("\n")),
                None => current = Some(Vec::new()),
            }
        } else if let Some(lines) = current.as_mut() {
            lines.push(line);
        }
    }
    out
}

pub fn lint_course(course: &Course) -> Vec<Issue> {
    let mut issues = Vec::new();
    let mut ids = HashSet::new();

    for unit in &course.units {
        let file = unit.source_file.as_str();
        let mut add = |severity, line, message: String| {
            issues.push(Issue {
                severity,
                file: file.to_string(),
                line,
                message,
            })
        };
        let mut check_id = |id: &str, line, add: &mut dyn FnMut(Severity, u32, String)| {
            if !is_id(id) {
                add(
                    Severity::Error,
                    line,
                    format!("bad id {id:?} (use lowercase-kebab)"),
                );
            }
            if !ids.insert(id.to_string()) {
                add(Severity::Error, line, format!("duplicate id {id:?}"));
            }
        };

        check_id(&unit.id, 1, &mut add);
        for (name, value) in [
            ("title", &unit.title),
            ("tagline", &unit.tagline),
            ("icon", &unit.icon),
        ] {
            if value.is_empty() {
                add(Severity::Error, 1, format!("unit is missing '{name}:'"));
            }
        }

        for lesson in &unit.lessons {
            check_id(&lesson.id, lesson.source_line, &mut add);
            if lesson.title.is_empty() {
                add(
                    Severity::Error,
                    lesson.source_line,
                    "lesson is missing 'title:'".into(),
                );
            }
            if !lesson.steps.iter().any(|s| !s.is_exercise()) {
                add(
                    Severity::Error,
                    lesson.source_line,
                    "lesson has no @explain".into(),
                );
            }
            if lesson.exercise_count() < 3 {
                add(
                    Severity::Error,
                    lesson.source_line,
                    "lesson needs at least 3 exercises".into(),
                );
            }

            for step in &lesson.steps {
                let line = step.source_line;
                let mut codes: Vec<String> = step.code().map(String::from).into_iter().collect();
                match &step.content {
                    StepContent::Explain { body } => codes.extend(fenced_blocks(body)),
                    StepContent::Order { lines } => codes.push(lines.join("\n")),
                    _ => {}
                }
                for code in &codes {
                    for l in code.lines() {
                        let width = l.chars().count();
                        if width > MAX_WIDTH {
                            add(
                                Severity::Error,
                                line,
                                format!("code line longer than {MAX_WIDTH}: {l:?}"),
                            );
                        } else if width > WARN_WIDTH {
                            add(
                                Severity::Warning,
                                line,
                                format!("code line longer than {WARN_WIDTH}: {l:?}"),
                            );
                        }
                    }
                }
                if step.is_exercise() && step.explanation.is_empty() {
                    add(Severity::Error, line, "missing '? explanation'".into());
                }

                match &step.content {
                    StepContent::Explain { body } => {
                        if step.prompt.is_empty() {
                            add(Severity::Error, line, "@explain needs a title".into());
                        }
                        if body.trim().is_empty() {
                            add(Severity::Error, line, "empty @explain body".into());
                        }
                    }
                    StepContent::Choice {
                        code,
                        answer,
                        wrong,
                    } => {
                        if step.prompt.is_empty() {
                            add(Severity::Error, line, "missing prompt".into());
                        }
                        if !(1..=4).contains(&wrong.len()) {
                            add(Severity::Error, line, "need 1-4 '-' options".into());
                        }
                        let all: HashSet<&String> = wrong.iter().chain([answer]).collect();
                        if all.len() != wrong.len() + 1 {
                            add(Severity::Error, line, "duplicate options".into());
                        }
                        if step.check == Check::Output && code.is_none() {
                            add(Severity::Error, line, "!output needs a code block".into());
                        }
                    }
                    StepContent::Fill {
                        answers,
                        distractors,
                        ..
                    } => {
                        if distractors.is_empty() {
                            add(
                                Severity::Error,
                                line,
                                "add at least one '~' distractor".into(),
                            );
                        }
                        if distractors.iter().any(|d| answers.contains(d)) {
                            add(
                                Severity::Error,
                                line,
                                "a distractor equals an answer".into(),
                            );
                        }
                    }
                    StepContent::Order { lines } => {
                        if !(3..=10).contains(&lines.len()) {
                            add(
                                Severity::Error,
                                line,
                                "@order needs 3-10 non-empty lines".into(),
                            );
                        }
                        if lines.iter().map(|l| l.trim()).collect::<HashSet<_>>().len() < 2 {
                            add(
                                Severity::Error,
                                line,
                                "@order lines are all identical".into(),
                            );
                        }
                    }
                    StepContent::Bug { .. } => {
                        if step.prompt.is_empty() {
                            add(Severity::Error, line, "missing prompt".into());
                        }
                    }
                    StepContent::Compiles { .. } => {}
                }
            }
        }
    }
    issues
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::parser::parse_unit;

    #[test]
    fn flags_problems() {
        let text = "@unit Bad_Id
title: T
icon: x

@lesson l1
title: L

@choice Pick
+ a
- a
? why

@fill Fill
```
let ___ = 1;
let a_very_long_line_that_goes_past_the_limit = 1;
```
= x
? why
";
        let issues = lint_course(&Course {
            units: vec![parse_unit(text, "bad.md").unwrap()],
        });
        let messages: Vec<&str> = issues.iter().map(|i| i.message.as_str()).collect();
        for expected in [
            "bad id",
            "missing 'tagline:'",
            "no @explain",
            "at least 3 exercises",
            "duplicate options",
            "distractor",
            "longer than 46",
        ] {
            assert!(
                messages.iter().any(|m| m.contains(expected)),
                "missing {expected:?} in {messages:#?}"
            );
        }
    }

    #[test]
    fn ids() {
        assert!(is_id("own-moves") && is_id("a1"));
        assert!(!is_id("Own") && !is_id("a--b") && !is_id("-a") && !is_id(""));
    }
}
