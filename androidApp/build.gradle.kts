import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}
dependencies {
    implementation(project(":shared"))
    implementation(project(":core:common"))
    implementation(project(":core:rest-alerts"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    debugImplementation(libs.compose.uiTooling)
    androidTestImplementation(project(":core:preferences"))
    androidTestImplementation(project(":feature:workout"))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.testExt.junit)
}

android {
    namespace = "com.hulkdx.core5x5"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileSdkMinor = libs.versions.android.compileSdkMinor.get().toInt()

    defaultConfig {
        applicationId = "com.hulkdx.core5x5"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            // Native verification installs beside development data, using the production host.
            applicationIdSuffix = if (providers.gradleProperty("nativeVerification").orNull == "true")
                ".verification" else ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}
