plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false

    // ✅ 显式声明 Hilt 插件 2.52 版本
    id("com.google.dagger.hilt.android") version "2.52" apply false
}
