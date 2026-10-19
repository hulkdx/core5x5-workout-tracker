import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.hulkdx.core5x5.uitest"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_21 }
        androidResources { enable = true }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(project(":shared"))
            implementation(project(":feature:workout"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:history"))
            implementation(project(":core:navigation"))
            implementation(project(":core:preferences"))
            implementation(project(":core:training"))
            implementation(project(":core:ui"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiTest)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.androidx.room.runtime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.compose.uiTestJunit4)
            implementation(libs.compose.uiTestManifest)
            implementation(libs.androidx.activity.compose)
        }
    }
}
