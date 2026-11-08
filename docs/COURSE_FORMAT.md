# Rustle course format

Every unit of the course is one plain-text file in
`app/src/main/assets/course/`, named `NN-slug.md` (e.g. `03-ownership.md`).
Files are loaded in filename order.

Run `python3 tools/verify_course.py` after editing. It checks the structure
and compiles/runs every snippet that carries a check flag with `rustc`
(edition 2024).

## Structure

Lines starting with `@` begin a new block. Everything up to the next `@`
line belongs to that block.

```
@unit ownership
title: Ownership
tagline: Rust's big idea - memory safety without a garbage collector
icon: 📦

@lesson moves
title: Moving values
```

* `@unit <id>` must appear exactly once, first in the file.
* `@lesson <id>` starts a lesson. Ids are lowercase-kebab and unique across
  the whole course.
* Inside a lesson come **steps** (below). Each lesson needs at least one
  `@explain` and at least three exercises.

Code blocks are fenced with three backticks (optionally ```` ```rust ````).

## Steps

### `@explain <title>`
A short teaching card. Body is light Markdown: paragraphs, `**bold**`,
`` `inline code` ``, bullet lines starting with `- `, and fenced code
blocks.

### `@choice <prompt>` - multiple choice
```
@choice What does this print?
!output
    (optional code block)
+ the correct option
- a wrong option
- another wrong option
? Explanation shown after answering. Several `?` lines are joined.
```
Exactly one `+` line, one to four `-` lines. Options are shuffled in the app.
`!output` (optional) means the code block is a full program and its stdout
(trimmed) must equal the `+` option exactly. The verifier runs it.

### `@fill <prompt>` - fill in the blanks
Code block with one or more `___` (three underscores) blanks. Then one
`= token` line per blank, in order, and `~ token` lines for distractor
tokens. The learner taps tokens from a shuffled bank.
`!compiles` (optional) - with the blanks filled in, the program must compile.

### `@order <prompt>` - put the lines in order
Code block with the lines in the **correct** order (indentation is kept for
display). The app shuffles them. Make sure only one order is valid (lines
with identical text, like `}`, are interchangeable and that's fine).
`!compiles` (optional) - the code as written must compile.

### `@compiles [prompt]` - will it compile?
Code block that is a full program, then `= yes` or `= no`, then `?`
explanation. **Always verified** with rustc. These cards also feed the
"Compiler Rush" mini game, so write plenty of them.

### `@bug <prompt>` - tap the broken line
Code block, then `= N` (1-based line number inside the block), then `?`
explanation.
`!error` (optional) - the program must fail to compile, and rustc's primary
error must point at line N.

## Style guide

* Phone screen: keep code lines **at most 38 characters** and snippets under
  ~14 lines. Prompts and explanations short and punchy.
* Friendly, playful tone. Ferris the crab 🦀 is our mascot.
* Use edition 2024 semantics.
* Favour checkable steps (`!output`, `!compiles`, `!error`) - they are
  compiled by the verifier, so the content can't silently be wrong.
