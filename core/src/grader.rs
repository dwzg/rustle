//! Checking the learner's answers.

use crate::model::{Step, StepContent};

/// What the learner submitted for an exercise.
#[derive(Debug, Clone, PartialEq, uniffi::Enum)]
pub enum Answer {
    Choice { option: String },
    Blanks { tokens: Vec<String> },
    Order { lines: Vec<String> },
    Compiles { compiles: bool },
    Line { line: i32 },
}

#[uniffi::export]
pub fn is_correct(step: &Step, answer: &Answer) -> bool {
    match (&step.content, answer) {
        (StepContent::Choice { answer: right, .. }, Answer::Choice { option }) => option == right,
        (StepContent::Fill { answers, .. }, Answer::Blanks { tokens }) => tokens == answers,
        // Compare trimmed text so identical lines (like `}`) are interchangeable.
        (StepContent::Order { lines: right }, Answer::Order { lines }) => {
            lines.len() == right.len() && lines.iter().zip(right).all(|(a, b)| a.trim() == b.trim())
        }
        (
            StepContent::Compiles {
                compiles: right, ..
            },
            Answer::Compiles { compiles },
        ) => compiles == right,
        (StepContent::Bug { bug_line, .. }, Answer::Line { line }) => line == bug_line,
        _ => false,
    }
}

/// Human-readable correct answer, shown when the learner gets it wrong.
#[uniffi::export]
pub fn solution(step: &Step) -> String {
    match &step.content {
        StepContent::Explain { .. } => String::new(),
        StepContent::Choice { answer, .. } => answer.clone(),
        StepContent::Fill { answers, .. } => answers.join("  ·  "),
        StepContent::Order { lines } => lines.join("\n"),
        StepContent::Compiles { compiles: true, .. } => "It compiles ✅".into(),
        StepContent::Compiles {
            compiles: false, ..
        } => "Compile error ❌".into(),
        StepContent::Bug { code, bug_line } => {
            let text = code
                .lines()
                .nth(*bug_line as usize - 1)
                .unwrap_or("")
                .trim();
            format!("Line {bug_line}: {text}")
        }
    }
}

/// Fills the blanks of a fill-in step with the correct answers.
pub fn filled_code(code: &str, answers: &[String]) -> String {
    let mut out = code.to_string();
    for a in answers {
        out = out.replacen(crate::model::BLANK, a, 1);
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::model::Check;

    fn step(content: StepContent) -> Step {
        Step {
            key: "k".into(),
            prompt: "p".into(),
            explanation: "e".into(),
            check: Check::None,
            content,
            source_line: 1,
        }
    }

    #[test]
    fn grades_each_kind() {
        let choice = step(StepContent::Choice {
            code: None,
            answer: "a".into(),
            wrong: vec!["b".into()],
        });
        assert!(is_correct(&choice, &Answer::Choice { option: "a".into() }));
        assert!(!is_correct(&choice, &Answer::Choice { option: "b".into() }));
        assert!(
            !is_correct(&choice, &Answer::Line { line: 1 }),
            "mismatched answer kind"
        );

        let order = step(StepContent::Order {
            lines: vec!["a".into(), "    }".into(), "}".into()],
        });
        assert!(is_correct(
            &order,
            &Answer::Order {
                lines: vec!["a".into(), "}".into(), "}".into()]
            }
        ));
        assert!(!is_correct(
            &order,
            &Answer::Order {
                lines: vec!["}".into(), "a".into(), "}".into()]
            }
        ));
        assert!(!is_correct(
            &order,
            &Answer::Order {
                lines: vec!["a".into()]
            }
        ));

        let bug = step(StepContent::Bug {
            code: "x\n  y;".into(),
            bug_line: 2,
        });
        assert!(is_correct(&bug, &Answer::Line { line: 2 }));
        assert_eq!(solution(&bug), "Line 2: y;");

        let fill = step(StepContent::Fill {
            code: "___ ___".into(),
            answers: vec!["a".into(), "b".into()],
            distractors: vec![],
        });
        assert!(is_correct(
            &fill,
            &Answer::Blanks {
                tokens: vec!["a".into(), "b".into()]
            }
        ));
        assert!(!is_correct(
            &fill,
            &Answer::Blanks {
                tokens: vec!["b".into(), "a".into()]
            }
        ));
        assert_eq!(filled_code("___ ___", &["a".into(), "b".into()]), "a b");
    }
}
