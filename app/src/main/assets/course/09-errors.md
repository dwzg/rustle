@unit errors
title: Error handling
tagline: When things go wrong - and how Rust makes you deal with it
icon: 🚨

@lesson err-result
title: Panic or Result?

@explain Two kinds of trouble
Rust splits errors into two kinds:

- **Unrecoverable**: `panic!` stops the program with a message. Use it for bugs - "this should never happen"
- **Recoverable**: return a `Result<T, E>` and let the caller decide what to do

```rust
enum Result<T, E> {
    Ok(T),  // success, with a value
    Err(E), // failure, with an error
}
```

`Result`, `Ok` and `Err` are in the prelude, so you can use them directly.

@compiles
```rust
fn main() {
    let lives = 0;
    if lives == 0 {
        panic!("out of lives!");
    }
    println!("still going");
}
```
= yes
? It compiles just fine - `panic!` is a runtime crash, not a compile error. When run, Ferris stops with the message "out of lives!".

@choice What does this print?
!output
```rust
fn half(n: u8) -> Result<u8, String> {
    if n % 2 == 0 {
        Ok(n / 2)
    } else {
        Err(format!("{n} is odd"))
    }
}

fn main() {
    match half(7) {
        Ok(h) => println!("half={h}"),
        Err(e) => println!("{e}!"),
    }
}
```
+ 7 is odd!
- half=3
- Err(7 is odd)!
- It panics
? 7 is odd, so `half` returns `Err(...)` and the `Err` arm runs. Nothing panics - the error is just a value.

@explain Results everywhere
Lots of the standard library returns `Result`. For example, `parse` turns text into a number - which can fail:

```rust
fn main() {
    let r = "42".parse::<i32>();
    match r {
        Ok(n) => println!("got {n}"),
        Err(e) => println!("bad {e}"),
    }
}
```

The `::<i32>` ("turbofish" 🐟) says which type to parse into. Its error type here is `std::num::ParseIntError`.

@fill Complete the function.
!compiles
```rust
fn check(n: i32) -> ___<i32, String> {
    if n > 0 {
        ___(n)
    } else {
        ___("n <= 0".to_string())
    }
}

fn main() {
    println!("{:?}", check(5));
}
```
= Result
= Ok
= Err
~ Option
~ Some
~ None
~ Error
? A function that can fail returns `Result<T, E>`: `Ok(value)` on success, `Err(error)` on failure. `Option`/`Some`/`None` is for values that may be *missing*.

@compiles
```rust
fn main() {
    let n: i32 = "5".parse();
    println!("{}", n + 1);
}
```
= no
? `parse` returns a `Result<i32, _>`, not a plain `i32`. You must handle the `Err` case first.

@compiles
```rust
fn main() {
    let r = Ok(5);
    println!("{r:?}");
}
```
= no
? From `Ok(5)` Rust knows `T` is a number, but nothing says what the error type `E` is. "Type annotations needed" - e.g. `let r: Result<i32, String> = Ok(5);`.

@choice What does this print?
!output
```rust
fn main() {
    let r = "7".parse::<u8>();
    println!("{r:?}");
}
```
+ Ok(7)
- 7
- Some(7)
- Err(7)
? Parsing succeeded, so `r` is `Ok(7)`, and `{:?}` shows the `Ok` wrapper too.

@bug Which line does the compiler complain about?
!error
```rust
fn main() {
    let r: Result<u8, String> = Ok(3);
    match r {
        Ok(n) => println!("{n}"),
    }
}
```
= 3
? `match` must be exhaustive, and the `Err(_)` case isn't covered. Rust won't let you forget about errors! 🦀

@order Arrange the lines so the program prints 12.
!compiles
```rust
fn main() {
    let r = "12".parse::<u8>();
    if let Ok(n) = r {
        println!("{n}");
    }
}
```
? Parse first, then `if let Ok(n)` unpacks the success case. Errors are simply skipped here.

@lesson err-question
title: unwrap, expect & ?

@explain unwrap & expect
Sometimes you just want the value:

```rust
fn main() {
    let a: i32 = "5".parse().unwrap();
    let b: i32 = "x"
        .parse()
        .expect("need a number");
}
```

- `unwrap()` returns the `Ok` value - or **panics** on `Err`
- `expect(msg)` does the same, but panics with your message. Future-you will thank you
- Both also work on `Option` (`None` panics)

Great for quick prototypes and tests - or when failure truly can't happen.

@choice What does this print?
!output
```rust
fn main() {
    let n: u8 = "21".parse().unwrap();
    println!("{}", n * 2);
}
```
+ 42
- Ok(42)
- 2121
- It panics
? `"21"` parses fine, so `unwrap` hands back the plain `u8` 21, and `21 * 2 = 42`.

@compiles
```rust
fn main() {
    let n: i32 = "abc"
        .parse()
        .expect("not a number");
    println!("{n}");
}
```
= yes
? It compiles! It will **panic** at runtime with "not a number", but the types are all fine.

@explain The ? operator
Writing a `match` for every `Result` gets old. The `?` operator does it for you:

```rust
let n = match s.parse() {
    Ok(n) => n,
    Err(e) => return Err(e),
};
// ...is the same as:
let n = s.parse()?;
```

On `Ok(v)` you get `v`; on `Err(e)` the function **returns early** with that error.

So `?` only works inside a function that returns `Result` (or `Option`, when used on an `Option`).

@fill Use `?` to pass parse errors up to the caller.
!compiles
```rust
use std::num::ParseIntError;

fn dbl(s: &str)
    -> Result<i32, ParseIntError> {
    let n: i32 = s.parse()___;
    ___(n * 2)
}

fn main() {
    println!("{:?}", dbl("4"));
}
```
= ?
= Ok
~ !
~ .unwrap
~ Some
~ return
? `s.parse()?` gives the number or returns the error early. The success value still needs wrapping: `Ok(n * 2)`.

@compiles
```rust
fn main() {
    let n: i32 = "5".parse()?;
    println!("{n}");
}
```
= no
? This `main` returns `()`, so `?` has nowhere to send an error. "The `?` operator can only be used in a function that returns `Result` or `Option`".

@choice What does this print?
!output
```rust
use std::num::ParseIntError;

fn sum(a: &str, b: &str)
    -> Result<i32, ParseIntError> {
    let x: i32 = a.parse()?;
    let y: i32 = b.parse()?;
    Ok(x + y)
}

fn main() {
    let a = sum("2", "3");
    let b = sum("2", "x");
    println!("{a:?} {}", b.is_ok());
}
```
+ Ok(5) false
- Ok(5) true
- 5 false
- It panics
? `sum("2", "3")` is `Ok(5)`. For `"x"`, the second `?` returns the error early, so `b` is an `Err` and `is_ok()` is `false`.

@explain ? in main (and on Option)
`main` itself may return a `Result`. Then `?` works there too:

```rust
fn main() -> Result<(), String> {
    let n = lvl(3)?;
    println!("{n}");
    Ok(())
}
```

If `main` returns an `Err`, Rust prints `Error: ` plus the error's `Debug` form and exits with a failure code. Don't forget the final `Ok(())`!

`?` also works on an `Option` inside a function that returns `Option`:

```rust
fn first(s: &str) -> Option<char> {
    let c = s.chars().next()?;
    Some(c.to_ascii_uppercase())
}
```

@compiles
```rust
fn lvl(n: u8) -> Result<u8, String> {
    if n > 9 {
        Err(format!("{n} > 9"))
    } else {
        Ok(n)
    }
}

fn main() -> Result<(), String> {
    let n = lvl(3)?;
    println!("level {n}");
    Ok(())
}
```
= yes
? `main` returns `Result<(), String>` and `lvl` fails with a `String`, so `?` fits perfectly. It prints `level 3`.

@compiles
```rust
fn main() -> Result<(), String> {
    let r: Result<u8, String> = Ok(4);
    println!("{}", r?);
}
```
= no
? `main` promises a `Result`, but its body ends with a statement, which means it returns `()`. Add `Ok(())` as the last line.

@bug Which line doesn't compile?
!error
```rust
fn hp(s: &str) -> i32 {
    let n: i32 = s.parse()?;
    n * 10
}

fn main() {
    println!("{}", hp("9"));
}
```
= 2
? `hp` returns a plain `i32`, so `?` can't return an error from it. Change the return type to `Result<i32, ParseIntError>`.

@lesson err-custom
title: Your own errors

@explain Custom error enums
A `String` error is fine for a quick hack, but an **enum** lets callers know exactly what went wrong - and `match` on it:

```rust
#[derive(Debug)]
enum Oops {
    Empty,
    Long(usize),
}

fn chk(s: &str) -> Result<(), Oops> {
    if s.is_empty() {
        return Err(Oops::Empty);
    }
    Ok(())
}
```

`#[derive(Debug)]` lets you print it with `{:?}` - and `main` needs it if it returns `Result<_, Oops>`.

@choice What does this print?
!output
```rust
#[derive(Debug)]
enum Oops { Empty, Long(usize) }

fn chk(s: &str) -> Result<(), Oops> {
    match s.len() {
        0 => Err(Oops::Empty),
        1..=5 => Ok(()),
        n => Err(Oops::Long(n)),
    }
}

fn main() {
    println!("{:?}", chk("crabby"));
}
```
+ Err(Long(6))
- Err(Oops::Long(6))
- Long(6)
- Ok(())
? `"crabby"` has 6 bytes, which hits the `n` arm. The derived `Debug` shows the variant name without the `Oops::` prefix.

@explain Nice messages with Display
`Debug` is for programmers. For users, implement `std::fmt::Display` - then `{}` works and so does `.to_string()`:

```rust
use std::fmt;

impl fmt::Display for Oops {
    fn fmt(
        &self,
        f: &mut fmt::Formatter,
    ) -> fmt::Result {
        match self {
            Oops::Empty => {
                write!(f, "no name!")
            }
            Oops::Long(_) => {
                write!(f, "too long!")
            }
        }
    }
}
```

`write!` works like `format!`, but writes into the formatter `f`.

@fill Give the error a friendly message.
!compiles
```rust
use std::fmt;

struct Oops;

impl fmt::___ for Oops {
    fn fmt(
        &self,
        f: &mut fmt::Formatter,
    ) -> fmt::Result {
        ___!(f, "oops! 🦀")
    }
}

fn main() {
    println!("{}", Oops);
}
```
= Display
= write
~ Debug
~ println
~ format
~ print
? `{}` uses the `Display` trait, and inside `fmt` you write to the formatter with `write!(f, ...)`.

@compiles
```rust
#[derive(Debug)]
enum Oops { Empty }

fn main() {
    let e = Oops::Empty;
    println!("{e:?}");
    println!("{e}");
}
```
= no
? `{:?}` is fine thanks to `derive(Debug)`, but `{}` needs `Display` - and that can't be derived. You have to `impl fmt::Display` yourself.

@explain map_err & ok_or
`?` only passes an error along if it fits the function's error type (or converts into it via the `From` trait). These two helpers bridge the gap:

- `.map_err(f)` turns `Err(e)` into `Err(f(e))`, and leaves `Ok` alone
- `.ok_or(err)` turns an `Option` into a `Result`: `Some(v)` → `Ok(v)`, `None` → `Err(err)`

```rust
let n = s
    .parse::<u8>()
    .map_err(|_| Oops::NotNum)?;
let c = s
    .chars()
    .next()
    .ok_or(Oops::Empty)?;
```

`|_| ...` is a tiny **closure** - an inline function. More about those in a later unit!

@choice What does this print?
!output
```rust
fn main() {
    let v = vec![10, 20];
    let r = v.get(5).ok_or("nope");
    println!("{r:?}");
}
```
+ Err("nope")
- Ok(None)
- None
- Err(nope)
? `get(5)` is `None`, and `ok_or` turns that into `Err("nope")`. `{:?}` shows the string with its quotes.

@compiles
```rust
fn up(s: &str) -> Result<u8, String> {
    let n = s
        .parse::<u8>()
        .map_err(|e| e.to_string())?;
    Ok(n + 1)
}

fn main() {
    println!("{:?}", up("41"));
}
```
= yes
? `map_err` converts the `ParseIntError` into a `String`, matching the function's error type, so `?` is happy. It prints `Ok(42)`.

@bug Which line doesn't compile?
!error
```rust
fn hp(s: &str) -> Result<u8, String> {
    let n: u8 = s.parse()?;
    Ok(n)
}

fn main() {
    println!("{:?}", hp("7"));
}
```
= 2
? The parse error is a `ParseIntError`, but `hp` returns `String` errors, and `?` can't convert one into the other. Fix it with `.map_err(|e| e.to_string())?`.

@order Arrange the lines so the program prints "no!".
!compiles
```rust
fn main() {
    let v = vec![3, 1, 4];
    let r = v.get(7).ok_or("no!");
    if let Err(e) = r {
        println!("{e}");
    }
}
```
? Make the vector, turn the missing element into an `Err` with `ok_or`, then print the error inside `if let Err(e)`.
