@unit enums-match
title: Enums & match
tagline: One of many - and the compiler checks them all
icon: 🔀

@lesson enum-basics
title: Enums & match

@explain One of these
A struct is "this **and** that". An **enum** is "this **or** that": a value is exactly one of its **variants**.

```rust
enum Dir {
    Up,
    Down,
}

let d = Dir::Up;
```

Variants can carry data, either tuple-style or with named fields:

```rust
enum Shape {
    Square(u32),
    Rect { w: u32, h: u32 },
    Dot,
}
```

@choice Which line creates a valid `Shape`?
+ Shape::Square(2)
- Shape.Square(2)
- Shape::Rect(1, 2)
- Square(2)
? Variants live in the enum's namespace, so you need `Shape::`. `Rect` has named fields, so it's built as `Shape::Rect { w: 1, h: 2 }`.

@explain match
`match` compares a value against **patterns**, top to bottom, and runs the first arm that fits:

```rust
fn area(s: Shape) -> u32 {
    match s {
        Shape::Square(x) => x * x,
        Shape::Rect { w, h } => w * h,
        Shape::Dot => 0,
    }
}
```

- Each arm is `pattern => expression`
- Patterns **bind** the variant's data to names like `x`, `w`, `h`
- `match` is an expression: the chosen arm is its value
- It must be **exhaustive**: every possible value needs an arm

@choice What does this print?
!output
```rust
enum Shape {
    Square(u32),
    Rect { w: u32, h: u32 },
}

fn area(s: Shape) -> u32 {
    match s {
        Shape::Square(x) => x * x,
        Shape::Rect { w, h } => w * h,
    }
}

fn main() {
    let r = Shape::Rect {
        w: 2,
        h: 5,
    };
    println!("{}", area(r));
}
```
+ 10
- 7
- 25
- 4
? `r` is a `Rect`, so the second arm matches with `w = 2` and `h = 5`: `2 * 5 = 10`.

@compiles
```rust
enum Light { Red, Yellow, Green }

fn main() {
    let l = Light::Green;
    let msg = match l {
        Light::Red => "stop",
        Light::Green => "go",
    };
    println!("{msg}");
}
```
= no
? "non-exhaustive patterns: `Light::Yellow` not covered". The compiler knows every variant, so it won't let you forget one. That's a superpower when you add new variants later.

@fill Add a catch-all arm.
!compiles
```rust
fn main() {
    let n = 7;
    let s = match n {
        1 => "one",
        2 => "two",
        ___ => "many",
    };
    println!("{s}");
}
```
= _
~ else
~ default
~ *
~ ..
? `_` matches anything without binding it. There are way too many integers to list them all, so a catch-all is needed.

@bug Which line breaks this `match`?
!error
```rust
fn main() {
    let lucky = true;
    let n = match lucky {
        true => 7,
        false => "none",
    };
}
```
= 5
? Since `match` is an expression, every arm must produce the **same type**. The first arm gives an integer, so `"none"` is an error: "`match` arms have incompatible types".

@compiles
```rust
enum Dir { Up, Down }

fn main() {
    let d = Dir::Down;
    match d {
        Up => println!("up"),
        Down => println!("down"),
    }
}
```
= no
? Without `Dir::`, `Up` is not the variant: it's a brand-new **variable** that matches anything! Rust catches this classic mistake: "pattern binding `Up` is named the same as one of the variants". Write `Dir::Up`.

@compiles
```rust
enum Coin { Penny, Dime }

fn cents(c: Coin) -> u32 {
    match c {
        Coin::Penny => 1,
        Coin::Dime => 10,
    }
}

fn main() {
    println!("{}", cents(Coin::Dime));
}
```
= yes
? Both variants are covered, and every arm returns a `u32`. Prints `10`.

@lesson enum-option
title: Option - no null!

@explain No null here
Many languages have `null`, and its inventor calls it his "billion-dollar mistake". Rust has no null. Instead, a value that might be missing is an `Option<T>`:

```rust
enum Option<T> {
    None,
    Some(T),
}
```

It's built in, so just write `Some(5)` or `None`.

An `Option<i32>` is **not** an `i32`. You have to deal with the `None` case before you can use the number, and the compiler makes sure you do 🦀

@compiles
```rust
fn main() {
    let a: Option<i32> = Some(2);
    let b = a + 1;
    println!("{b}");
}
```
= no
? "cannot add `{integer}` to `Option<i32>`". The number is wrapped up in a `Some`, and `a` might just as well be `None`. Unwrap it first, e.g. with `match`.

@choice What does this print?
!output
```rust
fn half(n: i32) -> Option<i32> {
    if n % 2 == 0 {
        Some(n / 2)
    } else {
        None
    }
}

fn main() {
    match half(7) {
        Some(h) => println!("{h}"),
        None => println!("odd!"),
    }
}
```
+ odd!
- 3
- 3.5
- Some(3)
? 7 is odd, so `half` returns `None` and the second arm runs.

@fill Return a result or nothing.
!compiles
```rust
fn find(v: &[i32]) -> Option<i32> {
    for x in v {
        if x % 2 == 0 {
            return ___(*x);
        }
    }
    ___
}

fn main() {
    println!("{:?}", find(&[1, 4]));
}
```
= Some
= None
~ null
~ Ok
~ nil
~ Option
? Found one: wrap it in `Some`. Nothing found: `None`. This prints `Some(4)`.

@explain if let
When you only care about **one** pattern, a full `match` is a bit much. `if let` runs its block only if the pattern fits:

```rust
let fav = Some("crab");
if let Some(f) = fav {
    println!("I love {f}!");
} else {
    println!("No favourite");
}
```

The `else` part is optional. Handy helpers too:

- `x.unwrap_or(0)` gives the value, or `0` for `None`
- `x.unwrap()` gives the value, or **panics** on `None`. Use it sparingly!

@choice What does this print?
!output
```rust
fn main() {
    let a: Option<u32> = None;
    let b = Some(5);
    let x = a.unwrap_or(1);
    let y = b.unwrap_or(1);
    println!("{}", x + y);
}
```
+ 6
- 2
- 5
- It panics
? `a` is `None`, so it falls back to `1`. `b` holds `5`. `1 + 5 = 6`.

@order Arrange the lines so it prints `got 3`.
!compiles
```rust
fn main() {
    let x: Option<i32> = Some(3);
    if let Some(n) = x {
        println!("got {n}");
    } else {
        println!("nothing");
    }
}
```
? `n` only exists inside the `if let` block, so `got {n}` has to go there. The `else` block handles `None`.

@compiles
```rust
fn main() {
    let x: Option<i32> = None;
    if let Some(n) = x {
        println!("{n}");
    } else {
        println!("{n}");
    }
}
```
= no
? "cannot find value `n` in this scope". `n` is only bound inside the `if let` block. In the `else` block there's no value to bind, since `x` is `None`.

@bug Which line doesn't compile?
!error
```rust
fn get(n: i32) -> Option<i32> {
    if n > 0 { Some(n) } else { None }
}

fn main() {
    let v: i32 = get(5);
    println!("{}", v + 1);
}
```
= 6
? Mismatched types: `get` returns an `Option<i32>`, not an `i32`. Use `match`, `if let` or `unwrap_or` to get at the number.

@lesson enum-patterns
title: Pattern power

@explain Fancy patterns
Patterns can do a lot more than name a variant:

```rust
fn main() {
    let n = 15;
    let s = match n {
        0 => "zero",
        1 | 2 => "tiny",
        3..=9 => "small",
        x if x % 2 == 0 => "big even",
        _ => "big odd",
    };
    println!("{s}"); // big odd
}
```

- `|` means "or"
- `3..=9` is an inclusive range
- `x if ...` binds the value and adds a **guard**: an extra condition
- The first arm that fits wins

@choice What does this print?
!output
```rust
fn main() {
    let n = 12;
    let s = match n {
        0 => "zero",
        x if x > 10 => "big",
        1..=20 => "medium",
        _ => "other",
    };
    println!("{s}");
}
```
+ big
- medium
- other
- zero
? 12 also fits `1..=20`, but arms are tried top to bottom, and `x if x > 10` comes first.

@compiles
```rust
fn main() {
    let n: u8 = 5;
    let s = match n {
        x if x < 10 => "small",
        x if x >= 10 => "big",
    };
    println!("{s}");
}
```
= no
? "non-exhaustive patterns". We know the two guards cover every `u8`, but the compiler doesn't look inside guard conditions when checking exhaustiveness. Make the last arm `_`.

@fill Complete the thermometer 🌡️
!compiles
```rust
fn main() {
    let t = 35;
    let msg = match t {
        x ___ x > 30 => "hot",
        0..=30 => "ok",
        ___ => "freezing",
    };
    println!("{msg}");
}
```
= if
= _
~ when
~ where
~ else
~ default
? A guard is written `pattern if condition`. Negative temperatures are left over, so `_` catches them.

@bug Which line has a broken pattern?
!error
```rust
enum Shape {
    Circle(u32),
    Square(u32),
}

fn main() {
    let s = Shape::Circle(2);
    let n = match s {
        Shape::Circle => 1,
        Shape::Square(x) => x,
    };
}
```
= 9
? `Circle` carries a value, so the pattern needs a slot for it: `Shape::Circle(r)`, or `Shape::Circle(_)` to ignore it. rustc: "expected unit struct, unit variant or constant, found tuple variant".

@explain let-else and matches!
`let ... else` unpacks a value **or bails out**:

```rust
fn plus_one(o: Option<u32>) -> u32 {
    let Some(n) = o else {
        return 0;
    };
    n + 1
}
```

- If the pattern fits, `n` is usable **after** the `let`, with no extra nesting
- The `else` block **must diverge**: `return`, `break`, `continue` or panic

`matches!(value, pattern)` just answers yes or no:

```rust
let c = 'x';
let low = matches!(c, 'a'..='z');
```

@compiles
```rust
fn main() {
    let o: Option<i32> = None;
    let Some(n) = o else {
        println!("none!");
    };
    println!("{n}");
}
```
= no
? "`else` clause of `let...else` does not diverge". If the `else` block just carried on, `n` would have no value. Add a `return;` after the `println!`.

@compiles
```rust
fn main() {
    let o = Some(4);
    let Some(n) = o else {
        return;
    };
    println!("{}", n * 10);
}
```
= yes
? `o` is `Some(4)`, so `n` is `4` and the program prints `40`. If `o` were `None`, `main` would simply return.

@choice What does this print?
!output
```rust
fn main() {
    let c = 'R';
    let up = matches!(c, 'A'..='Z');
    let dg = matches!(c, '0'..='9');
    println!("{up} {dg}");
}
```
+ true false
- false false
- true true
- false true
? `'R'` is in the range `'A'..='Z'`, but it's not a digit.
