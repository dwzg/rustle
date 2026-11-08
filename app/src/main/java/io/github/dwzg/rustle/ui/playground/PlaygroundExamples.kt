package io.github.dwzg.rustle.ui.playground

data class Example(val title: String, val code: String)

object PlaygroundExamples {
    val all = listOf(
        Example(
            "Hello, Ferris",
            """
            fn main() {
                let name = "Ferris";
                println!("Hello, {name}! 🦀");
            }
            """.trimIndent(),
        ),
        Example(
            "Ownership & borrowing",
            """
            fn len(s: &String) -> usize {
                s.len()
            }

            fn main() {
                let s = String::from("crab");
                let n = len(&s);
                println!("{s} has {n} letters");
                let t = s; // move!
                println!("{t}");
            }
            """.trimIndent(),
        ),
        Example(
            "Enums & match",
            """
            enum Shape {
                Circle(f64),
                Rect(f64, f64),
            }

            fn area(s: &Shape) -> f64 {
                match s {
                    Shape::Circle(r) => 3.14 * r * r,
                    Shape::Rect(w, h) => w * h,
                }
            }

            fn main() {
                let shapes = [
                    Shape::Circle(1.0),
                    Shape::Rect(2.0, 3.0),
                ];
                for s in &shapes {
                    println!("{:.2}", area(s));
                }
            }
            """.trimIndent(),
        ),
        Example(
            "Iterators",
            """
            fn main() {
                let squares: Vec<u32> = (1..=10)
                    .filter(|n| n % 2 == 0)
                    .map(|n| n * n)
                    .collect();
                println!("{squares:?}");
                let sum: u32 = squares.iter().sum();
                println!("sum = {sum}");
            }
            """.trimIndent(),
        ),
        Example(
            "Error handling",
            """
            use std::num::ParseIntError;

            fn double(s: &str) -> Result<i32, ParseIntError> {
                let n: i32 = s.trim().parse()?;
                Ok(n * 2)
            }

            fn main() {
                for input in ["21", "crab"] {
                    match double(input) {
                        Ok(v) => println!("{input} -> {v}"),
                        Err(e) => println!("{input}: {e}"),
                    }
                }
            }
            """.trimIndent(),
        ),
        Example(
            "Threads",
            """
            use std::sync::{Arc, Mutex};
            use std::thread;

            fn main() {
                let count = Arc::new(Mutex::new(0));
                let mut handles = vec![];
                for _ in 0..8 {
                    let c = Arc::clone(&count);
                    handles.push(thread::spawn(move || {
                        *c.lock().unwrap() += 1;
                    }));
                }
                for h in handles {
                    h.join().unwrap();
                }
                println!("count = {}", *count.lock().unwrap());
            }
            """.trimIndent(),
        ),
        Example(
            "Borrow checker says no",
            """
            fn main() {
                let mut v = vec![1, 2, 3];
                let first = &v[0];
                v.push(4);
                println!("{first}");
            }
            """.trimIndent(),
        ),
    )
}
