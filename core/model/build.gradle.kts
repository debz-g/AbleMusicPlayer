plugins {
    alias(libs.plugins.androidLibrary)
    // AGP 9.0+ has Kotlin built-in; kotlinAndroid plugin must NOT be applied separately
}

android {
    namespace = "io.github.uditkarode.able.model"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.gson)
    implementation(libs.core.ktx)
}
