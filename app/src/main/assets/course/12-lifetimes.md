@unit lifetimes
title: Lifetimes
tagline: How Rust knows your references are still good
icon: ⏳

@lesson life-why
title: Why lifetimes?

@explain No dangling allowed
A reference must never outlive the value it points to. Otherwise it would be **dangling** - pointing at freed memory.

```rust
fn main() {
    let r;
    {
        let x = 5;
        r = &x;
    } // x is dropped here...
    println!("{r}"); // ...so ❌
}
```

The stretch of code where a reference is valid is its **lifetime**. The borrow checker compares lifetimes at compile time, so dangling references never make it into your program.

@compiles
```rust
fn main() {
    let r;
    {
        let x = 5;
        r = &x;
    }
    println!("{r}");
}
```
= no
? "`x` does not live long enough": `x` is dropped at the end of the inner block, but `r` is still used after it.

@choice What does this print?
!output
```rust
fn main() {
    let outer = String::from("crab");
    let r;
    {
        r = &outer;
    }
    println!("{r}");
}
```
+ crab
- It doesn't compile
- Nothing, r is dangling
- &crab
? Only the **borrowed value** matters. `outer` lives until the end of `main`, so `r` stays valid even though it was assigned inside a block.

@choice What is a lifetime?
+ The region of code where a reference is valid
- How long the whole program runs
- A runtime timer that frees memory
- A kind of smart pointer
? Lifetimes are a purely compile-time idea. They cost nothing at runtime - no timers, no garbage collector.

@explain Returning references
A function can't hand out a reference to its own local variable - the local dies when the function returns:

```rust
fn dangle() -> &String {
    let s = String::from("hi");
    &s // s is dropped here! ❌
}
```

The fix is usually simple: return the **owned** value and let the caller own it.

@bug Which line does rustc complain about?
!error
```rust
fn dangle() -> &String {
    let s = String::from("hi");
    &s
}

fn main() {
    let r = dangle();
}
```
= 1
? "missing lifetime specifier": the return type is a reference, but there's no input it could be borrowing from. Rust stops right at the signature.

@fill Fix it by returning the String itself.
!compiles
```rust
fn make() -> ___ {
    let s = String::from("hi");
    ___
}

fn main() {
    println!("{}", make());
}
```
= String
= s
~ &String
~ &s
~ &str
~ *s
? Returning `s` **moves** ownership out to the caller, so nothing dangles. No lifetimes needed.

@order Put the lines in a working order.
!compiles
```rust
fn main() {
    let s = String::from("hi crab");
    let mut words = s.split(' ');
    let first = words.next().unwrap();
    println!("{first}");
}
```
? `first` is a `&str` that points into `s`. The owner `s` has to come first, and it stays alive until `first` is printed - a happy lifetime.

@compiles
```rust
fn main() {
    let r: &str;
    {
        let s = "hello";
        r = s;
    }
    println!("{r}");
}
```
= yes
? Sneaky! `s` is a string **literal**, which is baked into the program binary and lives forever (`&'static str`). Only the variable `s` goes away - the text it points to doesn't.

@lesson life-annotations
title: Lifetime annotations

@explain Which one do you borrow?
Here's a function that returns the longer of two strings:

```rust
fn longest(x: &str, y: &str) -> &str {
    if x.len() > y.len() { x }
    else { y }
}
```

It doesn't compile! The result borrows from `x` **or** `y`, and Rust can't tell which by looking at the signature. So it can't check whether the result is used for too long.

@compiles
```rust
fn pick(x: &str, y: &str) -> &str {
    x
}

fn main() {
    println!("{}", pick("a", "b"));
}
```
= no
? Even though the body returns `x`, Rust only checks the **signature**. With two reference inputs, it won't guess which one the output borrows from: "missing lifetime specifier".

@explain Naming lifetimes: 'a
Lifetime parameters start with an apostrophe and are declared in `< >` like generics:

```rust
fn longest<'a>(
    x: &'a str, y: &'a str,
) -> &'a str {
    if x.len() > y.len() { x }
    else { y }
}
```

This says: "the result is valid as long as **both** `x` and `y` are". In practice the result gets the **shorter** of the two lifetimes.

Annotations **don't** make anything live longer. They only describe how references relate, so the compiler can check the callers.

@fill Annotate `longest` so it compiles.
!compiles
```rust
fn longest<___>(
    x: &'a str, y: &'a str,
) -> ___ {
    if x.len() > y.len() { x }
    else { y }
}

fn main() {
    let w = longest("hi", "crab");
    println!("{w}");
}
```
= 'a
= &'a str
~ a
~ &str
~ &'static str
~ 'a str
? Declare the lifetime with `<'a>`, then tie the output to it with `&'a str`. Using `&'static str` would be a false promise: `x` and `y` might not live forever.

@choice What does `'a` in `longest` promise?
+ The result is valid while both inputs are
- x and y live exactly as long
- Rust keeps both strings alive longer
- The result lives for the whole program
? A lifetime annotation is a **constraint**, not an action. It never extends how long values live - it tells the compiler what to check.

@choice What does this print?
!output
```rust
fn longest<'a>(
    x: &'a str, y: &'a str,
) -> &'a str {
    if x.len() > y.len() { x }
    else { y }
}

fn main() {
    let a = String::from("ferris");
    let b = String::from("crab");
    println!("{}", longest(&a, &b));
}
```
+ ferris
- crab
- ferriscrab
- It doesn't compile
? Both strings live until the end of `main`, so the borrow is fine. `"ferris"` has 6 bytes, `"crab"` only 4.

@compiles
```rust
fn longest<'a>(
    x: &'a str, y: &'a str,
) -> &'a str {
    if x.len() > y.len() { x }
    else { y }
}

fn main() {
    let a = String::from("long one");
    let res;
    {
        let b = String::from("z");
        res = longest(&a, &b);
    }
    println!("{res}");
}
```
= no
? We know `res` would be `a`, but the compiler goes by the signature: `res` may borrow from `b`, which is dropped at the end of the block. "`b` does not live long enough".

@explain Elision: the rules you don't type
Most of the time you write no lifetimes at all - the compiler fills them in using three rules:

- every reference parameter gets its own lifetime
- exactly **one** input lifetime? The output gets it
- a method with `&self`? The output gets `self`'s lifetime

```rust
// really: fn first<'a>(s: &'a str)
//                        -> &'a str
fn first(s: &str) -> &str {
    &s[..1]
}
```

Only when the rules can't decide (like `longest`) do you have to annotate.

@compiles
```rust
fn first(s: &str) -> &str {
    &s[..1]
}

fn main() {
    println!("{}", first("crab"));
}
```
= yes
? One reference in, one reference out: elision ties the output to `s` automatically. Prints `c`.

@bug The signature promises a result tied to `x`. Which line breaks that promise?
!error
```rust
fn first<'a>(
    x: &'a str,
    y: &str,
) -> &'a str {
    y
}

fn main() {
    println!("{}", first("a", "b"));
}
```
= 5
? `y` has its own, unrelated lifetime, so it can't be returned as `&'a str`. Return `x`, or give `y` the lifetime `'a` too.

@lesson life-structs
title: Structs & 'static

@explain Structs that borrow
A struct can hold references too, but it needs a lifetime parameter:

```rust
struct Excerpt<'a> {
    part: &'a str,
}

fn main() {
    let s = String::from("Hi. Bye.");
    let first = &s[..3];
    let e = Excerpt { part: first };
    println!("{}", e.part);
}
```

`Excerpt<'a>` reads as: "an `Excerpt` can't outlive the text its `part` points into".

@compiles
```rust
struct Tag {
    name: &str,
}

fn main() {
    let t = Tag { name: "hot" };
    println!("{}", t.name);
}
```
= no
? A reference field needs a lifetime: `struct Tag<'a> { name: &'a str }`. Struct definitions never get lifetime elision.

@fill Let the struct hold a borrowed name.
!compiles
```rust
struct Crab<___> {
    name: &___ str,
}

fn main() {
    let n = String::from("Ferris");
    let c = Crab { name: &n };
    println!("{}", c.name);
}
```
= 'a
= 'a
~ a
~ &'a
~ 'static
? Declare `'a` on the struct, then use it on the reference field. `'static` wouldn't work: `&n` borrows a local `String`, which doesn't live forever.

@choice What does this print?
!output
```rust
struct Book<'a> {
    title: &'a str,
}

impl<'a> Book<'a> {
    fn short(&self) -> &str {
        &self.title[..4]
    }
}

fn main() {
    let t = String::from("Rustacean");
    let b = Book { title: &t };
    println!("{}", b.short());
}
```
+ Rust
- Rus
- Rustacean
- It doesn't compile
? `[..4]` takes the first 4 bytes. `short` needs no annotations: with `&self`, elision gives the output `self`'s lifetime.

@compiles
```rust
struct Tag<'a> {
    name: &'a str,
}

fn main() {
    let tag;
    {
        let s = String::from("hot");
        tag = Tag { name: &s };
    }
    println!("{}", tag.name);
}
```
= no
? `tag` borrows `s`, but `s` is dropped at the end of the inner block while `tag` is still used afterwards. The struct can't outlive what it points to.

@explain 'static: lives forever
The special lifetime `'static` means "valid for the entire program". Every string literal is a `&'static str`, stored right inside the binary:

```rust
fn motto() -> &'static str {
    "Fearless!"
}
```

You'll also see `T: 'static` bounds (for example on threads): the type holds no short-lived borrows.

⚠️ When the compiler complains, slapping `'static` on a reference is rarely the fix. Usually you want an **owned** value like `String`.

@compiles
```rust
fn motto() -> &'static str {
    "Fearless!"
}

fn main() {
    println!("{}", motto());
}
```
= yes
? `"Fearless!"` is a string literal, so it really does live for the whole program.

@choice Which of these has the type `&'static str`?
+ "hello"
- &String::from("hello")
- &name[..] where name is a String
- &format!("hello")
? Only string **literals** are baked into the binary. The others borrow a `String` that gets dropped at some point.

@bug Which line breaks the `'static` promise?
!error
```rust
fn name() -> &'static str {
    let s = String::from("Ferris");
    &s
}

fn main() {
    println!("{}", name());
}
```
= 3
? `&s` borrows a local `String` that dies when `name` returns. Return `String` instead - or, since the text is fixed, just return the literal `"Ferris"`.
