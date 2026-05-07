plugins {
    alias(libs.plugins.androidApplication)
    // AGP 9.0+ has Kotlin built-in; kotlinAndroid plugin must NOT be applied separately
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "io.github.uditkarode.able"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.uditkarode.able"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "ConcentricPuddles"
    }

    buildFeatures {
        viewBinding = true   // kept during incremental XML → Compose migration
        buildConfig = true
        compose = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("STORE_PASS")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASS")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs["release"]
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // ── AndroidX Core ────────────────────────────────────────────────────────
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.coordinatorlayout)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.preference.ktx)

    // ── Jetpack Compose (all versions from BOM) ───────────────────────────────
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.icons)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.animations)
    implementation(libs.compose.animations.core)
    implementation(libs.compose.runtime)
    implementation(libs.compose.activity)
    debugImplementation(libs.compose.ui.tooling)

    // ── Navigation ────────────────────────────────────────────────────────────
    implementation(libs.navigation.compose)

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // ── Hilt ──────────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jetbrains.kotlinx.coroutines.android)

    // ── Network & Extraction ──────────────────────────────────────────────────
    implementation(libs.okhttp)
    implementation(libs.newpipeextractor)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)

    // ── Media & Audio ─────────────────────────────────────────────────────────
    implementation(libs.palette.ktx)
    implementation(libs.jaudiotagger.android)
    implementation(files("src/main/libs/ffmpeg-kit.aar"))
    implementation("com.arthenica:smart-exception-java:0.2.1")

    // ── Image Loading ─────────────────────────────────────────────────────────
    // Coil 3: primary loader for all Compose screens
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // Glide: kept for legacy XML screens during migration
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // ── Animations ────────────────────────────────────────────────────────────
    implementation(libs.lottie)
    implementation(libs.lottie.compose)

    // ── WorkManager ───────────────────────────────────────────────────────────
    implementation(libs.work.runtime)

    // ── Legacy XML UI — removed screen-by-screen as Compose migration advances ─
    implementation(libs.calligraphy3)
    implementation(libs.viewpump)
    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.input)
    implementation(libs.material.dialogs.bottomsheets)
    implementation(libs.roundedimageview)
    implementation(libs.preferencex)

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
