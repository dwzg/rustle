@unit collections
title: Collections
tagline: Vec, String and HashMap - Ferris's storage shelves
icon: 📚

@lesson coll-vec
title: Vectors

@explain A list that grows
A `Vec<T>` is a growable list of values that all have the same type `T`. Ferris keeps every shiny pebble in one 🦀

```rust
fn main() {
    let mut v: Vec<i32> = Vec::new();
    v.push(1);
    v.push(2);
    let w = vec![1, 2, 3];
    println!("{v:?} {w:?}");
}
```

- `Vec::new()` makes an empty vector, `vec![...]` makes one with values
- `push` adds to the end, `len` counts the elements
- Changing a vector needs `let mut`

@choice What does this print?
!output
```rust
fn main() {
    let mut v = Vec::new();
    v.push(10);
    v.push(20);
    v.push(30);
    println!("{} {}", v.len(), v[1]);
}
```
+ 3 20
- 3 10
- 2 20
- 30 20
? Three pushes give a length of 3. Indexes start at 0, so `v[1]` is the second element: `20`.

@compiles
```rust
fn main() {
    let v = vec![1, 2];
    v.push(3);
    println!("{:?}", v);
}
```
= no
? `push` changes the vector, so it needs a mutable binding: `let mut v`.

@explain Reading elements
There are two ways to read an element:

```rust
fn main() {
    let v = vec![10, 20, 30];
    let a = v[0];
    let b = v.get(9);
    println!("{a} {b:?}"); // 10 None
}
```

- `v[i]` gives the element, but **panics** at runtime if `i` is out of bounds
- `v.get(i)` returns an `Option<&T>`: `Some(&x)` or `None`. No crash, you decide what to do

@choice What does this print?
!output
```rust
fn main() {
    let v = vec![1, 2, 3];
    match v.get(10) {
        Some(n) => println!("n={n}"),
        None => println!("nothing"),
    }
}
```
+ nothing
- n=3
- n=0
- It panics
? There's no index 10, so `get` returns `None`. Unlike `v[10]`, it doesn't panic.

@compiles
```rust
fn main() {
    let v = vec![1, 2, 3];
    let x = v[10];
    println!("{x}");
}
```
= yes
? It compiles! The compiler doesn't know the vector's length, so the bad index is only caught at **runtime** - where it panics. Use `.get()` when an index might be missing.

@compiles
```rust
fn main() {
    let v = vec![7, 8, 9];
    let x: i32 = v.get(0);
    println!("{x}");
}
```
= no
? `get` returns `Option<&i32>`, not `i32`. You have to unwrap the `Option` first, for example with `match` or `if let`.

@explain Looping over a Vec
Loop over a **reference** so the vector isn't moved:

```rust
fn main() {
    let mut v = vec![1, 2, 3];
    for x in &v {
        println!("{x}");
    }
    for x in &mut v {
        *x *= 2;
    }
    println!("{:?}", v); // [2, 4, 6]
}
```

With `&mut v`, each `x` is a `&mut i32`. Use `*x` to reach the value behind it.

@fill Add 10 to every element.
!compiles
```rust
fn main() {
    let mut v = vec![1, 2, 3];
    for x in ___ v {
        ___ += 10;
    }
    println!("{:?}", v);
}
```
= &mut
= *x
~ &
~ x
~ mut
~ &x
? To change elements, borrow the vector mutably with `&mut v`, then dereference each item with `*x` to modify it.

@bug Which line does the borrow checker reject?
!error
```rust
fn main() {
    let mut v = vec![1, 2, 3];
    let first = &v[0];
    v.push(4);
    println!("{first}");
}
```
= 4
? `first` borrows the vector, and it's still used later. `push` might move the elements to a bigger memory block, which would leave `first` dangling - so Rust forbids the mutable borrow on line 4.

@order Arrange the lines so the program greets Ferris.
!compiles
```rust
fn main() {
    let mut v = Vec::new();
    v.push("Ferris");
    for name in &v {
        println!("Hi, {name}!");
    }
}
```
? Create the vector, push a name, then loop over `&v` to greet every name in it.

@lesson coll-string
title: Strings

@explain Two string types
Rust has two main string types:

- `String` - owned, growable text on the heap
- `&str` - a borrowed view of some text, like the literal `"hi"`

```rust
fn main() {
    let mut s = String::from("Hi");
    s.push_str(", Ferris");
    s.push('!');
    println!("{s}"); // Hi, Ferris!
}
```

`push_str` appends a `&str`, `push` appends a single `char` (single quotes!).

@choice What does this print?
!output
```rust
fn main() {
    let mut s = String::new();
    s.push_str("cr");
    s.push('a');
    s.push_str("b");
    println!("{s} {}", s.len());
}
```
+ crab 4
- crab 3
- cr a b 4
- crab 6
? `"cr"` + `'a'` + `"b"` makes `crab`, which is 4 bytes long.

@fill Build the word "rustacean".
!compiles
```rust
fn main() {
    let mut s = String::___("rust");
    s.___("acean");
    println!("{s}");
}
```
= from
= push_str
~ new
~ push
~ append
~ add
? `String::from` builds a `String` from a literal. `push_str` appends text - `push` only takes a single `char`.

@explain Gluing strings
Two ways to combine strings:

```rust
fn main() {
    let a = String::from("tic");
    let b = String::from("tac");
    let c = format!("{a}-{b}");
    let d = a + "-" + &b;
    println!("{c} {d}");
}
```

- `format!` works like `println!`, but returns a `String`. It only **borrows** its arguments
- `+` takes the left `String` **by value** (it's moved!) and appends a `&str` on the right - hence `&b`

@compiles
```rust
fn main() {
    let a = String::from("salt");
    let b = String::from("water");
    let c = a + &b;
    println!("{a} {c}");
}
```
= no
? `a + &b` moves `a` into the result. Using `a` afterwards is a "borrow of moved value" error.

@compiles
```rust
fn main() {
    let a = String::from("salt");
    let b = String::from("water");
    let c = format!("{a}{b}");
    println!("{a} + {b} = {c}");
}
```
= yes
? `format!` only borrows `a` and `b`, so both are still usable afterwards.

@choice What does this print?
!output
```rust
fn main() {
    let s1 = String::from("Fer");
    let s2 = "ris";
    let s = s1 + s2 + " 🦀";
    println!("{s}");
}
```
+ Ferris 🦀
- Fer ris 🦀
- Fer + ris + 🦀
- It doesn't compile
? `s1` is a `String` and the rest are `&str`s, so `+` happily glues them: `Ferris 🦀`.

@explain No s[0]!
A `String` is stored as **UTF-8** bytes, and one character can take 1 to 4 bytes. So `s[0]` would be ambiguous - a byte? a character? Rust refuses to guess and won't compile it.

```rust
fn main() {
    let s = String::from("añb");
    println!("{}", s.len()); // 4
    for c in s.chars() {
        println!("{c}"); // a ñ b
    }
}
```

- `.len()` counts **bytes**, not characters
- `.chars()` walks the text character by character
- `.chars().next()` gives the first `char` as an `Option`

@compiles
```rust
fn main() {
    let s = String::from("hello");
    let c = s[0];
    println!("{c}");
}
```
= no
? Strings can't be indexed by an integer in Rust. Use `s.chars().next()` to get the first character.

@choice What does this print?
!output
```rust
fn main() {
    let s = "Café";
    let n = s.chars().count();
    println!("{} {}", s.len(), n);
}
```
+ 5 4
- 4 4
- 4 5
- 5 5
? `é` takes 2 bytes in UTF-8, so the text is 5 bytes long but only 4 characters.

@bug Which line doesn't compile?
!error
```rust
fn main() {
    let mut s = String::from("hi");
    for c in "yo".chars() {
        s.push(c);
    }
    s.push("!");
    println!("{s}");
}
```
= 6
? `push` takes a `char`, but `"!"` is a `&str`. Write `s.push('!')` or `s.push_str("!")`.

@lesson coll-hashmap
title: Hash maps

@explain Key → value
A `HashMap<K, V>` stores values you look up by key. It's not in the prelude, so bring it in with `use`:

```rust
use std::collections::HashMap;

fn main() {
    let mut m = HashMap::new();
    m.insert("crab", 10);
    m.insert("fish", 0);
    println!("{:?}", m.get("crab"));
}
```

- `insert(k, v)` adds a pair. Inserting an existing key **overwrites** the old value
- `get(&k)` returns an `Option<&V>`, since the key might be missing. This prints `Some(10)`

@choice What does this print?
!output
```rust
use std::collections::HashMap;

fn main() {
    let mut hp = HashMap::new();
    hp.insert("Ferris", 10);
    hp.insert("Ferris", 25);
    let n = hp.len();
    let f = hp.get("Ferris");
    println!("{n} {f:?}");
}
```
+ 1 Some(25)
- 2 Some(10)
- 2 Some(25)
- 1 25
? The second `insert` uses the same key, so it replaces `10` with `25`. There's still only one entry, and `get` wraps the value in `Some`.

@compiles
```rust
fn main() {
    let mut m = HashMap::new();
    m.insert(1, "one");
    println!("{}", m.len());
}
```
= no
? `HashMap` isn't in the prelude. Without `use std::collections::HashMap;` the compiler can't find it.

@fill Store Ferris's leg count, then read it back.
!compiles
```rust
use std::collections::HashMap;

fn main() {
    let mut m = HashMap::new();
    m.___("crab", 10);
    match m.___("crab") {
        Some(n) => println!("{n}!"),
        None => println!("no crab?"),
    }
}
```
= insert
= get
~ push
~ add
~ find
~ at
? `insert` adds a key-value pair; `get` looks a key up and returns an `Option`.

@explain The entry API
`entry(key).or_insert(default)` returns a `&mut V` to the value for `key`, inserting `default` first if the key is missing. Perfect for counting:

```rust
use std::collections::HashMap;

fn main() {
    let text = "a b a";
    let mut c = HashMap::new();
    for w in text.split(' ') {
        *c.entry(w).or_insert(0) += 1;
    }
    println!("{}", c["a"]); // 2
}
```

`map[key]` is a shortcut for reading, but it **panics** if the key is missing.

@choice What does this print?
!output
```rust
use std::collections::HashMap;

fn main() {
    let mut c = HashMap::new();
    for w in ["crab", "eel", "crab"] {
        *c.entry(w).or_insert(0) += 1;
    }
    let n = c["crab"];
    println!("{n} {}", c.len());
}
```
+ 2 2
- 2 3
- 1 2
- 3 2
? `"crab"` is counted twice and `"eel"` once, so there are 2 keys and `c["crab"]` is 2.

@bug Which line doesn't compile?
!error
```rust
use std::collections::HashMap;

fn main() {
    let mut c = HashMap::new();
    for w in ["a", "b", "a"] {
        c.entry(w).or_insert(0) += 1;
    }
    println!("{}", c["a"]);
}
```
= 6
? `or_insert` returns a `&mut` reference. You can't `+=` a reference - dereference it first: `*c.entry(w).or_insert(0) += 1`.

@explain What order? 🤷
Iterating a `HashMap` gives `(key, value)` pairs in **no particular order**. The order can even change from one run to the next!

```rust
use std::collections::HashMap;

fn main() {
    let mut m = HashMap::new();
    m.insert("x", 1);
    m.insert("y", 2);
    for (k, v) in &m {
        println!("{k} = {v}");
    }
}
```

Need a stable order? Sort the keys first, or use `BTreeMap`, which keeps its keys sorted.

@choice A HashMap gets keys "a", "b", "c" inserted in that order. In which order does a `for` loop visit them?
+ Any order - it isn't guaranteed
- Always a, b, c
- Always c, b, a
- Sorted by value
? A `HashMap` makes no promises about order. Don't rely on it - not even between two runs of the same program.

@compiles
```rust
use std::collections::HashMap;

fn main() {
    let mut m = HashMap::new();
    m.insert("crab", 10);
    m.insert("squid", 8);
    for (k, v) in &m {
        println!("{k}: {v}");
    }
}
```
= yes
? Iterating over `&m` gives `(&K, &V)` pairs and leaves the map usable afterwards. It compiles fine - just don't count on the order.

@fill Count the fish in stock.
!compiles
```rust
use std::collections::HashMap;

fn main() {
    let mut stock = HashMap::new();
    for f in ["cod", "eel", "cod"] {
        *stock.___(f).___(0) += 1;
    }
    println!("{}", stock["cod"]);
}
```
= entry
= or_insert
~ get
~ insert
~ or_default
~ unwrap_or
? `entry(f)` finds the slot for the key, and `or_insert(0)` fills in `0` if it's empty, returning a `&mut` you can add to.
