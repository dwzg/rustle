@unit traits-generics
title: Traits & generics
tagline: Write it once, use it for every type
icon: 🧩

@lesson gen-basics
title: Generics

@explain Placeholders for types
A **generic** function works for many types. `T` is a type parameter - a placeholder filled in at each call:

```rust
fn first<T>(items: &[T]) -> &T {
    &items[0]
}

fn main() {
    println!("{}", first(&[3, 1, 4]));
    println!("{}", first(&["hi"]));
}
```

The `<T>` after the name declares the parameter. Here `T` becomes `i32` in the first call and `&str` in the second.

@choice What does this print?
!output
```rust
fn last<T>(v: &[T]) -> &T {
    &v[v.len() - 1]
}

fn main() {
    let n = [1, 2, 3];
    let s = ["a", "b"];
    print!("{} ", last(&n));
    println!("{}", last(&s));
}
```
+ 3 b
- 1 a
- 3 a
- It doesn't compile
? `last` works for any element type. It returns the final element: `3` for the numbers and `b` for the strings.

@fill Complete the generic tuple flipper.
!compiles
```rust
fn flip<A, B>(p: (A, B))
    -> (___, ___) {
    (p.1, p.0)
}

fn main() {
    let p = flip((1, "crab"));
    println!("{:?}", p);
}
```
= B
= A
~ T
~ Self
~ _
? The function returns `(p.1, p.0)` - first a `B`, then an `A`. So the return type is `(B, A)`.

@explain Generic structs
Structs can be generic too. Methods need `impl<T>` to declare the `T` they use:

```rust
struct Boxy<T> {
    item: T,
}

impl<T> Boxy<T> {
    fn get(&self) -> &T {
        &self.item
    }
}
```

You already know some famous generic types: `Vec<T>`, `Option<T>` and `Result<T, E>`!

@compiles
```rust
struct Pair<T> {
    a: T,
    b: T,
}

fn main() {
    let p = Pair { a: 1, b: "two" };
}
```
= no
? Both fields use the same `T`, so they must have the same type. `1` and `"two"` don't match. Use `Pair<A, B>` for two different types.

@compiles
```rust
struct Wrap<T> {
    v: T,
}

fn main() {
    let a = Wrap { v: 5 };
    let b = Wrap { v: "five" };
    println!("{} {}", a.v, b.v);
}
```
= yes
? `a` is a `Wrap<i32>` and `b` is a `Wrap<&str>`. Each value picks its own `T`.

@bug Which line does the compiler reject first?
!error
```rust
struct Boxy<T> {
    item: T,
}

impl Boxy<T> {
    fn get(&self) -> &T {
        &self.item
    }
}

fn main() {
    let b = Boxy { item: 5 };
    println!("{}", b.get());
}
```
= 5
? `T` must be declared before it's used: write `impl<T> Boxy<T>`. Without the `<T>` after `impl`, Rust looks for a real type named `T`.

@compiles
```rust
fn bigger<T>(a: T, b: T) -> T {
    if a > b { a } else { b }
}

fn main() {
    println!("{}", bigger(3, 7));
}
```
= no
? `T` could be *any* type - even one that can't be compared! `>` needs the `PartialOrd` trait. We'll fix this with a **trait bound** soon.

@explain Zero cost 🚀
Generics don't slow your program down. The compiler stamps out a separate copy for every concrete type you use - this is called **monomorphization**:

```rust
first(&[1, 2]);     // first::<i32>
first(&["a", "b"]); // first::<&str>
```

You write the code once, the machine runs code as fast as if you'd written each version by hand.

@choice How does Rust run a generic function called with both `i32` and `f64`?
+ It compiles a separate copy for each type
- It checks the types at runtime
- It converts every value to one type
- It boxes every value on the heap
? Monomorphization: at compile time, each concrete type gets its own specialized copy. No runtime cost!

@lesson trait-basics
title: Traits

@explain Shared behavior
A **trait** describes behavior that types can share - like an interface:

```rust
trait Speak {
    fn speak(&self) -> String;
}

struct Crab;

impl Speak for Crab {
    fn speak(&self) -> String {
        String::from("click click")
    }
}
```

Each type writes its own `impl Speak for Type` block, providing every method the trait asks for.

@choice What does this print?
!output
```rust
trait Speak {
    fn talk(&self) -> &str;
}
struct Crab;
struct Cat;
impl Speak for Crab {
    fn talk(&self) -> &str { "click" }
}
impl Speak for Cat {
    fn talk(&self) -> &str { "meow" }
}
fn main() {
    println!("{}", Cat.talk());
}
```
+ meow
- click
- click meow
- It doesn't compile
? `Cat` has its own implementation of `talk`, which returns `"meow"`.

@compiles
```rust
trait Area {
    fn area(&self) -> f64;
}

struct Sq(f64);

impl Area for Sq {}

fn main() {}
```
= no
? `Area` requires an `area` method, and `Sq`'s impl doesn't provide one: "not all trait items implemented".

@fill Implement the trait for Dog.
!compiles
```rust
trait Speak {
    fn speak(&self) -> &str;
}
struct Dog;
___ Speak ___ Dog {
    fn speak(&self) -> &str { "woof" }
}
fn main() {
    println!("{}", Dog.speak());
}
```
= impl
= for
~ trait
~ struct
~ as
~ in
? The syntax is `impl Trait for Type { ... }`.

@explain Default methods
A trait can give a method a **default** body. Implementors get it for free - or override it:

```rust
trait Greet {
    fn name(&self) -> String;
    fn hi(&self) -> String {
        format!("Hi, {}", self.name())
    }
}
```

A type implementing `Greet` only *has* to write `name`. Default methods can call the other methods of the trait.

@choice What does this print?
!output
```rust
trait Greet {
    fn hi(&self) -> &str { "hello" }
}
struct A;
struct B;
impl Greet for A {}
impl Greet for B {
    fn hi(&self) -> &str { "yo" }
}
fn main() {
    println!("{} {}", A.hi(), B.hi());
}
```
+ hello yo
- hello hello
- yo yo
- It doesn't compile
? `A` keeps the default `hi`, while `B` overrides it with its own version.

@explain derive & Display
Some standard traits can be **derived** - the compiler writes the impl for you:

- `Debug` - print with `{:?}`
- `Clone` - make a copy with `.clone()`
- `PartialEq` - compare with `==` and `!=`

`Display` (for `{}`) can't be derived, you write it yourself:

```rust
use std::fmt;

#[derive(Debug, Clone, PartialEq)]
struct Pt { x: i32, y: i32 }

impl fmt::Display for Pt {
    fn fmt(
        &self,
        f: &mut fmt::Formatter,
    ) -> fmt::Result {
        let Pt { x, y } = self;
        write!(f, "({x}, {y})")
    }
}
```

@compiles
```rust
struct Gem {
    v: u8,
}

fn main() {
    let a = Gem { v: 1 };
    let b = Gem { v: 1 };
    println!("{}", a == b);
}
```
= no
? `Gem` doesn't implement `PartialEq`, so `==` isn't available. Add `#[derive(PartialEq)]` above the struct.

@choice What does this print?
!output
```rust
#[derive(Debug, Clone, PartialEq)]
struct Gem {
    v: u8,
}

fn main() {
    let a = Gem { v: 3 };
    let mut b = a.clone();
    b.v += 1;
    println!("{:?} {}", b, a == b);
}
```
+ Gem { v: 4 } false
- Gem { v: 4 } true
- Gem { v: 3 } true
- 4 false
? `b` is an independent copy, so changing it leaves `a` at 3. The derived `Debug` prints `Gem { v: 4 }`, and `a == b` is `false`.

@bug Which line doesn't compile?
!error
```rust
#[derive(Debug, PartialEq)]
struct Gem {
    v: u8,
}

fn main() {
    let a = Gem { v: 3 };
    let b = a.clone();
    println!("{}", a == b);
}
```
= 8
? `Gem` doesn't derive `Clone`, so there's no `.clone()` method. Add `Clone` to the derive list.

@order `String` implements `Clone` and `PartialEq` too. Arrange the lines so it prints `true`.
!compiles
```rust
fn main() {
    let a = String::from("🦀");
    let b = a.clone();
    let same = a == b;
    println!("{same}");
}
```
? You need `a` before cloning it, both strings before comparing them, and `same` before printing it.

@lesson trait-bounds
title: Bounds & dyn

@explain Trait bounds
A bare `T` could be anything, so you can't do much with it. A **trait bound** promises that `T` implements a trait:

```rust
use std::fmt::Display;

fn show<T: Display>(x: T) {
    println!("<{x}>");
}
```

Long bounds read better in a `where` clause. Combine traits with `+`:

```rust
fn bigger<T>(a: T, b: T) -> T
where
    T: PartialOrd + Copy,
{
    if a > b { a } else { b }
}
```

@compiles
```rust
fn bigger<T: PartialOrd>(a: T, b: T)
    -> T {
    if a > b { a } else { b }
}

fn main() {
    println!("{}", bigger(3, 7));
}
```
= yes
? With `T: PartialOrd`, the compiler knows `>` works for every `T` allowed in. It prints `7`.

@fill Add the bound with a `where` clause.
!compiles
```rust
use std::fmt::Display;

fn shout<T>(x: T)
___
    T: ___,
{
    println!("{x}!!!");
}

fn main() {
    shout("crab");
}
```
= where
= Display
~ when
~ if
~ Debug
~ Clone
? `where T: Display` lets `shout` print `x` with `{}`. `Debug` would only allow `{:?}`.

@explain impl Trait
`impl Trait` is a handy shorthand:

```rust
use std::fmt::Display;

fn show(x: impl Display) {
    println!("<{x}>");
}

fn label(n: u32) -> impl Display {
    format!("#{n}")
}
```

- As an **argument**: "any type that implements `Display`" - the same as a `T: Display` generic
- As a **return** type: "some type that implements `Display`". The caller doesn't get to know which - but it must be **one** concrete type

@choice What does this print?
!output
```rust
use std::fmt::Display;

fn wrap(x: impl Display) -> String {
    format!("[{x}]")
}

fn main() {
    print!("{}", wrap(1));
    println!("{}", wrap("a"));
}
```
+ [1][a]
- [1, a]
- 1a
- It doesn't compile
? `wrap` accepts anything that implements `Display`, numbers and strings alike, and puts it in brackets.

@compiles
```rust
use std::fmt::Display;

fn pick(b: bool) -> impl Display {
    if b { 5 } else { "five" }
}

fn main() {
    println!("{}", pick(true));
}
```
= no
? A returned `impl Trait` must be a single concrete type, but here it's either an `i32` or a `&str`. To return different types, use `Box<dyn Display>`.

@explain dyn Trait
Generics pick the type at **compile time**. A **trait object**, `dyn Trait`, picks at **runtime** - so different types can live side by side:

```rust
fn main() {
    let z: Vec<Box<dyn Speak>> = vec![
        Box::new(Crab),
        Box::new(Duck),
    ];
    for a in &z {
        println!("{}", a.speak());
    }
}
```

A `dyn Trait` always sits behind a pointer like `Box<dyn Trait>` or `&dyn Trait`. Each method call is looked up at runtime - a tiny cost for a lot of flexibility.

@choice What does this print?
!output
```rust
trait Speak { fn hi(&self) -> &str; }
struct Crab;
struct Duck;
impl Speak for Crab {
    fn hi(&self) -> &str { "click" }
}
impl Speak for Duck {
    fn hi(&self) -> &str { "quack" }
}
fn main() {
    let z: Vec<Box<dyn Speak>> = vec![
        Box::new(Duck),
        Box::new(Crab),
    ];
    for a in &z {
        print!("{} ", a.hi());
    }
}
```
+ quack click
- click quack
- quack quack
- It doesn't compile
? The vector holds a `Duck` and a `Crab` as `Box<dyn Speak>`. Each `hi` call runs the right implementation, in vector order.

@compiles
```rust
struct Crab;
struct Duck;

fn main() {
    let v = vec![
        Box::new(Crab),
        Box::new(Duck),
    ];
}
```
= no
? A `Vec` holds one type, and `Box<Crab>` isn't `Box<Duck>`. To mix them, give them a shared trait and write `Vec<Box<dyn Trait>>`.

@bug Which line doesn't compile?
!error
```rust
use std::fmt::Display;

struct Crab;

fn show(x: impl Display) {
    println!("{x}");
}

fn main() {
    show(42);
    show(Crab);
}
```
= 11
? `show` requires `Display`. `42` qualifies, but `Crab` doesn't implement it, so the call on line 11 is rejected.

@choice When is `Box<dyn Trait>` the right tool instead of generics?
+ To store different types in one collection
- To make method calls faster
- To avoid writing a trait
- Whenever you use a struct
? Generics are resolved at compile time to one type each. `dyn Trait` lets one `Vec` (or variable) hold values of *different* types sharing a trait.

@order Arrange the lines so the program prints 42.
!compiles
```rust
fn main() {
    let n = 42;
    let d: &dyn ToString = &n;
    let s = d.to_string();
    println!("{s}");
}
```
? `d` is a `&dyn ToString` trait object pointing at `n`. Calling `to_string` on it gives `"42"`.
