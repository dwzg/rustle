@unit closures-iterators
title: Closures & Iterators
tagline: Tiny functions, lazy pipelines, zero overhead
icon: 🔁

@lesson iter-closures
title: Closures

@explain Functions without a name
A **closure** is an anonymous function you can store in a variable. Parameters go between pipes `| |`:

```rust
fn main() {
    let add_one = |x| x + 1;
    let add = |a: i32, b: i32| {
        a + b
    };
    println!("{}", add_one(4)); // 5
    println!("{}", add(2, 3));  // 5
}
```

- Types are usually **inferred** - annotations are optional
- A single expression needs no `{ }`

@choice What does this print?
!output
```rust
fn main() {
    let square = |n: i32| n * n;
    let twice = |n| square(n) * 2;
    println!("{}", twice(3));
}
```
+ 18
- 12
- 36
- 9
? `square(3)` is `9`, and `twice` doubles it to `18`. Closures can call other closures.

@compiles
```rust
fn main() {
    let echo = |x| x;
    let a = echo(5);
    let b = echo("hi");
    println!("{a} {b}");
}
```
= no
? The first call fixes `x` as an integer. A closure gets **one** concrete type per parameter, so passing a `&str` later is a type mismatch.

@explain Capturing the environment
Unlike `fn`s, closures can use variables from the scope around them. Rust captures each one in the gentlest way that works:

- just **reads** it → borrows `&x`
- **changes** it → borrows `&mut x`
- **moves** it out → takes ownership

```rust
fn main() {
    let bonus = 10;
    let score = |s| s + bonus;
    println!("{}", score(5)); // 15
}
```

@fill Build a counter closure that changes `count`.
!compiles
```rust
fn main() {
    let mut count = 0;
    let ___ inc = ___ count += 1;
    inc();
    inc();
    println!("{count}");
}
```
= mut
= ||
~ fn
~ |count|
~ const
~ ()
? A closure that mutates what it captures must itself be stored with `let mut`. `||` means "no parameters". This prints `2`.

@compiles
```rust
fn main() {
    let mut hp = 3;
    let hit = || hp -= 1;
    hit();
    println!("{hp}");
}
```
= no
? `hit` mutates `hp`, so calling it needs a mutable borrow of `hit` itself. Declare it as `let mut hit`.

@explain move, and the three Fn traits
Write `move` before the pipes to make a closure **take ownership** of everything it captures - handy when it must outlive the current scope (threads, coming soon!).

Every closure implements one or more of these traits, based on what it does with its captures:

- `Fn` - only reads; call it as often as you like
- `FnMut` - mutates; needs `mut`, callable many times
- `FnOnce` - gives a captured value away; callable **once**

```rust
fn twice<F: Fn()>(f: F) {
    f();
    f();
}
```

@choice `let s = String::from("🦀");` then `let give = move || s;` - which trait describes `give` best?
+ FnOnce
- Fn
- FnMut
- None, it doesn't compile
? Calling `give` hands the `String` back to the caller. After that, the closure has nothing left to give, so it can only be called once.

@compiles
```rust
fn main() {
    let s = String::from("crab");
    let give = move || s;
    let a = give();
    let b = give();
    println!("{a} {b}");
}
```
= no
? `give` moves `s` out on the first call, so it's `FnOnce`. The second call is a "use of moved value".

@bug Ferris moved `name` into the closure. Which line breaks?
!error
```rust
fn main() {
    let name = String::from("Ferris");
    let greet = move || {
        println!("Hi {name}");
    };
    greet();
    println!("{name}");
}
```
= 7
? `move` transferred ownership of `name` into `greet`. Line 7 tries to borrow a value that main no longer owns. Drop the `move` (or clone `name`) to fix it.

@order Reorder so the program prints 15.
!compiles
```rust
fn main() {
    let factor = 3;
    let triple = |x| x * factor;
    let n = triple(5);
    println!("{n}");
}
```
? `factor` must exist before the closure can capture it, and `n` must exist before it is printed.

@lesson iter-basics
title: Iterators

@explain One at a time
An **iterator** hands out values one by one. Each call to `next()` returns `Some(value)`, and `None` when it's done:

```rust
fn main() {
    let v = vec![1, 2];
    let mut it = v.iter();
    let a = it.next(); // Some(1)
    let b = it.next(); // Some(2)
    let c = it.next(); // None
}
```

The iterator must be `mut` - calling `next()` moves its position forward. `for` loops call `next()` for you.

@choice What does this print?
!output
```rust
fn main() {
    let v = vec![10, 20];
    let mut it = v.iter();
    it.next();
    let a = it.next();
    let b = it.next();
    println!("{a:?} {b:?}");
}
```
+ Some(20) None
- Some(10) Some(20)
- 20 None
- Some(20) Some(0)
? The first `next()` (whose result is ignored) used up `10`. Then comes `Some(20)`, and after that the iterator is empty: `None`.

@explain Three flavours
How you get the iterator decides what you get from it:

- `v.iter()` → `&T`: borrows the items
- `v.iter_mut()` → `&mut T`: borrows them mutably
- `v.into_iter()` → `T`: **consumes** `v`, taking ownership

`for x in &v` means `v.iter()`, `for x in &mut v` means `v.iter_mut()`, and `for x in v` means `v.into_iter()`.

@compiles
```rust
fn main() {
    let v = vec!["Ferris", "Corro"];
    for n in &v {
        println!("{n}");
    }
    println!("{}", v.len());
}
```
= yes
? `for n in &v` only borrows the vector, so `v` is still usable afterwards.

@compiles
```rust
fn main() {
    let v = vec!["Ferris", "Corro"];
    for n in v {
        println!("{n}");
    }
    println!("{}", v.len());
}
```
= no
? `for n in v` calls `into_iter()`, which **moves** the vector into the loop. Afterwards `v` is gone: "borrow of moved value".

@fill Double every number in place.
!compiles
```rust
fn main() {
    let mut v = vec![1, 2, 3];
    for x in v.___() {
        ___x *= 2;
    }
    println!("{v:?}");
}
```
= iter_mut
= *
~ iter
~ into_iter
~ &
~ mut
? `iter_mut()` gives `&mut i32` references, and `*x` dereferences one so you can change the number it points to. Prints `[2, 4, 6]`.

@explain Lazy by design 😴
Methods like `map` don't do any work by themselves. They build a new iterator that waits until something **consumes** it:

```rust
fn main() {
    let v = vec![1, 2, 3];
    // does nothing (and rustc warns!)
    v.iter().map(|x| x * 2);
}
```

Nothing happens until a `for` loop, `collect`, `sum`, ... starts pulling values through. That's what lets Rust fuse long chains into one fast loop.

@choice What does this print?
!output
```rust
fn main() {
    let v = vec![1, 2, 3];
    v.iter().map(|x| {
        println!("{x}");
    });
    println!("done");
}
```
+ done
- 1 2 3 done
- 1 then done
- Nothing at all
? The `map` iterator is created and immediately thrown away - nobody ever asks it for a value, so the closure never runs. Only `done` is printed.

@bug Which line stops this from compiling?
!error
```rust
fn main() {
    let mut v = vec![1, 2, 3];
    for x in v.iter() {
        if *x == 2 {
            v.push(4);
        }
    }
}
```
= 5
? The loop holds an immutable borrow of `v` while it runs, so `v.push(4)` can't borrow it mutably. Growing a vector mid-iteration could leave the iterator pointing at freed memory - Rust says no.

@order Walk the iterator by hand.
!compiles
```rust
fn main() {
    let v = vec!['a', 'b'];
    let mut it = v.iter();
    while let Some(c) = it.next() {
        println!("{c}");
    }
}
```
? Create the vector, then an iterator over it, then pull from it with `while let` until `next()` returns `None` - exactly what a `for` loop does behind the scenes.

@lesson iter-chains
title: Adapters & consumers

@explain Adapters: build a pipeline
**Adapters** take an iterator and return a new one. Chain as many as you like:

- `map(f)` - transform each item
- `filter(p)` - keep items where `p` is true
- `enumerate()` - pair items with their index `(i, x)`
- `zip(other)` - pair items from two iterators
- `take(n)` - stop after `n` items
- `rev()` - go backwards

```rust
fn main() {
    let v: Vec<i32> = (1..=10)
        .rev()
        .take(3)
        .collect();
    println!("{v:?}"); // [10, 9, 8]
}
```

@choice What does this print?
!output
```rust
fn main() {
    let v: Vec<i32> = (1..=6)
        .filter(|x| x % 2 == 0)
        .map(|x| x * 10)
        .collect();
    println!("{v:?}");
}
```
+ [20, 40, 60]
- [10, 30, 50]
- [2, 4, 6]
- [10, 20, 30, 40, 50, 60]
? `filter` keeps 2, 4 and 6, then `map` multiplies each by 10.

@explain Consumers: get a result
**Consumers** drive the iterator and produce a final value:

- `collect()` - build a collection
- `sum()`, `count()` - add up / count items
- `fold(init, f)` - combine into one value
- `any(p)`, `all(p)` - is `p` true for some / every item?
- `find(p)` - first matching item, as an `Option`

`collect` can build many types, so tell it which one:

```rust
let a: Vec<i32> = (1..4).collect();
let b = (1..4).collect::<Vec<_>>();
```

That `::<>` is the **turbofish** 🐟. The `_` lets Rust infer the item type.

@compiles
```rust
fn main() {
    let v = vec![1, 2, 3];
    let d = v.iter()
        .map(|x| x * 2)
        .collect();
    println!("{d:?}");
}
```
= no
? "type annotations needed": `collect` could build a `Vec`, a `HashSet`, a `String`... Write `let d: Vec<i32>` or use `collect::<Vec<_>>()`.

@fill Collect the big numbers and add them all up.
!compiles
```rust
fn main() {
    let v = vec![3, 4, 5];
    let big = v.iter()
        .filter(|x| **x > 3)
        .collect::<___>();
    let total: i32 = v.iter().___();
    println!("{big:?} {total}");
}
```
= Vec<_>
= sum
~ Vec
~ count
~ len
~ i32
? The turbofish needs a full type: `Vec<_>` (plain `Vec` is missing its item type). `sum()` adds the numbers up; `count()` would return a `usize`, not an `i32`. Prints `[4, 5] 12`.

@choice What does this print?
!output
```rust
fn main() {
    let names = ["Ferris", "Corro"];
    let ages = [7, 3, 9];
    let n = names
        .iter()
        .zip(ages.iter())
        .count();
    println!("{n}");
}
```
+ 2
- 3
- 5
- 6
? `zip` stops as soon as **either** side runs out. There are only two names, so you get two pairs.

@compiles
```rust
fn main() {
    let v = vec![1, 2, 3];
    let big = v.iter().any(|x| x > 2);
    println!("{big}");
}
```
= no
? `v.iter()` yields `&i32`, and you can't compare a reference with a plain number. Write `*x > 2` or destructure with `|&x| x > 2`.

@choice What does this print?
!output
```rust
fn main() {
    let v = vec![1, 2, 3, 4];
    let r = v
        .iter()
        .rev()
        .fold(0, |a, x| a * 10 + x);
    println!("{r}");
}
```
+ 4321
- 1234
- 10
- 24
? `rev()` visits 4, 3, 2, 1. `fold` starts at `0`, and each step shifts the total left one digit and adds the new item: 4, 43, 432, 4321.

@compiles
```rust
fn main() {
    let v = vec![4, 8, 15, 16];
    match v.iter().find(|&&x| x > 9) {
        Some(x) => println!("{x}"),
        None => println!("none"),
    }
}
```
= yes
? `find` returns an `Option`. Its closure gets a `&&i32` (a reference to an item which is itself `&i32`), so the pattern `&&x` unwraps both. Prints `15`.

@bug Which line stops this from compiling?
!error
```rust
fn main() {
    let v = vec![1, 2, 3];
    let ok = v.iter().all(|x| *x > 0);
    let n: i32 = v.iter().count();
    println!("{ok} {n}");
}
```
= 4
? `count()` always returns a `usize`. Storing it in an `i32` is a type mismatch - use `let n: usize` (or just `let n`).

@order Arrange the chain so it prints 20.
!compiles
```rust
fn main() {
    let v = vec![5, 6, 7];
    let s: usize = v
        .iter()
        .enumerate()
        .map(|(i, x)| i * x)
        .sum();
    println!("{s}");
}
```
? Start with `iter()`, then `enumerate()` creates the `(i, x)` pairs that `map` destructures, and `sum()` consumes it all at the end: 0×5 + 1×6 + 2×7 = 20.
