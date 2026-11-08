@unit structs
title: Structs
tagline: Bundle data into your own types
icon: 🧱

@lesson struct-basics
title: Defining structs

@explain Your own type
A **struct** groups related values into one named type. Each value is a **field**:

```rust
struct Crab {
    name: String,
    legs: u32,
}

fn main() {
    let c = Crab {
        name: String::from("Ferris"),
        legs: 10,
    };
    println!("{} {}", c.name, c.legs);
}
```

- Create an **instance** with `Name { field: value, ... }`
- Read fields with a dot: `c.name`
- Every field needs a value, in any order

@choice What does this print?
!output
```rust
struct Point {
    x: i32,
    y: i32,
}

fn main() {
    let mut p = Point { x: 1, y: 2 };
    p.x += 10;
    println!("{} {}", p.x, p.y);
}
```
+ 11 2
- 1 2
- 11 12
- It doesn't compile
? `p` is `mut`, so its fields can change. Only `x` gets `+ 10`.

@compiles
```rust
struct Counter {
    n: u32,
}

fn main() {
    let c = Counter { n: 0 };
    c.n = 1;
    println!("{}", c.n);
}
```
= no
? "cannot assign to `c.n`, as `c` is not declared as mutable". Mutability belongs to the whole binding: `let mut c`. You can't mark a single field `mut`.

@compiles
```rust
struct Pet {
    name: String,
    age: u8,
}

fn main() {
    let p = Pet {
        name: String::from("Rex"),
    };
    println!("{}", p.name);
}
```
= no
? "missing field `age`". When you create a struct, every field must get a value. No silent defaults here!

@explain Shortcuts
**Field init shorthand**: if a variable has the same name as the field, write it once:

```rust
fn crab(name: String) -> Crab {
    Crab { name, legs: 10 }
}
```

**Struct update syntax**: `..other` fills in all remaining fields from another instance:

```rust
let b = Crab {
    name: String::from("Corro"),
    ..a
};
```

`..a` must come **last**. It moves (or copies) the fields it takes, so if it takes a `String`, that part of `a` is moved away.

@fill Use field init shorthand.
!compiles
```rust
struct User {
    name: String,
    level: u32,
}

fn make(name: String) -> User {
    User { ___, level: 1 }
}

fn main() {
    let u = make(String::from("ana"));
    println!("{}", u.level);
}
```
= name
~ name = name
~ self.name
~ &name
~ ..name
? The parameter `name` matches the field `name`, so `User { name, ... }` is short for `User { name: name, ... }`.

@choice What does this print?
!output
```rust
struct Size {
    w: u32,
    h: u32,
}

fn main() {
    let a = Size { w: 4, h: 3 };
    let b = Size { w: 9, ..a };
    println!("{} {}", b.w, b.h);
}
```
+ 9 3
- 4 3
- 9 9
- It doesn't compile
? `b` sets `w` itself and takes the rest (`h: 3`) from `a`.

@bug Which line won't compile?
!error
```rust
struct Size {
    w: u32,
    h: u32,
}

fn main() {
    let a = Size { w: 1, h: 2 };
    let b = Size { ..a, w: 5 };
}
```
= 8
? The `..a` base must be the **last** thing in the braces. Write `Size { w: 5, ..a }`.

@compiles
```rust
struct User {
    name: String,
    lvl: u32,
}

fn main() {
    let a = User {
        name: String::from("Ana"),
        lvl: 1,
    };
    let b = User { lvl: 2, ..a };
    println!("{}", a.name);
}
```
= no
? `..a` **moved** the `String` in `a.name` into `b`. Using `a.name` afterwards is a "borrow of moved value". (`a.lvl` would still be fine: `u32` is `Copy`.)

@lesson struct-kinds
title: Tuple structs & Debug

@explain Tuple and unit structs
Fields don't always need names. **Tuple structs** use positions:

```rust
struct Rgb(u8, u8, u8);
struct Meters(f64);
struct Marker;

fn main() {
    let c = Rgb(255, 128, 0);
    let d = Meters(4.5);
    let _m = Marker;
    println!("{} {}", c.0, d.0);
}
```

- Access fields with `.0`, `.1`, ...
- A one-field wrapper like `Meters` is a **newtype**: same data as `f64`, but a distinct type, so you can't mix it up with something else
- `Marker` is a **unit struct**, with no fields at all. Handy later with traits

@choice What does this print?
!output
```rust
struct Pair(i32, i32);

fn main() {
    let p = Pair(3, 7);
    println!("{}", p.1 - p.0);
}
```
+ 4
- -4
- 3
- 37
? Tuple struct fields count from zero: `p.0` is 3 and `p.1` is 7, so `7 - 3 = 4`.

@compiles
```rust
struct Meters(f64);
struct Feet(f64);

fn show(m: Meters) {
    println!("{}", m.0);
}

fn main() {
    show(Feet(3.0));
}
```
= no
? Mismatched types: expected `Meters`, found `Feet`. They wrap the same `f64` but are different types. A unit mix-up like this once cost NASA a Mars probe 🚀

@explain Debug printing
`println!("{}", x)` needs the `Display` trait, and your structs don't have it. But you can **derive** `Debug` for free and print with `{:?}`:

```rust
#[derive(Debug)]
struct Crab {
    name: String,
    legs: u32,
}
```

With `let c = Crab { ... };`:

- `println!("{c:?}")` prints `Crab { name: "Ferris", legs: 10 }`
- `println!("{c:#?}")` pretty-prints it, one field per line

```text
Crab {
    name: "Ferris",
    legs: 10,
}
```

@compiles
```rust
struct Dot {
    x: i32,
}

fn main() {
    let d = Dot { x: 1 };
    println!("{:?}", d);
}
```
= no
? "`Dot` doesn't implement `Debug`". Add `#[derive(Debug)]` on the line above `struct Dot`.

@choice What does this print?
!output
```rust
#[derive(Debug)]
struct Pt(i32, i32);

fn main() {
    let p = Pt(1, -2);
    println!("{:?}", p);
}
```
+ Pt(1, -2)
- (1, -2)
- Pt { 0: 1, 1: -2 }
- 1 -2
? Derived `Debug` for a tuple struct looks just like the code that builds it: `Pt(1, -2)`.

@fill Make the score printable.
!compiles
```rust
___
struct Score {
    pts: u32,
}

fn main() {
    let s = Score { pts: 7 };
    println!("{___}");
}
```
= #[derive(Debug)]
= s:?
~ #[derive(Display)]
~ #derive(Debug)
~ s
~ s:#
? `#[derive(Debug)]` generates the `Debug` impl, and `{s:?}` prints with it. `Display` can't be derived, and plain `{s}` needs `Display`.

@bug Which line doesn't compile?
!error
```rust
#[derive(Debug)]
struct Cat {
    lives: u8,
}

fn main() {
    let c = Cat { lives: 9 };
    println!("{c:?}");
    println!("{c}");
}
```
= 9
? `{c}` uses `Display`, which `Cat` doesn't implement. Deriving `Debug` only unlocks `{:?}` and `{:#?}`.

@compiles
```rust
#[derive(Debug)]
struct Nothing;

fn main() {
    let n = Nothing;
    println!("{n:?}");
}
```
= yes
? A unit struct is created by just writing its name. Its `Debug` output is the name too: `Nothing`.

@lesson struct-methods
title: Methods

@explain impl blocks
Functions that belong to a type go in an `impl` block:

```rust
struct Rect {
    w: u32,
    h: u32,
}

impl Rect {
    fn area(&self) -> u32 {
        self.w * self.h
    }
}

fn main() {
    let r = Rect { w: 3, h: 4 };
    println!("{}", r.area()); // 12
}
```

- `&self` is short for `self: &Self`, and `Self` means "this type", here `Rect`
- Call methods with a dot. Rust borrows `r` for you automatically

@choice What does this print?
!output
```rust
struct Counter { n: u32 }

impl Counter {
    fn bump(&mut self) {
        self.n += 1;
    }
}

fn main() {
    let mut c = Counter { n: 0 };
    c.bump();
    c.bump();
    println!("{}", c.n);
}
```
+ 2
- 0
- 1
- It doesn't compile
? `bump` takes `&mut self`, so it changes `c` in place. Two bumps: `2`.

@explain Three kinds of self
The first parameter decides what a method may do:

- `&self`: read only
- `&mut self`: modify in place (the caller needs `let mut`)
- `self`: take **ownership**, consuming the value. Good for "finish" or "convert" methods

Functions in `impl` **without** `self` are **associated functions**. Call them with `::`, like `String::from`. Constructors are usually named `new`:

```rust
impl Rect {
    fn new(w: u32, h: u32) -> Self {
        Self { w, h }
    }
}

let r = Rect::new(3, 4);
```

@compiles
```rust
struct Counter { n: u32 }

impl Counter {
    fn bump(&mut self) {
        self.n += 1;
    }
}

fn main() {
    let c = Counter { n: 0 };
    c.bump();
}
```
= no
? `bump` needs `&mut self`, but `c` isn't `mut`: "cannot borrow `c` as mutable, as it is not declared as mutable".

@fill Finish the constructor and the method.
!compiles
```rust
struct Dog { name: String }

impl Dog {
    fn new(name: &str) -> ___ {
        let name = name.to_string();
        Self { name }
    }
    fn bark(___) {
        print!("{}: woof", self.name);
    }
}

fn main() {
    let d = Dog::new("Rex");
    d.bark();
}
```
= Self
= &self
~ &Self
~ &mut self
~ this
? `new` returns a fresh `Self` (a `Dog`). `bark` only reads `self.name`, so `&self` is enough. `&mut self` would fail, since `d` isn't `mut`.

@order Put the constructor together.
```rust
impl Crab {
    fn new(name: String) -> Self {
        Self { name, legs: 10 }
    }
}
```
? The `impl` block wraps the function, the function wraps its body, and the closing braces come last.

@compiles
```rust
struct Ticket { id: u32 }

impl Ticket {
    fn redeem(self) -> u32 {
        self.id
    }
}

fn main() {
    let t = Ticket { id: 7 };
    let a = t.redeem();
    let b = t.redeem();
}
```
= no
? `redeem` takes `self` by value, so the first call **moves** `t` into it. The second call is a "use of moved value". A ticket can only be redeemed once 🎟️

@bug Which line doesn't compile?
!error
```rust
struct Rect { w: u32, h: u32 }

impl Rect {
    fn area(&self) -> u32 {
        w * h
    }
}

fn main() {
    let r = Rect { w: 2, h: 3 };
    println!("{}", r.area());
}
```
= 5
? Fields aren't in scope by themselves inside a method: "cannot find value `w` in this scope". Write `self.w * self.h`.

@compiles
```rust
struct Rect { w: u32, h: u32 }

impl Rect {
    fn square(s: u32) -> Self {
        Self { w: s, h: s }
    }
    fn area(&self) -> u32 {
        self.w * self.h
    }
}

fn main() {
    let r = Rect::square(3);
    println!("{}", r.area());
}
```
= yes
? `Rect::square` is an associated function (no `self`), called with `::`. It returns a `Rect`, and then `.area()` borrows it. Prints `9`.

@choice How do you call `fn new() -> Self` on `Robot`?
+ Robot::new()
- Robot.new()
- new Robot()
- Robot::new
? Associated functions are called with `::` on the type. There's no `new` keyword in Rust: `new` is just a naming convention.
