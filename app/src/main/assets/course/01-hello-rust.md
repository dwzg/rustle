@unit hello-rust
title: Hello, Rust
tagline: Your first steps with Ferris
icon: 👋

@lesson first-program
title: Your first program

@explain Meet Ferris 🦀
Rust is a fast, safe systems language. Its mascot is **Ferris** the crab, and Rust programmers call themselves **Rustaceans**.

Every Rust program starts running in a function called `main`:

```rust
fn main() {
    println!("Hello, Ferris!");
}
```

- `fn` declares a function
- `println!` prints a line. The `!` means it's a **macro**, not a normal function
- Statements end with a semicolon `;`

@choice What does this program print?
!output
```rust
fn main() {
    println!("Hello, Rustacean!");
}
```
+ Hello, Rustacean!
- "Hello, Rustacean!"
- println!("Hello, Rustacean!")
- Nothing, there is no return
? `println!` prints the text between the quotes (without the quotes) followed by a newline.

@fill Complete the program so it greets Ferris.
!compiles
```rust
___ main() {
    ___("Hi, Ferris!");
}
```
= fn
= println!
~ func
~ println
~ print()
~ def
? Functions are declared with `fn`, and printing uses the `println!` macro - don't forget the `!`.

@explain Placeholders
`println!` can fill values into `{}` placeholders, in order:

```rust
fn main() {
    let crabs = 3;
    println!("{} crabs!", crabs);
    println!("{crabs} crabs!");
}
```

You can even name a variable directly inside the braces, like `{crabs}`. Both lines print `3 crabs!`.

@choice What does this print?
!output
```rust
fn main() {
    let a = 2;
    let b = 5;
    println!("{} + {} = {}", a, b, a + b);
}
```
+ 2 + 5 = 7
- {} + {} = {}
- a + b = 7
- 2 + 5 = 25
? Each `{}` is replaced by the next argument: `a`, then `b`, then `a + b`.

@order Arrange the lines into a working program.
!compiles
```rust
fn main() {
    let name = "Ferris";
    println!("Hi, {name}!");
}
```
? The variable must be declared before it is used, and everything lives inside `main`'s curly braces.

@compiles
```rust
fn main() {
    println("Hello!");
}
```
= no
? `println` is a macro, so it must be called as `println!`. Without the `!` the compiler looks for a function named `println` and doesn't find one.

@bug Which line stops this program from compiling?
!error
```rust
fn main() {
    let x = 7;
    println!("x is {}", x)
    println!("done");
}
```
= 3
? Line 3 is missing its semicolon. Rust needs `;` to separate the two statements.

@lesson variables
title: Variables & mutability

@explain let it be
Variables are created with `let`. By default they are **immutable** - once a value is bound, it can't change.

```rust
fn main() {
    let lives = 3;
    // lives = 2; // ❌ error!
    println!("{lives}");
}
```

Lines starting with `//` are **comments**, ignored by the compiler.

@compiles
```rust
fn main() {
    let score = 10;
    score = 20;
    println!("{score}");
}
```
= no
? `score` is immutable. Assigning to it a second time is an error: "cannot assign twice to immutable variable".

@explain mut
Add `mut` to make a variable **mutable**:

```rust
fn main() {
    let mut score = 10;
    score = score + 5;
    println!("{score}"); // 15
}
```

Immutability by default makes code easier to reason about - you can see at a glance which values may change.

@fill Make the counter changeable.
!compiles
```rust
fn main() {
    let ___ count = 0;
    count += 1;
    println!("{count}");
}
```
= mut
~ var
~ const
~ mutable
? `let mut` creates a mutable binding, so `count += 1` is allowed.

@choice What does this print?
!output
```rust
fn main() {
    let mut x = 1;
    x = x * 10;
    x += 2;
    println!("{x}");
}
```
+ 12
- 1
- 10
- 30
? `x` starts at 1, becomes `1 * 10 = 10`, then `10 + 2 = 12`.

@compiles
```rust
fn main() {
    let mut gems = 5;
    gems -= 2;
    println!("{gems} gems left");
}
```
= yes
? `gems` is declared `mut`, so subtracting from it is fine. It prints `3 gems left`.

@explain Constants
For values that never change, use `const`. Constants need a type annotation and are written in `SCREAMING_SNAKE_CASE`:

```rust
const MAX_LEVEL: u32 = 99;

fn main() {
    println!("{MAX_LEVEL}");
}
```

@choice Which declaration is a valid constant?
+ const SPEED: u32 = 300;
- const speed = 300;
- const mut SPEED: u32 = 300;
- let const SPEED = 300;
? Constants always need a type (`: u32`) and can never be `mut`.

@lesson shadowing
title: Shadowing

@explain A new variable, same name
You can declare a new variable with the same name as an old one. The new one **shadows** the old:

```rust
fn main() {
    let x = 5;
    let x = x + 1;
    let x = x * 2;
    println!("{x}"); // 12
}
```

Each `let` creates a brand-new variable. The old values are never mutated.

@choice What does this print?
!output
```rust
fn main() {
    let level = 1;
    let level = level + 1;
    {
        let level = level * 10;
        println!("{level}");
    }
}
```
+ 20
- 2
- 10
- 1
? Inside the block, `level` is shadowed again: `2 * 10 = 20`.

@choice And what does this one print?
!output
```rust
fn main() {
    let level = 2;
    {
        let level = level * 10;
    }
    println!("{level}");
}
```
+ 2
- 20
- 0
- It doesn't compile
? The shadowing inside the `{ }` block ends with the block. Outside, `level` is still `2`.

@explain Changing type
Shadowing can even change a variable's **type**, which `mut` can't:

```rust
fn main() {
    let spaces = "   ";
    let spaces = spaces.len();
    println!("{spaces}"); // 3
}
```

The first `spaces` is text, the second is a number.

@compiles
```rust
fn main() {
    let mut spaces = "   ";
    spaces = spaces.len();
    println!("{spaces}");
}
```
= no
? `mut` lets you change the **value**, not the **type**. `spaces` is a string slice, so you can't assign a number to it. Use shadowing (`let spaces = ...`) instead.

@compiles
```rust
fn main() {
    let guess = "42";
    let guess: i32 = guess.parse().unwrap();
    println!("{}", guess + 1);
}
```
= yes
? The second `guess` shadows the text with a number. It prints `43`.

@order Reorder so the program prints 10.
!compiles
```rust
fn main() {
    let n = 4;
    let n = n + 1;
    let n = n * 2;
    println!("{n}");
}
```
? 4, then `4 + 1 = 5`, then `5 * 2 = 10`. Every shadowing `let` builds on the previous one.
