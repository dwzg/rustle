@unit borrowing
title: Borrowing
tagline: Lend your values without giving them away
icon: 🤝

@lesson borrow-refs
title: References

@explain Borrow, don't take
Passing a `String` to a function **moves** it, and then it's gone. Often you just want to *look* at a value. That's what a **reference** is for:

```rust
fn count(s: &String) -> usize {
    s.len()
}

fn main() {
    let name = String::from("Ferris");
    let n = count(&name);
    println!("{name}: {n}");
}
```

- `&name` creates a reference to `name`
- `&String` is the type "reference to a `String`"
- Creating a reference is called **borrowing**. The owner keeps the value, the borrower just gets to look 🦀

@choice What does this print?
!output
```rust
fn shout(s: &String) {
    print!("{s}! ");
}

fn main() {
    let word = String::from("hi");
    shout(&word);
    shout(&word);
}
```
+ hi! hi!
- hi!
- It doesn't compile: `word` was moved
- &word! &word!
? `shout` only borrows `word`, so it can be lent out as many times as you like. A reference prints just like the value it points to.

@compiles
```rust
fn shout(s: String) {
    println!("{s}!");
}

fn main() {
    let word = String::from("hi");
    shout(word);
    shout(word);
}
```
= no
? `shout` takes a `String` **by value**, so the first call moves `word` into it. The second call is a "use of moved value". Taking `&String` and calling `shout(&word)` fixes it.

@fill Borrow the vector instead of moving it.
!compiles
```rust
fn size(v: ___Vec<i32>) -> usize {
    v.len()
}

fn main() {
    let v = vec![1, 2, 3];
    let n = size(___v);
    println!("{n} {v:?}");
}
```
= &
= &
~ *
~ mut
~ ref
? The parameter type is `&Vec<i32>` and the call passes `&v`. Since `v` is only borrowed, `main` can still print it afterwards.

@compiles
```rust
fn main() {
    let s = String::from("crab");
    let a = &s;
    let b = &s;
    println!("{a} {b} {s}");
}
```
= yes
? You can have as many shared `&` references as you like at the same time. Everyone is only reading, so nothing can go wrong. Prints `crab crab crab`.

@explain No dangling references
A reference must never outlive the value it points to. The compiler (the **borrow checker**) checks this for you:

```rust
fn main() {
    let r;
    {
        let x = 5;
        r = &x;
    } // x is dropped here
    println!("{r}"); // ❌
}
```

`r` would point at memory that's already been freed, a **dangling reference**. In C that's a crash waiting to happen. In Rust it simply doesn't compile: "`x` does not live long enough".

How long a reference is valid is called its **lifetime**. You'll learn to write those out yourself in a later unit.

@bug Which line does the compiler complain about?
!error
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
= 5
? rustc points at the borrow `&x`: "`x` does not live long enough". `x` is dropped at the inner `}`, but `r` is still used on the last line.

@compiles
```rust
fn make() -> &String {
    let s = String::from("hi");
    &s
}

fn main() {
    let r = make();
    println!("{r}");
}
```
= no
? `s` is dropped when `make` returns, so `&s` would dangle. rustc reports "missing lifetime specifier": the returned reference has nothing it could validly borrow from. The fix is to return the `String` itself and move ownership out.

@choice What does `&x` do?
+ Borrows `x` without taking ownership
- Copies `x` into a new variable
- Moves `x` into the new variable
- Makes `x` mutable
? `&x` creates a reference: a pointer to `x` that the compiler guarantees is valid. `x` keeps its owner.

@lesson borrow-mut
title: Mutable references

@explain &mut
A plain `&` reference is read-only. To let someone **change** your value, lend it with `&mut`:

```rust
fn add_one(n: &mut i32) {
    *n += 1;
}

fn main() {
    let mut score = 1;
    add_one(&mut score);
    println!("{score}"); // 2
}
```

- The variable itself must be `let mut`
- `*n` **dereferences** `n`: "the value `n` points at"
- Method calls deref automatically, so `s.push('!')` works on a `&mut String` without any `*`

@fill Make `double` change the caller's number.
!compiles
```rust
fn double(n: ___ i32) {
    ___n *= 2;
}

fn main() {
    let mut x = 21;
    double(&mut x);
    println!("{x}");
}
```
= &mut
= *
~ &
~ mut
~ ref
? The parameter must be a mutable reference, `&mut i32`. To change the number behind it, deref with `*n`. Plain `n *= 2` would try to multiply the reference itself.

@choice What does this print?
!output
```rust
fn grow(s: &mut String) {
    s.push_str("!!");
}

fn main() {
    let mut s = String::from("Hey");
    grow(&mut s);
    grow(&mut s);
    println!("{s}");
}
```
+ Hey!!!!
- Hey!!
- Hey
- It doesn't compile
? Each call borrows `s` mutably only while the call runs, so two calls in a row are fine. Each adds `!!`.

@compiles
```rust
fn main() {
    let x = 5;
    let r = &mut x;
    *r += 1;
    println!("{x}");
}
```
= no
? You can't borrow `x` as mutable because `x` itself isn't declared `mut`. Change it to `let mut x = 5;`.

@explain The golden rule
At any moment, a value can have **either**:

- any number of shared references `&T`, **or**
- exactly **one** mutable reference `&mut T`

Never both at once. Readers never see data change under their feet, and data races become compile errors.

A borrow lasts only until its **last use**, not until the end of the block:

```rust
fn main() {
    let mut s = String::from("hi");
    let a = &s;
    println!("{a}"); // last use of a
    let m = &mut s; // ✅ fine
    m.push('!');
}
```

@bug Which line breaks the golden rule?
!error
```rust
fn main() {
    let mut s = String::from("hi");
    let r = &s;
    let m = &mut s;
    m.push('!');
    println!("{r}");
}
```
= 4
? `r` is still used on the last line, so the shared borrow is alive when line 4 asks for `&mut s`. rustc: "cannot borrow `s` as mutable because it is also borrowed as immutable". Printing `r` before line 4 would fix it.

@compiles
```rust
fn main() {
    let mut n = 0;
    let a = &mut n;
    let b = &mut n;
    *a += 1;
    *b += 1;
}
```
= no
? "cannot borrow `n` as mutable more than once at a time". `a` is still used after `b` is created, so two `&mut` borrows overlap.

@compiles
```rust
fn main() {
    let mut n = 0;
    let a = &mut n;
    *a += 1;
    let b = &mut n;
    *b += 1;
    println!("{n}");
}
```
= yes
? `a` is last used before `b` is created, so the two borrows never overlap. Prints `2`.

@choice Which of these can exist at the same time?
+ Three `&s` references
- One `&s` and one `&mut s`
- Two `&mut s` references
- Two `&mut s`, if you promise to be careful 🦀
? Many readers **or** one writer. The compiler doesn't accept promises!

@order Order the lines so the program prints 15.
!compiles
```rust
fn main() {
    let mut x = 10;
    let r = &mut x;
    *r += 5;
    println!("{x}");
}
```
? Create the mutable borrow, use it, and only then read `x`. Printing `x` while `r` is still going to be used would break the golden rule.

@lesson borrow-slices
title: Slices

@explain A piece of the pie 🍰
A **slice** is a reference to a *part* of a collection. For strings it has the type `&str`:

```rust
fn main() {
    let s = String::from("hi crab");
    let hi = &s[0..2];
    let crab = &s[3..];
    println!("{crab} {hi}");
}
```

- `a..b` is a range: start included, end **excluded**
- `..b` starts at 0, `a..` runs to the end, `..` is everything
- `a..=b` includes the end
- String literals like `"hi"` are `&str` too

String slice indices count **bytes**. Slicing through the middle of a multi-byte character like `é` panics.

@choice What does this print?
!output
```rust
fn main() {
    let s = String::from("rustacean");
    println!("{}", &s[0..4]);
}
```
+ rust
- rusta
- ust
- rustacean
? `0..4` covers indices 0, 1, 2 and 3, four bytes: `rust`. The end of a range is excluded.

@fill Slice off the first two letters.
!compiles
```rust
fn main() {
    let s = String::from("ferris");
    let tail = &s[___];
    println!("{tail}"); // rris
}
```
= 2..
~ 3..
~ 1..
~ ..2
? `2..` runs from index 2 to the end: `rris`. `..2` would be the first two letters, `fe`.

@explain &str and &[T]
Arrays and vectors slice the same way, giving `&[T]`:

```rust
fn sum(xs: &[i32]) -> i32 {
    let mut t = 0;
    for x in xs {
        t += x;
    }
    t
}
```

`sum(&v)`, `sum(&v[1..])` and `sum(&array)` all work.

Pro tip: write `&str` parameters instead of `&String`. A `&String` turns into a `&str` automatically, so one function accepts both literals and `String`s.

@choice What does this print?
!output
```rust
fn sum(xs: &[i32]) -> i32 {
    let mut t = 0;
    for x in xs {
        t += x;
    }
    t
}

fn main() {
    let v = vec![1, 2, 3, 4, 5];
    println!("{}", sum(&v[1..4]));
}
```
+ 9
- 10
- 14
- 6
? `&v[1..4]` is the slice `[2, 3, 4]` (indices 1, 2 and 3), and `2 + 3 + 4 = 9`.

@compiles
```rust
fn greet(name: &String) {
    println!("Hi, {name}!");
}

fn main() {
    greet("Ferris");
}
```
= no
? Mismatched types: `"Ferris"` is a `&str`, not a `&String`. Declare the parameter as `name: &str` and it accepts both.

@compiles
```rust
fn greet(name: &str) {
    println!("Hi, {name}!");
}

fn main() {
    let s = String::from("Ferris");
    greet(&s);
    greet("Corro");
}
```
= yes
? `&s` is a `&String`, which Rust automatically turns into a `&str` (deref coercion). The literal already is a `&str`.

@bug Which line does rustc reject?
!error
```rust
fn main() {
    let mut s = String::from("hey");
    let word = &s[..2];
    s.clear();
    println!("{word}");
}
```
= 4
? `word` borrows part of `s` and is still used on line 5. `clear` needs `&mut s`, which isn't allowed while `word` is alive. Without this rule, `word` would point at text that was just erased!

@compiles
```rust
fn main() {
    let s = String::from("hi");
    let t = &s[0..10];
    println!("{t}");
}
```
= yes
? It compiles, but it **panics** when it runs: byte index 10 is out of range for `"hi"`. The compiler doesn't track string lengths, so slice bounds are checked at runtime.

@choice `let a = [1, 2, 3, 4];` What is the type of `&a[1..3]`?
+ &[i32]
- [i32; 2]
- &[i32; 4]
- Vec<i32>
? Slicing an array gives a `&[i32]`: a reference to some elements plus a length (2 here). The length isn't part of the type.
