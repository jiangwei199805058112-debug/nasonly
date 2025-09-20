plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)

    // ✅ 启用 kapt
    kotlin("kapt")

    // ✅ 应用 Hilt 插件
    id("com.google.dagger.hilt.android")
}

android {
    // 使用 com.example.nasonly 作为 namespace
    namespace = "com.example.nasonly"

    defaultConfig {
        // 应用 ID 必须和 namespace 一致
        applicationId = "com.example.nasonly"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileSdk = 34

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // ✅ Hilt 依赖
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
}
