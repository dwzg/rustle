@unit ownership
title: Ownership
tagline: Rust's big idea - memory safety without a garbage collector
icon: 📦

@lesson own-rules
title: The ownership rules

@explain Who owns this?
Most languages either make you free memory by hand (C) or run a garbage collector (Java, Python). Rust does neither. Instead, the compiler checks three **ownership rules**:

- Each value has exactly one **owner** (a variable)
- There can only be **one owner at a time**
- When the owner goes **out of scope**, the value is **dropped** (its memory is freed)

```rust
fn main() {
    {
        let s = String::from("🦀");
        println!("{s}");
    } // s is dropped here
}
```

No leaks, no double frees, no GC pauses. Ferris approves. 🦀

@choice When is the String's memory freed?
```rust
fn main() {
    let a = 1;
    {
        let s = String::from("hi");
    } // <- A
    println!("{a}");
} // <- B
```
+ At A, when `s` goes out of scope
- At B, when `main` ends
- Whenever the garbage collector runs
- Never - you must call `free(s)`
? `s` is owned by the inner block. At its closing `}` Rust automatically drops it.

@explain String vs string literals
Two kinds of text you'll meet constantly:

- `"hello"` is a **string literal** (`&str`). It's baked into the program and can't change.
- `String` is an owned, growable string stored on the **heap**.

```rust
fn main() {
    let lit = "fixed";
    let mut s = String::from("Hi");
    s.push_str(", crab"); // grows!
    println!("{lit} {s}");
}
```

A `String` owns its heap memory - that's exactly what ownership rules are about.

@compiles
```rust
fn main() {
    let s = "Ferris";
    s.push_str(" 🦀");
    println!("{s}");
}
```
= no
? `s` is a string literal (`&str`), which can't grow: "no method named `push_str` found for reference `&str`". Use `String::from("Ferris")` instead.

@fill Build a growable String.
!compiles
```rust
fn main() {
    let mut s = String::___("Fer");
    s.___("ris");
    println!("{s}");
}
```
= from
= push_str
~ new
~ push
~ append
~ str
? `String::from` creates a `String` from a literal. `push_str` appends text (plain `push` adds a single `char`).

@choice What does this print?
!output
```rust
fn main() {
    let mut s = String::new();
    s.push('R');
    s.push_str("ust");
    println!("{s} {}", s.len());
}
```
+ Rust 4
- R 1
- Rust 3
- ust 3
? `String::new()` starts empty, `push` adds `'R'`, `push_str` adds `"ust"`. That's 4 bytes long.

@bug Which line stops this from compiling?
!error
```rust
fn main() {
    let s = String::from("hi");
    s.push_str("!");
    println!("{s}");
}
```
= 3
? `push_str` changes the String, but `s` isn't `mut`. rustc says "cannot borrow `s` as mutable" (borrowing is next unit!). Fix: `let mut s`.

@compiles
```rust
fn main() {
    let mut s = String::from("a");
    {
        let t = String::from("b");
        s.push_str("c");
    }
    println!("{s}");
}
```
= yes
? `t` is dropped at the inner `}`, but `s` lives in `main`'s scope, so it's still usable. Prints `ac`.

@order Arrange so it prints `Hello, Ferris`.
!compiles
```rust
fn main() {
    let mut msg = String::new();
    msg.push_str("Hello");
    msg.push_str(", Ferris");
    println!("{msg}");
}
```
? Create an empty `String`, append the pieces in order, then print it.

@lesson own-moves
title: Moves & clones

@explain Moves
Assigning a `String` to another variable **moves** it. The old variable is no longer valid:

```rust
fn main() {
    let a = String::from("crab");
    let b = a; // a moved into b
    // println!("{a}"); ❌
    println!("{b}");
}
```

Why? Both would point at the same heap memory, and both would free it at the end of scope - a **double free**. With a move there's only ever one owner.

Using `a` afterwards is an error: "borrow of moved value: `a`".

@bug Which line breaks the rules?
!error
```rust
fn main() {
    let a = String::from("crab");
    let b = a;
    println!("{b}");
    println!("{a}");
}
```
= 5
? The value moved from `a` to `b` on line 3. Line 5 uses `a` again: "borrow of moved value: `a`".

@compiles
```rust
fn main() {
    let mut a = String::from("old");
    let b = a;
    a = String::from("new");
    println!("{a} {b}");
}
```
= yes
? After the move, `a` holds no value - but since it's `mut`, you can assign it a brand-new one. Prints `new old`.

@explain clone & Copy
Want a real second copy? Ask for it with `.clone()`, which duplicates the heap data:

```rust
fn main() {
    let a = String::from("hi");
    let b = a.clone();
    println!("{a} {b}"); // both OK
}
```

Simple types that live entirely on the stack are **`Copy`**: assigning them copies instead of moving. That includes all integers, floats, `bool`, `char`, and tuples/arrays made only of `Copy` types.

@compiles
```rust
fn main() {
    let x = 5;
    let y = x;
    println!("{x} {y}");
}
```
= yes
? `i32` is `Copy`, so `let y = x` copies the number. `x` stays valid. Prints `5 5`.

@fill Keep both Strings usable.
!compiles
```rust
fn main() {
    let a = String::from("🦀");
    let b = a.___();
    println!("{a}{b}");
}
```
= clone
~ copy
~ move
~ dup
? `.clone()` makes a deep copy, so `a` and `b` each own their own String. `String` has no `copy` method - it isn't `Copy`.

@choice What does this print?
!output
```rust
fn main() {
    let a = String::from("hi");
    let mut b = a.clone();
    b.push_str("!!");
    println!("{a} {b}");
}
```
+ hi hi!!
- hi!! hi!!
- hi hi
- It doesn't compile
? `b` is an independent copy. Changing it leaves `a` untouched.

@choice Which type is **not** `Copy`?
+ String
- i32
- char
- (bool, f64)
? `String` owns heap memory, so it moves. A tuple is `Copy` when all its parts are, like `(bool, f64)`.

@compiles
```rust
fn main() {
    let t = (1, String::from("x"));
    let u = t;
    println!("{}", t.0);
}
```
= no
? The tuple holds a `String`, so it isn't `Copy`. `let u = t` moves the whole tuple, and `t.0` is a "borrow of moved value: `t`".

@order Arrange the lines so the program compiles.
!compiles
```rust
fn main() {
    let a = String::from("hi");
    let b = a.clone();
    let c = a;
    println!("{b} {c}");
}
```
? You must clone `a` **before** moving it into `c`. Swap those two lines and you'd be cloning a moved value.

@lesson own-functions
title: Ownership & functions

@explain Functions take ownership
Passing a value to a function works just like assigning it: a `String` is **moved** into the parameter, and a `Copy` type is copied.

```rust
fn eat(food: String) {
    println!("nom {food}");
} // food is dropped here

fn main() {
    let pie = String::from("🥧");
    eat(pie); // pie moved
    let n = 5;
    println!("{n}"); // n is Copy
}
```

After `eat(pie)`, `main` can't use `pie` anymore - `eat` owned it and dropped it.

@bug Which line fails to compile?
!error
```rust
fn eat(s: String) {
    println!("nom {s}");
}
fn main() {
    let snack = String::from("🍕");
    eat(snack);
    println!("{snack}");
}
```
= 7
? `eat(snack)` moved the String into `eat`, which dropped it. Line 7 is a "borrow of moved value: `snack`".

@compiles
```rust
fn show(n: i32) {
    println!("{n}");
}
fn main() {
    let lives = 3;
    show(lives);
    show(lives);
}
```
= yes
? `i32` is `Copy`, so each call gets its own copy and `lives` stays valid.

@compiles
```rust
fn take(s: String) {}
fn main() {
    let s = String::from("hi");
    take(s);
    take(s);
}
```
= no
? The first `take(s)` moves `s`. The second call uses a moved value: "use of moved value: `s`".

@explain Giving ownership back
Returning a value **moves it out** to the caller:

```rust
fn make() -> String {
    String::from("fresh") // moved out
}

fn main() {
    let s = make(); // s owns it now
    println!("{s}");
}
```

So a function can take a `String`, change it, and hand it back. Passing ownership back and forth gets tiring though... next unit, **references** (`&`) let functions use a value without taking it. 🦀

@fill Return a new String from `make`.
!compiles
```rust
fn make() -> ___ {
    String::from("🦀")
}
fn main() {
    let s = ___();
    println!("{s}");
}
```
= String
= make
~ str
~ char
~ String::new
~ new
? `make` returns an owned `String`, and calling `make()` moves it into `s`.

@choice What does this print?
!output
```rust
fn shout(mut s: String) -> String {
    s.push_str("!");
    s
}
fn main() {
    let a = String::from("hey");
    let b = shout(a);
    println!("{b}");
}
```
+ hey!
- hey
- hey!!
- It doesn't compile
? `a` moves into `shout`, which adds `!` and moves the String back out into `b`. Writing `mut s` makes the parameter mutable.

@choice What does this print?
!output
```rust
fn size(s: String)
    -> (String, usize) {
    let n = s.len();
    (s, n)
}
fn main() {
    let s = String::from("crab");
    let (s, n) = size(s);
    println!("{s} {n}");
}
```
+ crab 4
- 4 crab
- crab 0
- It doesn't compile
? `size` takes the String, measures it, and returns it together with the length in a tuple. Ownership goes in and comes back out.

@compiles
```rust
fn take(s: String) {}
fn main() {
    let s = String::from("hi");
    take(s.clone());
    println!("{s}");
}
```
= yes
? Only the clone moves into `take`. The original `s` still belongs to `main`.
