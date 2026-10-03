# Feature modules

Gradle modules follow product capabilities. Clean Architecture layers are packages inside each feature, not separate project-wide modules.

| Module | Future responsibility |
| --- | --- |
| `:feature:workout` | Today and the related active-workout, rest, and completion flow |
| `:feature:history` | Completed-session history and details |
| `:feature:settings` | Training and application preferences |

The modules currently contain build configuration only. No product screens, workout behavior, repositories, navigation, or storage have been implemented.

Each feature is a Compose Multiplatform library targeting Android, `iosArm64`, and `iosSimulatorArm64`. Compose, AndroidX ViewModels, coroutines, Koin, and common-test dependencies are ready.

When implementing a feature, add code under `<feature>/src/commonMain/kotlin/com/example/myapplication/feature/<feature>/`, using `presentation`, `domain`, `data`, and `di` packages only where needed. Put its tests in that feature's `src/commonTest`.

Keep implementation types `internal`. Expose only entry-point composables and a Koin module when they exist; `:shared` composes the application. Do not make one feature depend on another feature. Extract a narrowly named shared capability only when a real cross-feature dependency is required.

See [the architecture guide](../../docs/ARCHITECTURE.md) for package dependency rules, UDF conventions, and lifecycle ownership.
