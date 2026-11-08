//! Parser for the plain-text course format (docs/COURSE_FORMAT.md).
//!
//! Parsing fails only on problems that would break the app. Style and
//! content rules (ids, widths, option counts, ...) live in [`crate::lint`].

use crate::model::*;

struct Block {
    kind: String,
    arg: String,
    line: u32,
    lines: Vec<String>,
}

fn err(file: &str, line: u32, reason: impl Into<String>) -> CourseError {
    CourseError::Parse {
        file: file.to_string(),
        line,
        reason: reason.into(),
    }
}

fn split_blocks(text: &str, file: &str) -> Result<Vec<Block>, CourseError> {
    let mut blocks: Vec<Block> = Vec::new();
    let mut in_fence = false;
    for (i, line) in text.lines().enumerate() {
        let n = i as u32 + 1;
        if line.starts_with("```") {
            in_fence = !in_fence;
        }
        if !in_fence && line.starts_with('@') {
            let rest = &line[1..];
            let (kind, arg) = rest.split_once(' ').unwrap_or((rest, ""));
            blocks.push(Block {
                kind: kind.trim().to_string(),
                arg: arg.trim().to_string(),
                line: n,
                lines: Vec::new(),
            });
        } else if let Some(b) = blocks.last_mut() {
            b.lines.push(line.to_string());
        } else if !line.trim().is_empty() {
            return Err(err(file, n, "text before the first @block"));
        }
    }
    if in_fence {
        return Err(err(file, 0, "unclosed code fence"));
    }
    Ok(blocks)
}

fn meta(b: &Block) -> impl Fn(&str) -> String + '_ {
    move |key| {
        b.lines
            .iter()
            .filter_map(|l| l.split_once(':'))
            .find(|(k, _)| k.trim() == key)
            .map(|(_, v)| v.trim().to_string())
            .unwrap_or_default()
    }
}

#[derive(Default)]
struct Parts {
    codes: Vec<String>,
    plus: Vec<String>,
    minus: Vec<String>,
    eq: Vec<String>,
    tilde: Vec<String>,
    why: Vec<String>,
    flags: Vec<String>,
}

fn parts(b: &Block, file: &str) -> Result<Parts, CourseError> {
    let mut p = Parts::default();
    let mut in_fence = false;
    let mut code: Vec<&str> = Vec::new();
    for line in &b.lines {
        if line.starts_with("```") {
            if in_fence {
                p.codes.push(code.join("\n"));
                code.clear();
            }
            in_fence = !in_fence;
            continue;
        }
        if in_fence {
            code.push(line);
            continue;
        }
        let s = line.trim();
        if s.is_empty() {
            continue;
        }
        if let Some(flag) = s.strip_prefix('!') {
            p.flags.push(flag.trim().to_string());
            continue;
        }
        let target = match s.get(..2) {
            Some("+ ") => &mut p.plus,
            Some("- ") => &mut p.minus,
            Some("= ") => &mut p.eq,
            Some("~ ") => &mut p.tilde,
            Some("? ") => &mut p.why,
            _ => return Err(err(file, b.line, format!("unexpected line: {line:?}"))),
        };
        target.push(s[2..].trim().to_string());
    }
    Ok(p)
}

fn check_for(p: &Parts, allowed: &str, file: &str, b: &Block) -> Result<Check, CourseError> {
    let mut check = Check::None;
    for f in &p.flags {
        if f != allowed {
            return Err(err(
                file,
                b.line,
                format!("flag !{f} is not valid on @{}", b.kind),
            ));
        }
        check = match f.as_str() {
            "output" => Check::Output,
            "compiles" => Check::Compiles,
            "error" => Check::Error,
            _ => unreachable!(),
        };
    }
    Ok(check)
}

fn parse_step(b: &Block, key: String, file: &str) -> Result<Step, CourseError> {
    let step = |prompt: String, explanation: String, check, content| Step {
        key: key.clone(),
        prompt,
        explanation,
        check,
        content,
        source_line: b.line,
    };
    if b.kind == "explain" {
        let body = b.lines.join("\n").trim().to_string();
        return Ok(step(
            b.arg.clone(),
            String::new(),
            Check::None,
            StepContent::Explain { body },
        ));
    }

    let p = parts(b, file)?;
    let why = p.why.join(" ");
    let one_code = || -> Result<String, CourseError> {
        match p.codes.as_slice() {
            [c] => Ok(c.clone()),
            _ => Err(err(
                file,
                b.line,
                format!("@{} needs exactly one code block", b.kind),
            )),
        }
    };

    let (check, content) = match b.kind.as_str() {
        "choice" => {
            if p.codes.len() > 1 {
                return Err(err(file, b.line, "@choice takes at most one code block"));
            }
            let [answer] = p.plus.as_slice() else {
                return Err(err(file, b.line, "@choice needs exactly one '+' option"));
            };
            (
                check_for(&p, "output", file, b)?,
                StepContent::Choice {
                    code: p.codes.first().cloned(),
                    answer: answer.clone(),
                    wrong: p.minus.clone(),
                },
            )
        }
        "fill" => {
            let code = one_code()?;
            let blanks = code.matches(BLANK).count();
            if blanks == 0 || blanks != p.eq.len() {
                return Err(err(
                    file,
                    b.line,
                    format!("{blanks} blanks but {} '=' answers", p.eq.len()),
                ));
            }
            (
                check_for(&p, "compiles", file, b)?,
                StepContent::Fill {
                    code,
                    answers: p.eq.clone(),
                    distractors: p.tilde.clone(),
                },
            )
        }
        "order" => {
            let lines = one_code()?
                .lines()
                .filter(|l| !l.trim().is_empty())
                .map(String::from)
                .collect();
            (
                check_for(&p, "compiles", file, b)?,
                StepContent::Order { lines },
            )
        }
        "compiles" => {
            let compiles = match p.eq.as_slice() {
                [v] if v == "yes" => true,
                [v] if v == "no" => false,
                _ => return Err(err(file, b.line, "@compiles needs '= yes' or '= no'")),
            };
            check_for(&p, "", file, b)?;
            // Compile cards are always checked: they must compile exactly when `compiles` says so.
            (
                Check::Compiles,
                StepContent::Compiles {
                    code: one_code()?,
                    compiles,
                },
            )
        }
        "bug" => {
            let code = one_code()?;
            let lines = code.lines().count() as i32;
            let bug_line = match p.eq.as_slice() {
                [v] => v.parse::<i32>().ok().filter(|n| (1..=lines).contains(n)),
                _ => None,
            }
            .ok_or_else(|| {
                err(
                    file,
                    b.line,
                    format!("@bug needs '= N' with N in 1..={lines}"),
                )
            })?;
            (
                check_for(&p, "error", file, b)?,
                StepContent::Bug { code, bug_line },
            )
        }
        other => return Err(err(file, b.line, format!("unknown block @{other}"))),
    };
    let prompt = if b.kind == "compiles" && b.arg.is_empty() {
        "Will this compile?".to_string()
    } else {
        b.arg.clone()
    };
    Ok(step(prompt, why, check, content))
}

/// Parses one unit file.
pub fn parse_unit(text: &str, file: &str) -> Result<CourseUnit, CourseError> {
    let blocks = split_blocks(text, file)?;
    let Some(head) = blocks.first().filter(|b| b.kind == "unit") else {
        return Err(err(file, 1, "file must start with @unit"));
    };
    let unit_meta = meta(head);
    let mut lessons: Vec<Lesson> = Vec::new();
    for b in &blocks[1..] {
        match b.kind.as_str() {
            "unit" => return Err(err(file, b.line, "only one @unit per file")),
            "lesson" => lessons.push(Lesson {
                id: b.arg.clone(),
                title: meta(b)("title"),
                steps: Vec::new(),
                source_line: b.line,
            }),
            _ => {
                let Some(lesson) = lessons.last_mut() else {
                    return Err(err(file, b.line, "step outside a lesson"));
                };
                let key = format!("{}#{}", lesson.id, lesson.steps.len());
                lesson.steps.push(parse_step(b, key, file)?);
            }
        }
    }
    Ok(CourseUnit {
        id: head.arg.clone(),
        title: unit_meta("title"),
        tagline: unit_meta("tagline"),
        icon: unit_meta("icon"),
        lessons,
        source_file: file.to_string(),
    })
}

/// Parses all unit files, in file-name order.
pub fn parse_course(files: &[CourseFile]) -> Result<Course, CourseError> {
    let mut sorted: Vec<&CourseFile> = files.iter().collect();
    sorted.sort_by(|a, b| a.name.cmp(&b.name));
    let units = sorted
        .into_iter()
        .map(|f| parse_unit(&f.text, &f.name))
        .collect::<Result<_, _>>()?;
    Ok(Course { units })
}

#[cfg(test)]
mod tests {
    use super::*;

    const SAMPLE: &str = "@unit demo
title: Demo
tagline: t
icon: 🦀

@lesson demo-one
title: One

@explain Hi
Some **text**.

```rust
@not_a_block
```

@fill Fill it
!compiles
```rust
let ___ x = 1;
```
= mut
~ var
? Because.

@order Order it
```
a
    }
b
```
? Order.

@compiles
```
fn main() {}
```
= yes
? Fine.

@bug Where?
!error
```
a
b
```
= 2
? There.
";

    #[test]
    fn parses_sample() {
        let unit = parse_unit(SAMPLE, "demo.md").unwrap();
        assert_eq!(unit.id, "demo");
        assert_eq!(unit.icon, "🦀");
        let steps = &unit.lessons[0].steps;
        assert_eq!(steps.len(), 5);
        let StepContent::Explain { body } = &steps[0].content else {
            panic!()
        };
        assert!(
            body.contains("@not_a_block"),
            "@ inside a fence is code, not a block"
        );
        assert_eq!(steps[1].key, "demo-one#1");
        assert_eq!(steps[1].check, Check::Compiles);
        assert_eq!(
            steps[1].content,
            StepContent::Fill {
                code: "let ___ x = 1;".into(),
                answers: vec!["mut".into()],
                distractors: vec!["var".into()]
            }
        );
        assert_eq!(
            steps[2].content,
            StepContent::Order {
                lines: vec!["a".into(), "    }".into(), "b".into()]
            }
        );
        assert_eq!(steps[3].prompt, "Will this compile?");
        assert_eq!(steps[3].check, Check::Compiles);
        assert_eq!(steps[4].check, Check::Error);
        assert_eq!(steps[4].explanation, "There.");
    }

    #[test]
    fn rejects_broken_input() {
        let cases = [
            ("hello\n@unit x", "before the first"),
            ("@lesson x", "must start with @unit"),
            ("@unit x\n@choice Q\n+ a\n? b", "outside a lesson"),
            ("@unit x\n@lesson l\n@choice Q\n- a\n? b", "exactly one '+'"),
            (
                "@unit x\n@lesson l\n@fill Q\n```\n___ ___\n```\n= a\n? b",
                "2 blanks but 1",
            ),
            (
                "@unit x\n@lesson l\n@bug Q\n```\na\n```\n= 3\n? b",
                "N in 1..=1",
            ),
            (
                "@unit x\n@lesson l\n@compiles\n```\na\n```\n= maybe\n? b",
                "yes' or",
            ),
            ("@unit x\n@lesson l\n@quiz Q\n? b", "unknown block"),
            (
                "@unit x\n@lesson l\n@choice Q\n!compiles\n+ a\n- b\n? c",
                "not valid",
            ),
            (
                "@unit x\n@lesson l\n@choice Q\nstray\n? c",
                "unexpected line",
            ),
            ("@unit x\n```\nopen", "unclosed"),
        ];
        for (text, expected) in cases {
            let e = parse_unit(text, "t.md").unwrap_err().to_string();
            assert!(e.contains(expected), "{text:?} gave {e:?}");
        }
    }
}
