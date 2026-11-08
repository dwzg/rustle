#!/usr/bin/env python3
"""Validate Rustle course files and check Rust snippets with rustc.

Usage:
    python3 tools/verify_course.py              # all units
    python3 tools/verify_course.py 03 04        # only files starting with 03 / 04
    python3 tools/verify_course.py --no-rustc   # structure only

See docs/COURSE_FORMAT.md for the format.
"""
import json
import os
import re
import subprocess
import sys
import tempfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
COURSE_DIR = ROOT / "app/src/main/assets/course"
STEP_KINDS = {"explain", "choice", "fill", "order", "compiles", "bug"}
EXERCISES = STEP_KINDS - {"explain"}
ID_RE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
WARN_WIDTH = 38
MAX_WIDTH = 46
BLANK = "___"


class Block:
    def __init__(self, kind, arg, line_no, path):
        self.kind, self.arg, self.line_no, self.path = kind, arg, line_no, path
        self.lines = []

    def where(self):
        return f"{self.path.name}:{self.line_no} @{self.kind} {self.arg}".rstrip()


def split_blocks(path):
    blocks, cur, in_fence = [], None, False
    for i, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if line.startswith("```"):
            in_fence = not in_fence
        if not in_fence and line.startswith("@"):
            kind, _, arg = line[1:].partition(" ")
            cur = Block(kind.strip(), arg.strip(), i, path)
            blocks.append(cur)
            continue
        if cur is None:
            if line.strip():
                raise ValueError(f"{path.name}:{i}: text before first @block")
            continue
        cur.lines.append(line)
    if in_fence:
        raise ValueError(f"{path.name}: unclosed code fence")
    return blocks


def parse_step(block):
    """Pull code blocks and prefixed lines out of an exercise block."""
    step = {"kind": block.kind, "prompt": block.arg, "codes": [], "flags": set(),
            "plus": [], "minus": [], "eq": [], "tilde": [], "why": [], "body": [],
            "where": block.where()}
    in_fence, code = False, []
    for line in block.lines:
        if line.startswith("```"):
            if in_fence:
                step["codes"].append("\n".join(code))
                code = []
            in_fence = not in_fence
            if block.kind == "explain":
                step["body"].append(line)
            continue
        if in_fence:
            code.append(line)
            if block.kind == "explain":
                step["body"].append(line)
            continue
        if block.kind == "explain":
            step["body"].append(line)
            continue
        s = line.strip()
        if not s:
            continue
        for prefix, key in (("+ ", "plus"), ("- ", "minus"), ("= ", "eq"), ("~ ", "tilde"), ("? ", "why")):
            if s.startswith(prefix):
                step[key].append(s[2:].strip())
                break
        else:
            if s.startswith("!"):
                step["flags"].add(s[1:].strip())
            elif s in ("=", "~", "+", "-", "?"):
                raise ValueError(f"{block.where()}: empty '{s}' line")
            else:
                raise ValueError(f"{block.where()}: unexpected line: {line!r}")
    return step


def validate(paths):
    errors, warnings, checks = [], [], []
    seen_ids = set()
    stats = {"units": 0, "lessons": 0, "steps": 0, "compiles": 0}

    def err(msg):
        errors.append(msg)

    for path in paths:
        try:
            blocks = split_blocks(path)
        except ValueError as e:
            err(str(e))
            continue
        if not blocks or blocks[0].kind != "unit":
            err(f"{path.name}: must start with @unit")
            continue
        stats["units"] += 1
        lesson = None
        lessons = []
        for b in blocks:
            if b.kind in ("unit", "lesson"):
                if not ID_RE.match(b.arg):
                    err(f"{b.where()}: bad id")
                if b.arg in seen_ids:
                    err(f"{b.where()}: duplicate id")
                seen_ids.add(b.arg)
                meta = dict(l.split(":", 1) for l in b.lines if ":" in l)
                meta = {k.strip(): v.strip() for k, v in meta.items()}
                need = ("title", "tagline", "icon") if b.kind == "unit" else ("title",)
                for k in need:
                    if not meta.get(k):
                        err(f"{b.where()}: missing '{k}:'")
                if b.kind == "unit" and b is not blocks[0]:
                    err(f"{b.where()}: only one @unit per file")
                if b.kind == "lesson":
                    lesson = {"block": b, "steps": []}
                    lessons.append(lesson)
                continue
            if b.kind not in STEP_KINDS:
                err(f"{b.where()}: unknown block kind")
                continue
            if lesson is None:
                err(f"{b.where()}: step outside a lesson")
                continue
            try:
                s = parse_step(b)
            except ValueError as e:
                err(str(e))
                continue
            lesson["steps"].append(s)
            stats["steps"] += 1
            w = s["where"]
            for c in s["codes"]:
                for ln in c.split("\n"):
                    if len(ln) > MAX_WIDTH:
                        err(f"{w}: code line longer than {MAX_WIDTH}: {ln!r}")
                    elif len(ln) > WARN_WIDTH:
                        warnings.append(f"{w}: code line > {WARN_WIDTH} chars: {ln!r}")
            k = s["kind"]
            code = s["codes"][0] if s["codes"] else None
            if k != "explain" and not s["why"]:
                err(f"{w}: missing '? explanation'")
            if k in ("fill", "order", "compiles", "bug") and len(s["codes"]) != 1:
                err(f"{w}: needs exactly one code block")
                continue
            if k == "explain":
                if not b.arg:
                    err(f"{w}: explain needs a title")
                if not any(l.strip() for l in s["body"]):
                    err(f"{w}: empty explain body")
            elif k == "choice":
                if not s["prompt"]:
                    err(f"{w}: missing prompt")
                if len(s["plus"]) != 1 or not 1 <= len(s["minus"]) <= 4:
                    err(f"{w}: need one '+' and 1-4 '-' options")
                if len(set(s["plus"] + s["minus"])) != len(s["plus"] + s["minus"]):
                    err(f"{w}: duplicate options")
                if len(s["codes"]) > 1:
                    err(f"{w}: at most one code block")
                if "output" in s["flags"]:
                    if not code:
                        err(f"{w}: !output needs code")
                    elif s["plus"]:
                        checks.append(("output", w, code, s["plus"][0]))
            elif k == "fill":
                n = code.count(BLANK)
                if n == 0:
                    err(f"{w}: no ___ blanks")
                if len(s["eq"]) != n:
                    err(f"{w}: {n} blanks but {len(s['eq'])} '=' answers")
                if not s["tilde"]:
                    err(f"{w}: add at least one '~' distractor")
                if set(s["tilde"]) & set(s["eq"]):
                    err(f"{w}: distractor equals an answer")
                if "compiles" in s["flags"] and len(s["eq"]) == n:
                    filled = code
                    for a in s["eq"]:
                        filled = filled.replace(BLANK, a, 1)
                    checks.append(("compiles", w, filled, None))
            elif k == "order":
                lines = [l for l in code.split("\n") if l.strip()]
                if not 3 <= len(lines) <= 10:
                    err(f"{w}: order needs 3-10 non-empty lines")
                if len(set(l.strip() for l in lines)) < 2:
                    err(f"{w}: lines are all identical")
                if "compiles" in s["flags"]:
                    checks.append(("compiles", w, code, None))
            elif k == "compiles":
                if s["eq"] not in (["yes"], ["no"]):
                    err(f"{w}: needs '= yes' or '= no'")
                else:
                    stats["compiles"] += 1
                    checks.append(("compiles" if s["eq"] == ["yes"] else "fails", w, code, None))
            elif k == "bug":
                if not s["prompt"]:
                    err(f"{w}: missing prompt")
                n_lines = len(code.split("\n"))
                if len(s["eq"]) != 1 or not s["eq"][0].isdigit() or not 1 <= int(s["eq"][0]) <= n_lines:
                    err(f"{w}: '= N' must be a line number 1..{n_lines}")
                elif "error" in s["flags"]:
                    checks.append(("error-line", w, code, int(s["eq"][0])))
        for l in lessons:
            kinds = [s["kind"] for s in l["steps"]]
            if "explain" not in kinds:
                err(f"{l['block'].where()}: lesson has no @explain")
            if sum(k in EXERCISES for k in kinds) < 3:
                err(f"{l['block'].where()}: lesson needs at least 3 exercises")
        stats["lessons"] += len(lessons)
    return errors, warnings, checks, stats


def rustc(code, workdir, run):
    src = Path(workdir) / "main.rs"
    src.write_text(code, encoding="utf-8")
    out = Path(workdir) / "main"
    cmd = ["rustc", "--edition", "2024", "-A", "warnings", "--error-format=json",
           "-o", str(out), str(src)]
    # Always build fully: some errors (e.g. overflow lints) only fire during codegen.
    p = subprocess.run(cmd, capture_output=True, text=True, cwd=workdir, timeout=120)
    diags = [json.loads(l) for l in p.stderr.splitlines() if l.startswith("{")]
    errs = [d for d in diags if d.get("level") == "error"]
    stdout = None
    if p.returncode == 0 and run:
        r = subprocess.run([str(out)], capture_output=True, text=True, timeout=10, cwd=workdir)
        stdout = r.stdout
    return p.returncode == 0, errs, stdout


def first_error_line(errs):
    for e in errs:
        for sp in e.get("spans", []):
            if sp.get("is_primary"):
                return sp["line_start"], e.get("message", "")
    return None, errs[0].get("message", "") if errs else ""


def run_check(check):
    kind, where, code, expected = check
    with tempfile.TemporaryDirectory() as d:
        ok, errs, stdout = rustc(code, d, run=(kind == "output"))
    msg = errs[0]["message"] if errs else ""
    if kind == "compiles" and not ok:
        return f"{where}: expected to compile, but: {msg}"
    if kind == "fails" and ok:
        return f"{where}: marked '= no' but it compiles"
    if kind == "output":
        if not ok:
            return f"{where}: program does not compile: {msg}"
        if stdout.strip() != expected.strip():
            return f"{where}: output {stdout.strip()!r} != correct option {expected!r}"
    if kind == "error-line":
        if ok:
            return f"{where}: !error but it compiles"
        line, m = first_error_line(errs)
        if line != expected:
            return f"{where}: rustc points at line {line} ({m}), answer says {expected}"
    return None


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    no_rustc = "--no-rustc" in sys.argv
    show_warnings = "--warnings" in sys.argv
    paths = sorted(COURSE_DIR.glob("*.md"))
    if args:
        paths = [p for p in paths if any(p.name.startswith(a) for a in args)]
    errors, warnings, checks, stats = validate(paths)
    if not no_rustc:
        with ThreadPoolExecutor(max_workers=os.cpu_count() or 2) as pool:
            errors += [r for r in pool.map(run_check, checks) if r]
    if show_warnings:
        for w in warnings:
            print("warning:", w)
    for e in errors:
        print("ERROR:", e)
    print(f"{stats['units']} units, {stats['lessons']} lessons, {stats['steps']} steps, "
          f"{stats['compiles']} compile cards, {len(checks)} rustc checks"
          f"{' (skipped)' if no_rustc else ''}, {len(warnings)} width warnings, {len(errors)} errors")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
