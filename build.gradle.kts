plugins {
    id("com.android.application") version "8.5.2" apply false
    // Align Kotlin plugin with the version resolved in CI to avoid stdlib/compiler metadata mismatch.
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
}
