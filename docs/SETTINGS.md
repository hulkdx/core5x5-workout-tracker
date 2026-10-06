# Settings and shared training preferences

Settings contains Rest timer, Units, and About. Theme selection, haptics,
notifications, and data reset remain post-v1. About reads the installed Android
package or iOS bundle version, opens the application's GitHub repository, and
provides its MIT license offline.

## Agreed behavior

The user confirmed these choices on 2026-10-06:

- Units default to kg. Selecting kg or lb in a choice dialog saves immediately.
  Weights display to the nearest tenth, with positive ties rounded up and `.0`
  omitted. Conversion uses exactly 0.45359237 kg per international pound. Only
  display values change; prescriptions and sessions remain stored in kg.
- Rest defaults to 180 seconds. The selector accepts positive whole seconds,
  with explicit Save and Cancel. There is no one-hour cap. Values must fit a
  positive `Long` millisecond interval: at most `Long.MAX_VALUE / 1_000` seconds.
  Preference changes affect future timers; a running deadline stays unchanged.

## Consumer contract

`:core:preferences` owns `TrainingPreferencesRepository`, `TrainingPreferences`,
`WeightUnit`, and the shared `formatWeight(weightKg, unit)` function. Features
depend on this narrow module, without depending on Settings or each other.

The repository publishes an observable preference flow and provides a consistent
`getPreferences()` snapshot. Settings edits only this repository. The app shell
observes the unit and passes it to Today, Active Workout, and Workout Complete.
Those screens format their saved or prescribed kg weights without writing them.
History has no implemented weight display; apply this same formatter and unit
when its list/detail work arrives.

When integrating rest timer creation after a successful saved set, read:

```kotlin
val durationMillis = preferencesRepository.getPreferences().restDurationMillis
val timer = restTimerRules.start(durationMillis)
```

Read the snapshot for each new timer. Do not collect preference changes into an
already persisted timer or rewrite its deadline. Preference read failures must
follow the timer integration's error handling, without silently selecting a
different duration. The timer engine separately validates absolute-deadline
overflow. This branch exposes the preference; persisted timer creation remains
the parallel rest-timer task.

## Persistence and lifecycle

Android uses one private SharedPreferences string record, committing on IO.
iOS uses one app-private NSUserDefaults string record. The native host's privacy
manifest declares UserDefaults access for app-private information (`CA92.1`),
following [Apple's required-reason API documentation](https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api).
The synchronized Xcode source group includes that manifest as an app resource.
No workout tables, Room schema, or saved weight values are changed.

The shared singleton serializes edits under a mutex and updates only the chosen
field. Successful writes are published after persistence; failures preserve the
previous value. Once a write begins, cancellation cannot interrupt the write and
publication pair. Missing values use defaults; malformed fields fall back
independently without overwriting storage. Read failures propagate for retry.

Settings owns a destination-scoped ViewModel, immutable state, typed callbacks,
and loading/save/error states. Save failures retain the selector for retry.
Cancelling an unsaved rest edit leaves preferences unchanged. Observation ends
when the destination owner is cleared. Settings is retained by navigation saved
state; Back returns to Today. History's visible tab stays disabled until task 5.4
connects its implemented screen.

## Layout and validation

The workspace's corrected Settings spec supplies the three-row order, 20 dp
screen inset, 58 dp minimum rows, 12 dp gaps on both sides of 1 dp dividers, and
anchored navigation. The shell owns real system insets; feature content owns its
padding and scrolling. New selectors/About/license layouts reuse measured tokens
and are documented implementation choices, since they have no Figma export.
Typography uses platform sans-serif in place of Inter.

Common tests cover serialization/reloads, defaults, invalid fields, failures and
retries, concurrent edits, cancellation, unit conversions/rounding, input
validation, ViewModel state and lifecycle, isolated Koin graphs, shell unit
updates, and saved Settings navigation. The approved validation on 2026-10-06
passed 12 preference, 13 Settings, and 10 shared tests, built the Android debug
APK, and linked the iOS simulator framework with:

```sh
./gradlew :core:preferences:testAndroidHostTest :feature:settings:testAndroidHostTest :shared:testAndroidHostTest :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64
```

Reference, narrow, and larger-text previews are supplied. Rendered screenshot,
text-growth/keyboard, Android recreation, and iOS host lifecycle checks were
deferred at the user's request; preview declarations are not visual validation.
