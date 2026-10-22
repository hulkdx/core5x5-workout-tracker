import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.hulkdx.core5x5.feature.workout"
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
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        if (providers.gradleProperty("iosSnapshots").orNull == "true") {
            matching { it.name == "iosMain" }.configureEach {
                kotlin.srcDir("src/iosSnapshot/kotlin")
            }
        }
        commonMain.dependencies {
            implementation(project(":core:navigation"))
            implementation(libs.kotlinx.serialization.core)
            implementation(project(":core:training"))
            implementation(project(":core:preferences"))
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(project(":core:ui"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.koin.compose.viewmodel)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.espresso.core)
            implementation(libs.compose.uiTestJunit4)
            implementation(libs.compose.uiTestManifest)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.koin.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    androidRuntimeClasspath(libs.compose.uiTooling)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
