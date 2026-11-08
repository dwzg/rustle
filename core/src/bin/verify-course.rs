//! Checks the course files: structure and content rules, then compiles every
//! checkable snippet with rustc (edition 2024) to prove the answers are right.
//!
//! Usage: verify-course [--warnings] [--no-rustc] [--dir DIR] [FILE_PREFIX...]

use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};
use std::{env, fs, thread};

use rustle_core::grader::filled_code;
use rustle_core::lint::{Severity, lint_course};
use rustle_core::parser::parse_unit;
use rustle_core::{Check, Course, StepContent};

enum Expect {
    Compiles,
    Fails,
    Output(String),
    ErrorAt(i32),
}

struct Job {
    place: String,
    code: String,
    expect: Expect,
}

fn jobs(course: &Course) -> Vec<Job> {
    let mut out = Vec::new();
    for unit in &course.units {
        for step in unit.lessons.iter().flat_map(|l| &l.steps) {
            let place = format!("{}:{} {}", unit.source_file, step.source_line, step.key);
            let job = |code: String, expect| Job {
                place: place.clone(),
                code,
                expect,
            };
            match (&step.content, step.check) {
                (
                    StepContent::Choice {
                        code: Some(code),
                        answer,
                        ..
                    },
                    Check::Output,
                ) => out.push(job(code.clone(), Expect::Output(answer.clone()))),
                (StepContent::Fill { code, answers, .. }, Check::Compiles) => {
                    out.push(job(filled_code(code, answers), Expect::Compiles))
                }
                (StepContent::Order { lines }, Check::Compiles) => {
                    out.push(job(lines.join("\n"), Expect::Compiles))
                }
                (StepContent::Compiles { code, compiles }, _) => out.push(job(
                    code.clone(),
                    if *compiles {
                        Expect::Compiles
                    } else {
                        Expect::Fails
                    },
                )),
                (StepContent::Bug { code, bug_line }, Check::Error) => {
                    out.push(job(code.clone(), Expect::ErrorAt(*bug_line)))
                }
                _ => {}
            }
        }
    }
    out
}

struct Build {
    ok: bool,
    /// (line, message) of the first error, from rustc's short format.
    first_error: Option<(i32, String)>,
    stdout: String,
}

fn build_and_run(dir: &Path, code: &str, run: bool) -> std::io::Result<Build> {
    fs::create_dir_all(dir)?;
    fs::write(dir.join("main.rs"), code)?;
    // Always a full build: some errors (e.g. overflow lints) only appear during codegen.
    let out = Command::new("rustc")
        .args([
            "--edition",
            "2024",
            "-A",
            "warnings",
            "--error-format=short",
            "-o",
            "main",
            "main.rs",
        ])
        .current_dir(dir)
        .output()?;
    let stderr = String::from_utf8_lossy(&out.stderr);
    let first_error = stderr.lines().find_map(|l| {
        let rest = l.strip_prefix("main.rs:")?;
        let mut parts = rest.splitn(3, ':');
        let line = parts.next()?.parse().ok()?;
        let _col = parts.next()?;
        let msg = parts.next()?.trim();
        msg.starts_with("error").then(|| (line, msg.to_string()))
    });
    let mut stdout = String::new();
    if out.status.success() && run {
        let mut child = Command::new(dir.join("main"))
            .stdout(Stdio::piped())
            .stderr(Stdio::null())
            .spawn()?;
        let start = Instant::now();
        while child.try_wait()?.is_none() {
            if start.elapsed() > Duration::from_secs(10) {
                child.kill()?;
                break;
            }
            thread::sleep(Duration::from_millis(5));
        }
        stdout = String::from_utf8_lossy(&child.wait_with_output()?.stdout).into_owned();
    }
    Ok(Build {
        ok: out.status.success(),
        first_error,
        stdout,
    })
}

fn run_job(job: &Job, dir: &Path) -> Option<String> {
    let run = matches!(job.expect, Expect::Output(_));
    let b = match build_and_run(dir, &job.code, run) {
        Ok(b) => b,
        Err(e) => return Some(format!("{}: could not run rustc: {e}", job.place)),
    };
    let msg = b
        .first_error
        .as_ref()
        .map(|(_, m)| m.as_str())
        .unwrap_or("");
    let problem = match &job.expect {
        Expect::Compiles if !b.ok => format!("expected to compile, but: {msg}"),
        Expect::Fails if b.ok => "marked '= no' but it compiles".into(),
        Expect::Output(_) if !b.ok => format!("program does not compile: {msg}"),
        Expect::Output(want) if b.stdout.trim() != want.trim() => {
            format!("output {:?} != correct option {want:?}", b.stdout.trim())
        }
        Expect::ErrorAt(_) if b.ok => "!error but it compiles".into(),
        Expect::ErrorAt(want) => match &b.first_error {
            Some((line, _)) if line == want => return None,
            Some((line, m)) => format!("rustc points at line {line} ({m}), answer says {want}"),
            None => "rustc failed without a located error".into(),
        },
        _ => return None,
    };
    Some(format!("{}: {problem}", job.place))
}

fn main() {
    let mut args = env::args().skip(1);
    let mut show_warnings = false;
    let mut use_rustc = true;
    let mut dir = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../app/src/main/assets/course");
    let mut prefixes = Vec::new();
    while let Some(a) = args.next() {
        match a.as_str() {
            "--warnings" => show_warnings = true,
            "--no-rustc" => use_rustc = false,
            "--dir" => dir = PathBuf::from(args.next().expect("--dir needs a path")),
            _ => prefixes.push(a),
        }
    }

    let mut files: Vec<PathBuf> = fs::read_dir(&dir)
        .unwrap_or_else(|e| panic!("cannot read {}: {e}", dir.display()))
        .filter_map(|e| e.ok().map(|e| e.path()))
        .filter(|p| p.extension().is_some_and(|x| x == "md"))
        .filter(|p| {
            let name = p.file_name().unwrap().to_string_lossy();
            prefixes.is_empty() || prefixes.iter().any(|pre| name.starts_with(pre.as_str()))
        })
        .collect();
    files.sort();

    let mut errors: Vec<String> = Vec::new();
    let mut warnings: Vec<String> = Vec::new();
    let mut units = Vec::new();
    for path in &files {
        let name = path.file_name().unwrap().to_string_lossy().into_owned();
        match fs::read_to_string(path)
            .map_err(|e| e.to_string())
            .and_then(|t| parse_unit(&t, &name).map_err(|e| e.to_string()))
        {
            Ok(u) => units.push(u),
            Err(e) => errors.push(e),
        }
    }
    let course = Course { units };
    for issue in lint_course(&course) {
        let text = format!("{}:{}: {}", issue.file, issue.line, issue.message);
        match issue.severity {
            Severity::Error => errors.push(text),
            Severity::Warning => warnings.push(text),
        }
    }

    let jobs = jobs(&course);
    if use_rustc {
        let workers = thread::available_parallelism()
            .map(|n| n.get())
            .unwrap_or(2);
        let base = env::temp_dir().join(format!("rustle-verify-{}", std::process::id()));
        let found: Vec<String> = thread::scope(|s| {
            let handles: Vec<_> = (0..workers)
                .map(|w| {
                    let jobs = &jobs;
                    let dir = base.join(w.to_string());
                    s.spawn(move || {
                        jobs.iter()
                            .skip(w)
                            .step_by(workers)
                            .filter_map(|j| run_job(j, &dir))
                            .collect::<Vec<_>>()
                    })
                })
                .collect();
            handles
                .into_iter()
                .flat_map(|h| h.join().unwrap())
                .collect()
        });
        errors.extend(found);
        let _ = fs::remove_dir_all(&base);
    }

    if show_warnings {
        warnings.iter().for_each(|w| println!("warning: {w}"));
    }
    errors.iter().for_each(|e| println!("ERROR: {e}"));
    let lessons: usize = course.units.iter().map(|u| u.lessons.len()).sum();
    let steps = course.steps().count();
    let cards = course
        .steps()
        .filter(|s| matches!(s.content, StepContent::Compiles { .. }))
        .count();
    println!(
        "{} units, {lessons} lessons, {steps} steps, {cards} compile cards, {} rustc checks{}, {} width warnings, {} errors",
        course.units.len(),
        jobs.len(),
        if use_rustc { "" } else { " (skipped)" },
        warnings.len(),
        errors.len()
    );
    std::process::exit(if errors.is_empty() { 0 } else { 1 });
}
