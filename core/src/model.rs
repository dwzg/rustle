//! The course data model, shared with Kotlin through UniFFI.

/// One course file handed over by the app: its name and text.
#[derive(Debug, Clone, uniffi::Record)]
pub struct CourseFile {
    pub name: String,
    pub text: String,
}

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct Course {
    pub units: Vec<CourseUnit>,
}

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct CourseUnit {
    pub id: String,
    pub title: String,
    pub tagline: String,
    pub icon: String,
    pub lessons: Vec<Lesson>,
    /// File the unit came from, for error messages.
    pub source_file: String,
}

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct Lesson {
    pub id: String,
    pub title: String,
    pub steps: Vec<Step>,
    pub source_line: u32,
}

#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct Step {
    /// Stable key (`lessonId#index`) used to remember mistakes.
    pub key: String,
    /// The question, or the title of an explain card.
    pub prompt: String,
    /// Shown after answering; empty for explain cards.
    pub explanation: String,
    /// What the course verifier compiles for this step.
    pub check: Check,
    pub content: StepContent,
    pub source_line: u32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum Check {
    None,
    /// The code is a full program whose stdout must equal the correct option.
    Output,
    /// The (filled-in) code must compile.
    Compiles,
    /// The code must fail to compile, with rustc pointing at the bug line.
    Error,
}

#[derive(Debug, Clone, PartialEq, uniffi::Enum)]
pub enum StepContent {
    Explain {
        body: String,
    },
    Choice {
        code: Option<String>,
        answer: String,
        wrong: Vec<String>,
    },
    /// `code` contains [`BLANK`] markers, one per answer.
    Fill {
        code: String,
        answers: Vec<String>,
        distractors: Vec<String>,
    },
    /// Lines in the correct order, indentation kept.
    Order {
        lines: Vec<String>,
    },
    Compiles {
        code: String,
        compiles: bool,
    },
    /// `bug_line` is 1-based within `code`.
    Bug {
        code: String,
        bug_line: i32,
    },
}

pub const BLANK: &str = "___";

impl Step {
    pub fn is_exercise(&self) -> bool {
        !matches!(self.content, StepContent::Explain { .. })
    }

    /// The step's code block, if it has one.
    pub fn code(&self) -> Option<&str> {
        match &self.content {
            StepContent::Explain { .. } | StepContent::Order { .. } => None,
            StepContent::Choice { code, .. } => code.as_deref(),
            StepContent::Fill { code, .. }
            | StepContent::Compiles { code, .. }
            | StepContent::Bug { code, .. } => Some(code),
        }
    }
}

impl Lesson {
    pub fn exercise_count(&self) -> usize {
        self.steps.iter().filter(|s| s.is_exercise()).count()
    }
}

impl Course {
    pub fn lessons(&self) -> impl Iterator<Item = &Lesson> {
        self.units.iter().flat_map(|u| u.lessons.iter())
    }

    pub fn steps(&self) -> impl Iterator<Item = &Step> {
        self.lessons().flat_map(|l| l.steps.iter())
    }
}

#[derive(Debug, Clone, PartialEq, uniffi::Error)]
pub enum CourseError {
    Parse {
        file: String,
        line: u32,
        reason: String,
    },
}

impl std::fmt::Display for CourseError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            CourseError::Parse { file, line, reason } => write!(f, "{file}:{line}: {reason}"),
        }
    }
}

impl std::error::Error for CourseError {}
