//! The Rust core of Rustle: course parsing, answer grading, and the rules for
//! progress, rewards and achievements. The Android app calls it through UniFFI;
//! the `verify-course` tool uses the same parser to check the course with rustc.

uniffi::setup_scaffolding!();

pub mod achievements;
pub mod engine;
pub mod grader;
pub mod lint;
pub mod model;
pub mod parser;
pub mod progress;

pub use engine::Engine;
pub use model::*;
