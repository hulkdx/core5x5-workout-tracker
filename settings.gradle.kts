rootProject.name = "Core5x5"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":screenshot-test:android")
include(":screenshot-test:ios")
include(":ui-test")
include(":core:ui")
include(":core:preferences")
include(":core:training")
include(":core:navigation")
include(":feature:workout")
include(":feature:history")
include(":feature:settings")
include(":shared")
