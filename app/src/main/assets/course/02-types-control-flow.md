@unit types-control-flow
title: Types & Control Flow
tagline: Numbers, choices and loops - teach Ferris to decide
icon: 🔀

@lesson types-scalars
title: Scalar types

@explain Integers
Rust is **statically typed**: every value has a type known at compile time. The compiler usually infers it, but you can write it after a colon.

```rust
fn main() {
    let lives: u8 = 3;
    let temp: i32 = -40;
    let big = 1_000_000;
    println!("{lives} {temp} {big}");
}
```

- `i8` `i16` `i32` `i64` `i128` are **signed** (can be negative)
- `u8` ... `u128` are **unsigned** (0 and up)
- `isize`/`usize` match the machine's pointer size
- With no other hint, integers default to `i32`
- `_` in a number is just for readability

@choice Which type can store the value -5?
+ i8
- u8
- u32
- usize
? Only the `i` types are signed. All `u` types start at 0.

@compiles
```rust
fn main() {
    let level: u8 = 256;
    println!("{level}");
}
```
= no
? A `u8` holds 0 to 255. The compiler rejects it: "literal out of range for `u8`".

@explain Overflow
What happens when a number gets too big for its type?

- In a **debug** build, the program **panics** (crashes with an error)
- In a **release** build, it silently **wraps** around
- If you want a specific behaviour, say so with methods like `wrapping_add` or `checked_add`

```rust
fn main() {
    let x: u8 = 250;
    let y = x.wrapping_add(10); // 4
    let z = x.checked_add(10); // None
}
```

`checked_add` gives back `Some(value)` or `None` if it would overflow.

@choice What does this print?
!output
```rust
fn main() {
    let hp: u8 = 255;
    let next = hp.wrapping_add(1);
    println!("{next}");
}
```
+ 0
- 256
- 255
- It panics
? Wrapping arithmetic goes around like an odometer: 255 + 1 wraps to 0.

@explain Floats, bool & char
- `f64` (the default) and `f32` are floating-point numbers
- `bool` is `true` or `false`
- `char` is a single Unicode character in **single quotes**, so `'🦀'` is a valid `char`

```rust
fn main() {
    let pi = 3.14; // f64
    let happy: bool = true;
    let crab: char = '🦀';
}
```

Watch out: dividing two **integers** throws away the remainder.

@choice What does this print?
!output
```rust
fn main() {
    println!("{}", 7 / 2);
}
```
+ 3
- 3.5
- 4
- 3.0
? Both operands are integers, so this is integer division and the `.5` is truncated. Write `7.0 / 2.0` to get `3.5`.

@fill Give each variable its type.
!compiles
```rust
fn main() {
    let c: ___ = 'R';
    let ok: ___ = false;
    let x: ___ = 2.5;
    println!("{c} {ok} {x}");
}
```
= char
= bool
= f64
~ str
~ boolean
~ Bool
~ float
~ i32
? `char` for single-quoted characters, `bool` for true/false, and `f64` for decimals. There's no `float` or `boolean` type in Rust.

@compiles
```rust
fn main() {
    let total = 2 + 1.5;
    println!("{total}");
}
```
= no
? Rust never converts between integers and floats for you: "cannot add a float to an integer". Write `2.0 + 1.5` instead.

@bug Which line stops this from compiling?
!error
```rust
fn main() {
    let a: i32 = 10;
    let b: i64 = 20;
    let sum = a + b;
    println!("{sum}");
}
```
= 4
? `a` is `i32` but `b` is `i64`. Even two integer types can't be mixed: line 4 is a "mismatched types" error. Convert explicitly, e.g. `a as i64 + b`.

@lesson types-compound
title: Tuples & arrays

@explain Tuples
A **tuple** groups a fixed number of values, which can have different types:

```rust
fn main() {
    let crab = ("Ferris", 8, '🦀');
    println!("{}", crab.1); // 8
    let (name, legs, icon) = crab;
    println!("{name} {legs} {icon}");
}
```

- Access fields with `.0`, `.1`, `.2` ...
- Or **destructure** with `let (a, b, c) = ...`
- The empty tuple `()` is called **unit** - it means "no value"

@fill Print both parts of the tuple.
!compiles
```rust
fn main() {
    let p = (3, "crabs");
    println!("{} {}", p.___, p.___);
}
```
= 0
= 1
~ [0]
~ first
~ 2
? Tuple fields are numbered from zero: `p.0` is `3` and `p.1` is `"crabs"`.

@choice What does this print?
!output
```rust
fn main() {
    let point = (4, -2);
    let (x, y) = point;
    println!("{}", x * y);
}
```
+ -8
- 8
- 2
- (4, -2)
? Destructuring binds `x = 4` and `y = -2`, so `x * y = -8`.

@compiles
```rust
fn main() {
    let t = (1, 2.0, 'c');
    println!("{}", t.3);
}
```
= no
? The tuple has three fields: `.0`, `.1` and `.2`. There is "no field `3`".

@explain Arrays
An **array** holds a fixed number of values of the **same type**:

```rust
fn main() {
    let odds = [1, 3, 5];
    let zeros = [0; 4]; // four 0s
    let ab: [char; 2] = ['a', 'b'];
    println!("{}", odds[0]); // 1
    println!("{}", zeros.len()); // 4
}
```

- The type is written `[type; length]`
- Indexing starts at `0`
- Indexing past the end **panics** at runtime instead of reading random memory

Need a list that grows? That's `Vec`, coming in a later unit.

@choice What does this print?
!output
```rust
fn main() {
    let a = [10, 20, 30];
    let b = [7; 5];
    println!("{} {}", a[2], b.len());
}
```
+ 30 5
- 20 5
- 30 7
- 20 7
? `a[2]` is the third element, `30`. `[7; 5]` is five sevens, so its length is `5`.

@compiles
```rust
fn main() {
    let a = [1, 2, "three"];
    println!("{}", a.len());
}
```
= no
? Every element of an array must have the same type. `"three"` is text, the others are integers: "mismatched types".

@bug Which line has the error?
!error
```rust
fn main() {
    let a = [1, 2, 3];
    let b: [i32; 3] = [1, 2, 3, 4];
    println!("{}", a[0] + b[0]);
}
```
= 3
? The type says 3 elements, but four are given. The length is part of an array's type, so `[i32; 3]` and `[i32; 4]` are different types.

@compiles
```rust
fn main() {
    let g = [[0; 3]; 2];
    let (w, h) = (3, 2);
    println!("{}", g[1][2] + w * h);
}
```
= yes
? `g` is an array of 2 arrays of 3 zeros. `g[1][2]` is the last cell (`0`), so this prints `6`.

@order Arrange the lines to print `Ferris: 8 legs`.
!compiles
```rust
fn main() {
    let crab = ("Ferris", 8);
    let (name, legs) = crab;
    println!("{name}: {legs} legs");
}
```
? Create the tuple, destructure it, then print the parts.

@lesson types-control
title: if & loops

@explain if is an expression
`if` needs a `bool` condition - no parentheses needed, and no "truthy" numbers:

```rust
fn main() {
    let n = 7;
    let kind = if n % 2 == 0 {
        "even"
    } else {
        "odd"
    };
    println!("{kind}"); // odd
}
```

`if` is an **expression**, so it produces a value you can store with `let`. Both branches must have the **same type**.

@compiles
```rust
fn main() {
    let lives = 3;
    if lives {
        println!("alive!");
    }
}
```
= no
? Rust doesn't treat numbers as true/false: "mismatched types: expected `bool`, found integer". Write `if lives > 0`.

@choice What does this print?
!output
```rust
fn main() {
    let temp = 18;
    let msg = if temp > 25 {
        "hot"
    } else if temp > 15 {
        "nice"
    } else {
        "cold"
    };
    println!("{msg}");
}
```
+ nice
- hot
- cold
- hot nice
? `18 > 25` is false, `18 > 15` is true, so the second branch wins. Only one branch ever runs.

@bug Which line breaks the build?
!error
```rust
fn main() {
    let ok = true;
    let v = if ok {
        5
    } else {
        "five"
    };
    println!("{v}");
}
```
= 6
? "`if` and `else` have incompatible types": the first branch is an integer, the second is text. `v` can only have one type.

@explain Loops
Rust has three loops:

```rust
fn main() {
    let mut n = 1;
    let big = loop {
        n *= 2;
        if n > 50 { break n; }
    }; // big = 64
    while n > 1 { n /= 2; }
    for i in 1..4 { println!("{i}"); }
}
```

- `loop` repeats forever - `break value` exits **and** returns a value
- `while cond` runs as long as `cond` is `true`
- `for x in range` walks a range: `1..4` is 1, 2, 3 (end **excluded**), `1..=4` is 1, 2, 3, 4 (end **included**)

@choice What does this print?
!output
```rust
fn main() {
    let mut tries = 0;
    let result = loop {
        tries += 1;
        if tries == 3 {
            break tries * 10;
        }
    };
    println!("{result}");
}
```
+ 30
- 3
- 10
- 0
? The loop runs until `tries` is 3, then `break tries * 10` exits and hands `30` to `result`.

@choice What does this print?
!output
```rust
fn main() {
    for i in 0..3 {
        print!("{i}");
    }
}
```
+ 012
- 0123
- 123
- 3
? `0..3` stops **before** 3. `print!` (without `ln`) doesn't add a newline, so the digits end up side by side.

@fill Sum 1 through 5, **including** 5, to get 15.
!compiles
```rust
fn main() {
    let mut sum = 0;
    for n in 1___5 {
        sum += n;
    }
    println!("{sum}");
}
```
= ..=
~ ..
~ ...
~ to
? `1..=5` includes the end: 1+2+3+4+5 = 15. With `1..5` you'd stop at 4 and get 10.

@order Arrange to count down 3, 2, 1, then lift off.
!compiles
```rust
fn main() {
    let mut n = 3;
    while n > 0 {
        println!("{n}...");
        n -= 1;
    }
    println!("Liftoff! 🦀");
}
```
? Print first, then decrease `n`, so you see 3, 2, 1. When `n` hits 0 the condition is false and the loop ends.

@compiles
```rust
fn main() {
    let x = while true {
        break 5;
    };
}
```
= no
? Only `loop` can `break` with a value: "`break` with value from a `while` loop". A `while` might never run its body, so it can't promise a result.

@compiles
```rust
fn main() {
    for c in 'a'..='e' {
        print!("{c}");
    }
}
```
= yes
? Ranges work for `char` too! This prints `abcde`.
