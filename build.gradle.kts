plugins {
    alias(libs.plugins.androidApplication)   apply false
    alias(libs.plugins.androidLibrary)       apply false
    // AGP 9.0+ has Kotlin built-in; kotlinAndroid is intentionally omitted here
    alias(libs.plugins.kotlin.jvm)           apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize)     apply false
    alias(libs.plugins.compose.compiler)     apply false
    alias(libs.plugins.hilt)                apply false
    alias(libs.plugins.ksp)                 apply false
}
