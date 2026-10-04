# Core5x5 Mobile

Kotlin Multiplatform for Android and iOS, with feature-first Gradle modules, shared Compose UI, AndroidX ViewModels, UDF, lightweight Clean Architecture, and Koin.

The shared shell displays “Core5x5”. The workout domain defines the fixed A/B program, exercise prescriptions, and 20 kg starting weights. The workout feature also has a minimal Room persistence foundation for one unfinished workout (A/B and start timestamp), with Android/iOS builders and on-disk round-trip tests on Android devices/emulators and the iOS simulator. Interactive workout features, navigation, and the full design system are deferred.

Read [the project context](../docs/PROJECT_CONTEXT.md) for product requirements and [the architecture guide](../docs/ARCHITECTURE.md) for dependency rules, state conventions, DI lifecycle, and future code placement. Design references live in [design/](../design/README.md).

## Modules

- `feature:workout`: fixed program domain and tests; future Today, active-workout, rest, and completion flow.
- `feature:history`: completed-session history and details; build configuration only for now.
- `feature:settings`: training and application preferences; build configuration only for now.
- `core:ui`: reusable visual foundations, currently the minimal theme; no feature logic.
- `shared`: app composition, shell, Koin startup, and the `Shared` iOS framework; depends on features and core UI.
- `androidApp`: thin Android host using `App()`.
- `iosApp`: thin SwiftUI host using `MainViewController()` from `Shared`.

Each feature keeps its `presentation`, `domain`, `data`, and `di` packages inside its own `src/commonMain`. Add packages when real code needs them, not as empty placeholders. Shared tests live in each module's `src/commonTest`. The supported targets are Android, iOS devices (`iosArm64`), and Apple Silicon iOS simulators (`iosSimulatorArm64`).

Features depend on `core:ui`, not other features. `shared` coordinates them. See [feature conventions](feature/README.md) and [shared UI boundaries](core/ui/README.md).

## Build and run

Agents must follow the [KMM tooling and command approval policy](../AGENTS.md#kmm-tooling-and-command-approval): use `android` first for supported Android operations; suggest relevant Gradle commands at the end of the task and wait for explicit approval. Commands below are references for manual use or approved validation, not automatic agent steps. The approval rule also covers IDE sync and Xcode build phases that invoke Gradle.

Open this directory in Android Studio. Keep local SDK settings out of version control and use the checked-in Gradle wrapper and version catalog.

```bash
./gradlew :androidApp:assembleDebug
```

For manual development, run `androidApp` on an Android device/emulator from the IDE. For iOS, open [iosApp/iosApp.xcodeproj](iosApp/iosApp.xcodeproj) in Xcode and run the `iosApp` scheme. Its build phase invokes Gradle to compile and embed `Shared`. Device deployment requires your own signing configuration; simulator builds do not require a development team.

To validate just the Kotlin framework on macOS with Xcode installed:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared:linkDebugFrameworkIosArm64
```

Dependency and plugin versions are centralized in [gradle/libs.versions.toml](gradle/libs.versions.toml); the Gradle distribution and its SHA-256 checksum are pinned in [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties). App identifiers are preserved.

## Tests

```bash
./gradlew :shared:testAndroidHostTest
./gradlew :shared:iosSimulatorArm64Test
./gradlew :feature:workout:testAndroidHostTest
./gradlew :feature:workout:iosSimulatorArm64Test
```

`AppCompositionTest` loads the actual app graph and checks ViewModel retention, isolation between owners, cancellation on owner clearing, and isolation between app graphs. It uses `kotlin.test`, local Koin containers, and a controlled coroutine dispatcher. iOS test execution requires an installed compatible simulator runtime.

`WorkoutProgramTest` checks the fixed A/B contents and order, five exercises, sets/reps (including Deadlift at 1×5), and 20 kg starting weights on the workout module’s Android host and iOS simulator test targets. History and settings remain empty and do not yet provide behavior coverage. Before shipping app-root changes, also check Android Activity recreation and iOS background/foreground behavior on running apps. See [the testing strategy](../docs/ARCHITECTURE.md#testing-strategy) for details.

## Dependency updates

Versions were checked against official release pages and Maven metadata on **2026-10-03**. Use stable releases, with Material 3 aligned to the version shipped with stable Compose Multiplatform.

| Dependency / tool | Version |
| --- | --- |
| Android Gradle plugin | 9.4.1 |
| Gradle distribution | 9.8.0 |
| Kotlin and Compose compiler | 2.4.20 |
| Compose Multiplatform | 1.12.1 |
| Compose Material 3 | 1.12.0-alpha03 |
| Multiplatform AndroidX Lifecycle | 2.11.0 |
| Koin | 4.2.2 |
| kotlinx.coroutines | 1.11.0 |
| AndroidX Activity | 1.13.0 |
| AndroidX AppCompat | 1.8.0 |
| AndroidX Core | 1.19.1 |
| AndroidX Espresso | 3.7.0 |
| AndroidX Test JUnit extension | 1.3.0 |
| JUnit 4 | 4.13.2 |

[Compose Multiplatform 1.12.1](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.1) ships Material 3 `1.12.0-alpha03`. Keep that independent version pin: Material 3 does not have a matching `1.12.1` stable artifact. The newer `1.13.0-alpha01` belongs to the Compose 1.13 preview track.

[AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes) requires Gradle 9.6.0 or newer. The latest stable AGP and Gradle pins exceed [Kotlin 2.4.20's fully supported range](https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin), which ends at AGP 9.3.1 and Gradle 9.7.0. Kotlin permits newer releases but warns about possible deprecations or unsupported new features. Validate Android compilation, shared host tests, and iOS framework linking with approved Gradle commands before merging updates.

[Dependabot](.github/dependabot.yml) checks Gradle dependencies and plugins every Monday, with at most five open version-update pull requests. It monitors the repository root, including module build files, [the version catalog](gradle/libs.versions.toml), and the Gradle wrapper. Review and validate updates before merging them.

To activate version updates on GitHub, commit the configuration to the mobile repository's default branch. The mobile checkout is a separate repository, so its configured directory is `/`, not `/kmm`. See [GitHub's Dependabot configuration guide](https://docs.github.com/en/code-security/concepts/supply-chain-security/about-the-dependabot-yml-file). Dependabot alerts and security updates are configured separately in GitHub settings; Gradle security updates also require [dependency graph submissions](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#gradle).
