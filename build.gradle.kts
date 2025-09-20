import org.gradle.api.tasks.Delete

val kotlinVersion: String by extra("1.9.10")
val composeVersion: String by extra("1.5.0")
val material3Version: String by extra("1.1.0")
val lifecycleVersion: String by extra("2.6.1")
val media3Version: String by extra("1.2.3")
val exoplayerVersion: String by extra("2.18.7")
val coilVersion: String by extra("2.4.0")
val jcifsVersion: String by extra("1.3.19")

subprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}