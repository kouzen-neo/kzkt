plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9.0 ships built-in Kotlin; the plugin jar is provided via classpath so the
        // Compose compiler plugin and Kotlin stdlib resolve for all subprojects.
        classpath(kotlin("gradle-plugin", "2.4.10"))
    }
}
