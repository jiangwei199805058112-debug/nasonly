// build.gradle.kts
plugins {
    kotlin("jvm") version "1.5.31"
    id("com.android.application") version "7.0.3"
}

android {
    compileSdk = 31
    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.compose.ui:ui:1.0.0")
    implementation("androidx.compose.material3:material3:1.0.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.0.0")
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation("io.coil-kt:coil-compose:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:1.0.0-alpha07")
    implementation("androidx.media3:media3-exoplayer:1.0.0")
    implementation("androidx.media3:media3-smb:1.0.0")
    implementation("jcifs:jcifs:2.1.16")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
}