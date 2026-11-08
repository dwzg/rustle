# JNA and the UniFFI-generated bindings for the Rust core use reflection.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class io.github.dwzg.rustle.core.** { *; }
-dontwarn java.awt.**
