@unit fearless
title: Fearless Concurrency
tagline: Smart pointers, threads, and the grand finale
icon: 🧵

@lesson fear-pointers
title: Smart pointers

@explain Box: a value on the heap
`Box<T>` stores a value on the **heap** and keeps a small pointer to it. Deref with `*` to reach the value:

```rust
fn main() {
    let b = Box::new(41);
    println!("{}", *b + 1); // 42
}
```

Its killer feature: **recursive types**. A type can't contain itself directly (its size would be infinite), but it can contain a `Box` of itself - a box is always pointer-sized:

```rust
enum List {
    Cons(i32, Box<List>),
    Nil,
}
```

@compiles
```rust
enum List {
    Cons(i32, List),
    Nil,
}

fn main() {
    let l = List::Nil;
}
```
= no
? "recursive type has infinite size": a `List` would contain a `List` which contains a `List`... Wrap the inner one in a `Box` to break the loop.

@fill Build a two-item cons list.
!compiles
```rust
enum List {
    Cons(i32, ___<List>),
    Nil,
}

fn main() {
    let tail = List::Nil;
    let l = List::Cons(7,
        Box::___(tail));
}
```
= Box
= new
~ Option
~ Ref
~ make
~ alloc
? The field is a `Box<List>`, and `Box::new` moves a value onto the heap. `Option<List>` would still have infinite size.

@explain Rc: shared ownership
Sometimes one value needs **several owners** - like a node shared by two lists. `Rc<T>` (reference counted) keeps a count of owners and frees the value when the last one goes away:

```rust
use std::rc::Rc;

fn main() {
    let a = Rc::new(5);
    let b = Rc::clone(&a);
    let n = Rc::strong_count(&a); // 2
}
```

`Rc::clone` doesn't copy the data - it just bumps the counter. `Rc` gives **shared** access, so the data is read-only.

@choice What does this print?
!output
```rust
use std::rc::Rc;

fn main() {
    let a = Rc::new(5);
    let b = Rc::clone(&a);
    let n = {
        let _c = Rc::clone(&b);
        Rc::strong_count(&a)
    };
    let m = Rc::strong_count(&a);
    println!("{n} {m}");
}
```
+ 3 2
- 2 2
- 3 3
- 1 1
? Inside the block there are three owners (`a`, `b`, `_c`). When the block ends, `_c` is dropped and the count goes back to 2. All clones share **one** counter.

@compiles
```rust
use std::rc::Rc;

fn main() {
    let a = Rc::new(vec![1]);
    a.push(2);
    println!("{a:?}");
}
```
= no
? "cannot borrow data in an `Rc` as mutable". Many owners could be looking at the vector, so `Rc` only hands out shared references.

@explain RefCell: mutate through &
`RefCell<T>` gives **interior mutability**: you can change the value even through a shared reference. Instead of compile-time borrow checks, it checks the rules **at runtime**:

- `.borrow()` → a shared borrow
- `.borrow_mut()` → a mutable borrow

Break the rules (say, two `borrow_mut`s at once) and your program **panics** instead of failing to compile.

`Rc<RefCell<T>>` is the classic combo: many owners, and every one of them can mutate.

@compiles
```rust
use std::cell::RefCell;

fn main() {
    let c = RefCell::new(1);
    let a = c.borrow_mut();
    let b = c.borrow_mut();
    println!("{a} {b}");
}
```
= yes
? It **compiles**! `RefCell` moves the borrow check to runtime, so this program panics with "already borrowed" when it runs. The compiler can't save you here - that's the price of the flexibility.

@choice What does this print?
!output
```rust
use std::cell::RefCell;
use std::rc::Rc;

fn main() {
    let s = Rc::new(RefCell::new(0));
    let t = Rc::clone(&s);
    *t.borrow_mut() += 10;
    *s.borrow_mut() += 1;
    println!("{}", s.borrow());
}
```
+ 11
- 1
- 10
- It panics
? `s` and `t` own the **same** `RefCell`. Each `borrow_mut()` ends at the end of its statement, so the borrows never overlap: 0 + 10 + 1 = 11.

@bug Which line stops this from compiling?
!error
```rust
use std::cell::RefCell;

fn main() {
    let score = RefCell::new(5);
    *score.borrow_mut() += 1;
    *score.borrow() += 1;
    println!("{}", score.borrow());
}
```
= 6
? `borrow()` gives a read-only `Ref`. Changing the value needs `borrow_mut()`, just like `&` vs `&mut`.

@lesson fear-threads
title: Threads

@explain Spawning threads
`std::thread::spawn` runs a closure on a new thread and returns a **handle**. Call `join()` on it to wait for the thread to finish and get its return value:

```rust
use std::thread;

fn main() {
    let h = thread::spawn(|| {
        println!("hi from a thread!");
    });
    h.join().unwrap();
}
```

Without `join`, `main` may finish first - and when `main` ends, every other thread is stopped.

@choice What does this print?
!output
```rust
use std::thread;

fn main() {
    let h = thread::spawn(|| {
        (1..=10).sum::<i32>()
    });
    let total = h.join().unwrap();
    println!("{total}");
}
```
+ 55
- 10
- 0
- It varies between runs
? The thread computes 1 + 2 + ... + 10. `join()` waits for it and returns its result (wrapped in a `Result`, hence the `unwrap`).

@compiles
```rust
use std::thread;

fn main() {
    let v = vec![1, 2, 3];
    let h = thread::spawn(|| {
        println!("{v:?}");
    });
    h.join().unwrap();
}
```
= no
? The closure only **borrows** `v`, but the thread might outlive `main`'s `v` for all Rust knows. "closure may outlive the current function" - add `move`.

@explain move it into the thread
A spawned thread can run for any amount of time, so `spawn` requires its closure to be `'static`: no borrows of short-lived locals. Use `move` to hand ownership over:

```rust
use std::thread;

fn main() {
    let v = vec![1, 2, 3];
    let h = thread::spawn(move || {
        println!("{v:?}");
    });
    h.join().unwrap();
}
```

Now the thread **owns** `v`. Remember lifetimes and `'static`? Here they are, keeping you safe. 🦀

@fill Move the name into the thread and wait for it.
!compiles
```rust
use std::thread;

fn main() {
    let name = String::from("Ferris");
    let h = thread::spawn(___ || {
        println!("hi {name}");
    });
    h.___().unwrap();
}
```
= move
= join
~ ref
~ &
~ wait
~ await
? `move` gives the closure ownership of `name`, and `join()` blocks until the thread is done.

@compiles
```rust
use std::thread;

fn main() {
    let v = vec![1, 2];
    let h = thread::spawn(move || {
        v.len()
    });
    println!("{v:?}");
    h.join().unwrap();
}
```
= no
? `v` was moved into the thread, so `main` can't use it anymore: "borrow of moved value". Ownership rules work across threads too.

@explain Send: safe to send
Types that may be moved to another thread implement the marker trait `Send`. Almost everything is `Send` - but not `Rc`!

`Rc` updates its counter with plain, non-atomic operations. Two threads cloning the same `Rc` at once could corrupt the count. So the compiler simply **refuses** to let an `Rc` cross a thread boundary.

The thread-safe version is `Arc<T>` - an **atomically** reference counted `Rc`. Same API, slightly slower.

@compiles
```rust
use std::rc::Rc;
use std::thread;

fn main() {
    let a = Rc::new(5);
    let h = thread::spawn(move || {
        println!("{a}");
    });
    h.join().unwrap();
}
```
= no
? "`Rc<i32>` cannot be sent between threads safely". `Rc` isn't `Send`. Swap in `std::sync::Arc` and it compiles.

@choice What does this print?
!output
```rust
use std::thread;

fn main() {
    let mut hs = Vec::new();
    for i in 1..=3 {
        let h = thread::spawn(
            move || i * i,
        );
        hs.push(h);
    }
    let mut sum = 0;
    for h in hs {
        sum += h.join().unwrap();
    }
    println!("{sum}");
}
```
+ 14
- 6
- 9
- It varies between runs
? The threads may finish in any order, but we only add up their results after joining: 1 + 4 + 9 = 14, every time.

@order Arrange the lines so it prints 2.
!compiles
```rust
fn main() {
    let msg = String::from("hi");
    let h = std::thread::spawn(
        move || msg.len(),
    );
    let n = h.join().unwrap();
    println!("{n}");
}
```
? Create `msg`, move it into the thread, then `join()` to get its length back before printing it.

@lesson fear-share
title: Sharing between threads

@explain Channels: pass messages
A **channel** has a sending end and a receiving end. `mpsc` stands for "multiple producer, single consumer":

```rust
use std::sync::mpsc;
use std::thread;

fn main() {
    let (tx, rx) = mpsc::channel();
    thread::spawn(move || {
        tx.send("🦀").unwrap();
    });
    let msg = rx.recv().unwrap();
    println!("{msg}");
}
```

- `send` **moves** the value into the channel
- `recv` blocks until a message arrives
- `rx` also works as an iterator, which ends once every sender is dropped
- more producers? `tx.clone()`

@choice What does this print?
!output
```rust
use std::sync::mpsc;
use std::thread;

fn main() {
    let (tx, rx) = mpsc::channel();
    thread::spawn(move || {
        for i in 1..=4 {
            tx.send(i).unwrap();
        }
    });
    let total: i32 = rx.iter().sum();
    println!("{total}");
}
```
+ 10
- 4
- 0
- It never finishes
? The thread sends 1, 2, 3, 4 and then ends, dropping `tx`. That closes the channel, so `rx.iter()` stops and the sum is 10.

@compiles
```rust
use std::sync::mpsc;
use std::thread;

fn main() {
    let (tx, rx) = mpsc::channel();
    thread::spawn(move || {
        let s = String::from("hi");
        tx.send(s).unwrap();
        println!("{s}");
    });
    let msg = rx.recv().unwrap();
    println!("{msg}");
}
```
= no
? `send(s)` moved the `String` into the channel - it now belongs to the receiver. Using `s` afterwards is a "borrow of moved value". No accidental sharing!

@explain Arc + Mutex: shared state
A `Mutex<T>` lets only one thread at a time touch the data inside:

- `.lock()` waits for its turn and returns a **guard**
- the guard derefs to the data: `*guard += 1`
- the lock is released when the guard is dropped

To give several threads the same mutex, wrap it in an `Arc`:

```rust
use std::sync::{Arc, Mutex};

let c = Arc::new(Mutex::new(0));
let c2 = Arc::clone(&c);
// move c2 into a thread...
```

`Arc<Mutex<T>>` is the thread-safe cousin of `Rc<RefCell<T>>`.

@fill Finish the shared counter.
!compiles
```rust
use std::sync::{Arc, Mutex};
use std::thread;

fn main() {
    let n = ___::new(Mutex::new(0));
    let n2 = Arc::clone(&n);
    let h = thread::spawn(move || {
        *n2.___().unwrap() += 1;
    });
    h.join().unwrap();
    println!("{}", n.lock().unwrap());
}
```
= Arc
= lock
~ Rc
~ Box
~ borrow_mut
~ get
? `Arc` shares ownership across threads (`Rc` isn't `Send`, and a `Box` has just one owner). `lock()` returns a guard you deref to update the number.

@compiles
```rust
use std::sync::Mutex;
use std::thread;

fn main() {
    let m = Mutex::new(0);
    for _ in 0..2 {
        thread::spawn(move || {
            *m.lock().unwrap() += 1;
        });
    }
}
```
= no
? The first loop iteration moves `m` into a thread, so there's nothing left for the second: "use of moved value". A `Mutex` alone has one owner - you need `Arc` to share it.

@bug Which line stops this from compiling?
!error
```rust
use std::sync::Mutex;

fn main() {
    let m = Mutex::new(5);
    {
        let mut g = m.lock().unwrap();
        g += 1;
    }
    println!("{m:?}");
}
```
= 7
? `g` is a `MutexGuard`, not a number. Deref it to reach the data: `*g += 1`.

@compiles
```rust
use std::sync::Arc;
use std::thread;

fn main() {
    let v = Arc::new(vec![1, 2, 3]);
    let d = Arc::clone(&v);
    let h = thread::spawn(move || {
        d.iter().sum::<i32>()
    });
    println!("{}", h.join().unwrap());
    println!("{v:?}");
}
```
= yes
? Only the clone `d` moves into the thread; `main` keeps `v`. Read-only sharing needs just `Arc` - no `Mutex` required. Prints `6`, then `[1, 2, 3]`.

@choice The grand finale: ten threads, one counter. What does it print?
!output
```rust
use std::sync::{Arc, Mutex};
use std::thread;

fn main() {
    let c = Arc::new(Mutex::new(0));
    let mut v = vec![];
    for _ in 0..10 {
        let c = Arc::clone(&c);
        v.push(thread::spawn(move || {
            *c.lock().unwrap() += 1;
        }));
    }
    for h in v { h.join().unwrap(); }
    println!("{}", c.lock().unwrap());
}
```
+ 10
- 1
- 0
- It varies between runs
? The mutex makes each `+= 1` happen one at a time, and joining every handle means all ten are done before we print. No lost updates, no data race - **10, every time**.

@explain You made it! 🎉
Look at what just happened: the same rules you learned on day one kept your threads safe.

- **ownership** + `move` decide which thread owns what
- **borrowing** and **lifetimes** stop threads from holding dangling references
- `Send` keeps non-thread-safe types like `Rc` at home
- `Mutex` and channels make sharing explicit

In many languages data races are bugs you hunt at 3 a.m. In Rust they're **compile errors**. That's *fearless concurrency*.

Ferris is proud of you, Rustacean. 🦀🦀🦀 Now go build something!

@choice One last question: what makes Rust's concurrency "fearless"?
+ Data races are caught at compile time
- Threads never run at the same time
- A garbage collector cleans up after threads
- Rust only allows one thread per program
? Threads really do run in parallel - but the type system (ownership, `Send`, `Sync`) rejects programs with data races before they ever run. Congratulations on finishing Rustle! 🦀
