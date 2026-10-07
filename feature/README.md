 # Feature modules

Gradle modules follow product capabilities. Clean Architecture layers are packages inside each feature, not separate project-wide modules.

| Module | Responsibility |
| --- | --- |
| `:feature:workout` | Today and the related active-workout, rest, and completion flow |
| `:feature:history` | Completed-session History and read-only Workout Detail presentation |
| `:feature:settings` | Preference-backed training and application settings |

The workout module contains the pure Kotlin fixed program definition in `domain`, Room-backed session persistence, Today, Active Workout, rest/recovery, and completion. `Exercise` holds prescribed sets/reps and starting weight in kg, and `Workout` holds the ordered exercises for A/B. Focused common tests verify the program and persistence behavior. History consumes completed sessions through `:core:training` and provides state-driven list/detail screens with stable-ID navigation. Settings consumes `:core:preferences` for units and rest duration and provides the preference-backed Settings, About, and license surfaces.

Each feature is a Compose Multiplatform library targeting Android, `iosArm64`, and `iosSimulatorArm64`. Compose, AndroidX ViewModels, coroutines, Koin, and common-test dependencies are ready.

When implementing a feature, add code under `<feature>/src/commonMain/kotlin/com/hulkdx/core5x5/feature/<feature>/`, using `presentation`, `domain`, `data`, and `di` packages only where needed. Put its tests in that feature's `src/commonTest`.

Keep implementation types `internal`. Expose only entry-point composables and a Koin module when they exist; `:shared` composes the application. Do not make one feature depend on another feature. Extract a narrowly named shared capability only when a real cross-feature dependency is required.

See [the architecture guide](../../docs/ARCHITECTURE.md) for package dependency rules, UDF conventions, and lifecycle ownership.
