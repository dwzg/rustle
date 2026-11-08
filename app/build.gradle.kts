plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// ---- Rust core (../core) ------------------------------------------------
// The course parser, grading and progress rules are written in Rust. These
// tasks build it for Android (via cargo-ndk), for the JVM unit tests (host
// build), and generate the Kotlin bindings with UniFFI.
val coreDir = rootProject.file("core")
val rustJniLibs = layout.buildDirectory.dir("rustJniLibs")
val uniffiOut = layout.buildDirectory.dir("generated/uniffi")
val hostLibDir = coreDir.resolve("target/release")
val hostLib = hostLibDir.resolve(
    when {
        org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "librustle_core.dylib"
        org.gradle.internal.os.OperatingSystem.current().isWindows -> "rustle_core.dll"
        else -> "librustle_core.so"
    }
)

fun Exec.rustInputs() {
    inputs.dir(coreDir.resolve("src"))
    inputs.files(coreDir.resolve("Cargo.toml"), coreDir.resolve("Cargo.lock"), coreDir.resolve("uniffi.toml"))
    workingDir = coreDir
}

val cargoBuildHost by tasks.registering(Exec::class) {
    description = "Builds the Rust core for this machine (bindings + unit tests)."
    rustInputs()
    outputs.file(hostLib)
    commandLine("cargo", "build", "--release", "--lib")
}

val generateUniffiBindings by tasks.registering(Exec::class) {
    description = "Generates Kotlin bindings for the Rust core."
    dependsOn(cargoBuildHost)
    rustInputs()
    outputs.dir(uniffiOut)
    doFirst { delete(uniffiOut) }
    commandLine(
        "cargo", "run", "--release", "--features", "bindgen", "--bin", "uniffi-bindgen", "--",
        "generate", "--library", hostLib.absolutePath, "--language", "kotlin",
        "--no-format", "--out-dir", uniffiOut.get().asFile.absolutePath,
    )
}

val cargoBuildAndroid by tasks.registering(Exec::class) {
    description = "Builds the Rust core for Android devices and emulators."
    rustInputs()
    outputs.dir(rustJniLibs)
    doFirst { environment("ANDROID_NDK_HOME", android.ndkDirectory.absolutePath) }
    commandLine(
        "cargo", "ndk", "-t", "arm64-v8a", "-t", "armeabi-v7a", "-t", "x86_64",
        "-o", rustJniLibs.get().asFile.absolutePath, "build", "--release", "--lib",
    )
}

tasks.named("preBuild") { dependsOn(generateUniffiBindings, cargoBuildAndroid) }

android {
    namespace = "io.github.dwzg.rustle"
    compileSdk = 36
    ndkVersion = "30.0.16248370"

    defaultConfig {
        applicationId = "io.github.dwzg.rustle"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        // Must match the targets the Rust core is built for (cargoBuildAndroid).
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key so the release APK can be sideloaded.
            // Swap in your own signing config before publishing to a store.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("main") {
            java.srcDir(uniffiOut)
            jniLibs.srcDir(rustJniLibs)
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.maxHeapSize = "2g"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Robolectric's Android runtime jar, fetched by Gradle so tests can run offline.
val robolectricRuntime: Configuration by configurations.creating

val prepareRobolectric by tasks.registering(Copy::class) {
    from(robolectricRuntime)
    into(layout.buildDirectory.dir("robolectric-jars"))
}

tasks.withType<Test>().configureEach {
    dependsOn(prepareRobolectric, cargoBuildHost)
    // Lets JNA find the host build of the Rust core.
    systemProperty("jna.library.path", hostLibDir.absolutePath)
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", layout.buildDirectory.dir("robolectric-jars").get().asFile.absolutePath)
}

dependencies {
    robolectricRuntime(libs.robolectric.android.all)

    implementation(libs.jna) { artifact { type = "aar" } }
    testImplementation(libs.jna)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
