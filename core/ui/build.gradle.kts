import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.hulkdx.core5x5.core.ui"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
        androidResources {
            enable = true
        }
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:navigation"))
            implementation(project(":core:preferences"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
