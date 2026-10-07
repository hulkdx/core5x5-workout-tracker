import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeScreenshot)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "com.hulkdx.core5x5.screenshottest"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    screenshotTestImplementation(project(":core:ui"))
    screenshotTestImplementation(project(":core:preferences"))
    screenshotTestImplementation(project(":core:training"))
    screenshotTestImplementation(project(":feature:workout"))
    screenshotTestImplementation(project(":feature:history"))
    screenshotTestImplementation(project(":feature:settings"))
    screenshotTestImplementation(project(":shared"))
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.runtime)
    screenshotTestImplementation(libs.compose.foundation)
    screenshotTestImplementation(libs.compose.uiTooling)
    screenshotTestImplementation(libs.compose.uiToolingPreview)
}

// Only screenshot tests can see dependency internals; production APIs stay unchanged.
tasks.withType<KotlinCompile>().configureEach {
    if (name.contains("ScreenshotTest")) {
        friendPaths.from(libraries)
    }
}
