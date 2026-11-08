@unit functions
title: Functions
tagline: Little machines that take values in and give values back
icon: ⚙️

@lesson fn-basics
title: Parameters & return types

@explain Your own functions
Declare functions with `fn`. Names use `snake_case`, and every parameter **must** have a type:

```rust
fn greet(times: u32) {
    for _ in 0..times {
        println!("Hi! 🦀");
    }
}

fn main() {
    greet(2);
}
```

It doesn't matter whether a function is defined above or below the code that calls it.

@compiles
```rust
fn double(x) {
    println!("{}", x * 2);
}
fn main() {
    double(21);
}
```
= no
? Parameters always need a type annotation, like `x: i32`. Without one it's a syntax error - rustc expects a `:` after `x`. Rust never guesses types in signatures.

@explain Returning a value
Write the return type after an arrow `->`. The **last expression** of the body, with **no semicolon**, is the value returned:

```rust
fn square(n: i32) -> i32 {
    n * n
}

fn main() {
    let s = square(4);
    println!("{s}"); // 16
}
```

A function without `->` returns `()`, the empty "unit" value.

@fill Complete the signature.
!compiles
```rust
fn add(a: ___, b: i32) ___ i32 {
    a + b
}
fn main() {
    println!("{}", add(2, 3));
}
```
= i32
= ->
~ int
~ =>
~ :
~ number
? Parameters are written `name: type`, and the return type comes after `->`. Rust has no `int` type - use `i32`.

@choice What does this print?
!output
```rust
fn square(n: i32) -> i32 {
    n * n
}
fn main() {
    let x = square(3) + square(1);
    println!("{x}");
}
```
+ 10
- 9
- 16
- 7
? `square(3)` is 9 and `square(1)` is 1, so `x = 10`.

@bug Which line doesn't compile?
!error
```rust
fn add(a: i32, b: i32) -> i32 {
    a + b
}
fn main() {
    let total = add(1);
    println!("{total}");
}
```
= 5
? `add` takes 2 arguments but only 1 was supplied. Rust has no default or optional parameters.

@compiles
```rust
fn main() {
    let n = triple(5);
    println!("{n}");
}
fn triple(x: i32) -> i32 {
    x * 3
}
```
= yes
? Calling a function before its definition is fine - Rust looks at the whole file. This prints `15`.

@choice Which is the idiomatic Rust function name?
+ fn feed_crab()
- fn FeedCrab()
- fn feedCrab()
- fn feed-crab()
? Rust uses `snake_case` for functions and variables. `feed-crab` isn't even a valid name - it reads as `feed` minus `crab`.

@order Define `halve` above `main`, so it prints `8`.
!compiles
```rust
fn halve(n: i32) -> i32 {
    n / 2
}
fn main() {
    println!("{}", halve(16));
}
```
? Start with the signature `fn halve(...) -> i32 {`, then the body `n / 2`, close it, then `main` calls it.

@lesson fn-expressions
title: Statements vs expressions

@explain Two kinds of code
- A **statement** does something but produces no value. `let x = 5;` is a statement.
- An **expression** evaluates to a value: `5`, `x + 1`, `square(3)`, an `if`, even a whole `{ }` block.

```rust
fn main() {
    let y = {
        let x = 3;
        x + 1
    };
    println!("{y}"); // 4
}
```

The block's value is its last expression - `x + 1` - because it has **no semicolon**.

@compiles
```rust
fn main() {
    let x = (let y = 6);
    println!("{x}");
}
```
= no
? `let y = 6` is a statement, not an expression, so there's no value to put in `x`. rustc says "expected expression, found `let` statement".

@choice What does this print?
!output
```rust
fn main() {
    let y = {
        let a = 2;
        let b = 5;
        a * b
    };
    println!("{y}");
}
```
+ 10
- 7
- 5
- ()
? The block runs its statements, then evaluates to its tail expression `a * b = 10`.

@explain The semicolon gotcha
Adding `;` turns an expression into a statement, and a statement's value is `()`:

```rust
fn five() -> i32 {
    5; // ❌ oops
}
```

This fails with "mismatched types: expected `i32`, found `()`". Helpful rustc even says: "remove this semicolon to return this value". 🦀

@compiles
```rust
fn add_one(x: i32) -> i32 {
    x + 1;
}
fn main() {
    println!("{}", add_one(4));
}
```
= no
? The `;` after `x + 1` throws the value away, so the body returns `()` instead of an `i32`: "mismatched types".

@compiles
```rust
fn main() {
    let total = {
        let a = 2;
        a + 3;
    };
    println!("{total}");
}
```
= no
? The trailing `;` makes the block evaluate to `()`. Then `println!` fails: "`()` doesn't implement `std::fmt::Display`".

@fill Make `triple` return its result.
!compiles
```rust
fn triple(n: i32) -> i32 {
    ___
}
fn main() {
    println!("{}", triple(4));
}
```
= n * 3
~ n * 3;
~ let r = n * 3;
~ i32 = n * 3
? The tail expression must have no semicolon: `n * 3` becomes the return value.

@choice Which of these is an **expression**?
+ { 1 + 2 }
- let x = 3;
- let y = { 1 + 2 };
- fn f() {}
? A block `{ 1 + 2 }` evaluates to `3`. The `let` lines are statements, and `fn` is an item definition.

@bug Which line causes the error?
!error
```rust
fn pick(big: bool) -> i32 {
    if big {
        100
    }
}
fn main() {
    println!("{}", pick(true));
}
```
= 2
? An `if` without `else` produces `()` when the condition is false, so it can't be the `i32` result: "`if` may be missing an `else` clause".

@compiles
```rust
fn nothing() {}
fn main() {
    let u: () = nothing();
    println!("{:?}", u);
}
```
= yes
? A function without `->` returns `()`, so storing it in a `()` variable is fine. `{:?}` prints it as `()`.

@lesson fn-return-scope
title: Early return & scope

@explain return
Use `return` to leave a function **early**. It's great for "guard" checks at the top:

```rust
fn safe_div(a: i32, b: i32) -> i32 {
    if b == 0 {
        return 0;
    }
    a / b
}
```

At the end of a function, the idiomatic style is a plain tail expression (`a / b`), not `return a / b;`.

@choice What does this print?
!output
```rust
fn pts(n: i32) -> i32 {
    if n < 0 {
        return 0;
    }
    n * 10
}
fn main() {
    let (a, b) = (pts(-5), pts(2));
    println!("{a} {b}");
}
```
+ 0 20
- -50 20
- 0 0
- -5 20
? `pts(-5)` hits the guard and returns `0` early. `pts(2)` skips it and returns `2 * 10`.

@compiles
```rust
fn seven() -> i32 {
    return 7;
}
fn main() {
    println!("{}", seven());
}
```
= yes
? A semicolon after `return` is fine: `return` leaves the function immediately, so nothing is lost. Not idiomatic at the end, but it compiles.

@fill Return 0 early for negative numbers.
!compiles
```rust
fn clamp(n: i32) -> i32 {
    if n < 0 {
        ___ 0;
    }
    n
}
fn main() {
    println!("{}", clamp(-3));
}
```
= return
~ break
~ yield
~ exit
? `return` exits the whole function. `break` only leaves a loop.

@compiles
```rust
fn ping() {
    return 5;
}
fn main() {
    ping();
}
```
= no
? `ping` has no `->`, so it returns `()`. Returning `5` is a "mismatched types" error: expected `()`, found integer.

@explain Scope
A variable lives from its `let` until the end of the `{ }` block it was declared in. Functions are separate worlds: they can't see the caller's variables, only their parameters.

```rust
fn show(level: i32) {
    println!("level {level}");
}

fn main() {
    let level = 3;
    show(level); // pass it in!
}
```

@bug Which line fails to compile?
!error
```rust
fn main() {
    {
        let gem = 7;
    }
    println!("{gem}");
}
```
= 5
? `gem` stopped existing at the `}` on line 4. On line 5 the compiler says "cannot find value `gem` in this scope".

@compiles
```rust
fn show() {
    println!("{level}");
}
fn main() {
    let level = 3;
    show();
}
```
= no
? `show` can't see `main`'s variables: "cannot find value `level` in this scope". Give it a parameter instead.

@choice What does this print?
!output
```rust
fn first_even(start: i32) -> i32 {
    let mut n = start;
    loop {
        if n % 2 == 0 {
            return n;
        }
        n += 1;
    }
}
fn main() {
    println!("{}", first_even(7));
}
```
+ 8
- 7
- 0
- It loops forever
? 7 is odd, so `n` becomes 8. Then `return n` exits both the loop **and** the function.

@order Put `fee` above `main`. Kids under 12 ride free!
!compiles
```rust
fn fee(age: u32) -> u32 {
    if age < 12 {
        return 0;
    }
    10
}
fn main() {
    println!("{}", fee(8));
}
```
? The guard `if` comes first and returns `0` early. The tail `10` is only reached for ages 12 and up. (An `if` without `else` can't produce `10` - it must be `()`.)
