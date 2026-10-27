plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "Core5x5Screenshots"
            isStatic = true
        }
    }

    sourceSets {
        getByName("iosSimulatorArm64Main").dependencies {
            implementation(project(":feature:workout"))
            implementation(project(":feature:history"))
            implementation(project(":feature:settings"))
            implementation(project(":core:ui"))
            implementation(project(":core:navigation"))
            implementation(project(":core:preferences"))
            implementation(project(":core:rest-alerts"))
            implementation(project(":core:training"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
    }
}
