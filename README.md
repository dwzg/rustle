# Rustle 🦀

An Android app for learning the **Rust programming language** in short, game-like lessons, with Ferris the crab as your guide.

<p>
<img src="docs/screenshots/01_path.png" width="160">
<img src="docs/screenshots/04_choice.png" width="160">
<img src="docs/screenshots/05_fill.png" width="160">
<img src="docs/screenshots/07_compiles.png" width="160">
<img src="docs/screenshots/09_result.png" width="160">
<img src="docs/screenshots/11_rush_game.png" width="160">
<img src="docs/screenshots/12_playground.png" width="160">
<img src="docs/screenshots/13_profile.png" width="160">
</p>

## Features

- **Learning path**: 13 units, 39 lessons, ~380 steps, from `println!` to ownership, borrowing, traits, lifetimes and fearless concurrency. Lessons unlock as you go (or turn on *Explorer mode*).
- **Five exercise types**: multiple choice ("what does this print?"), fill in the blanks, put the lines in order, *will it compile?*, and *tap the line the compiler complains about*. Missed exercises come back at the end of the lesson.
- **Compiler Rush ⚡**: a 45-second swipe game. Right if it compiles, left if `rustc` says no. Streaks multiply your score.
- **Playground**: write Rust on your phone (syntax highlighting, auto-indent, a row of symbol keys) and run it on the official [Rust Playground](https://play.rust-lang.org). Needs internet; everything else works offline.
- **Motivation**: XP and levels, daily goal, streaks, star ratings, 16 achievements, and a *Practice* mode that resurfaces your past mistakes.
- Light and dark theme.

## Every answer is checked by the real compiler

Lesson content lives in plain text files in [`app/src/main/assets/course/`](app/src/main/assets/course) ([format](docs/COURSE_FORMAT.md)). `tools/verify_course.py` compiles every snippet with `rustc` (edition 2024): "will it compile?" cards must compile (or fail) as claimed, "what does this print?" answers must match the program's real output, and "spot the bug" lines must be where rustc reports the error. That's ~270 checks.

```sh
python3 tools/verify_course.py          # needs rustc on PATH
```

## Building

Requires JDK 17+ and the Android SDK (API 36).

```sh
./gradlew :app:assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease          # minified, signed with the debug key for sideloading
./gradlew :app:testDebugUnitTest        # logic, content, full-UI and screenshot tests (Robolectric)
```

GitHub Actions builds both APKs and uploads them as artifacts on every push.

## Project layout

| Path | What |
|---|---|
| `course/` | Data model, course file parser, answer grading |
| `data/` | Progress (XP, streaks, stars, mistakes), levels and achievements |
| `ui/home` | The winding lesson path |
| `ui/lesson` | Lesson player, exercise views, results screen |
| `ui/rush` | Compiler Rush |
| `ui/playground` | Editor and Rust Playground client |
| `ui/components` | Ferris (drawn in code), confetti, buttons, Markdown |

Built with Kotlin and Jetpack Compose. Rustle is an unofficial project; Ferris is the Rust community's unofficial mascot.
