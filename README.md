# Core5x5

<a href="https://play.google.com/store/apps/details?id=com.hulkdx.core5x5">
  <img
    alt="Get it on Google Play"
    src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
    width="190"
  />
</a>

A 5×5 strength-training tracker for Android and iOS, built with Kotlin Multiplatform and Compose Multiplatform.


## Build

Use JDK 21 and an Android SDK for Android builds. Open this repository root in
Android Studio or run its Gradle wrapper:

```bash
./gradlew :androidApp:assembleDebug
```

For iOS, open [Core5x5.xcodeproj](iosApp/Core5x5.xcodeproj) in Xcode and run the `Core5x5` scheme. Running on a physical device requires your own signing configuration.

The Xcode build phase invokes Gradle to build `Core5x5Shared`. iOS development
requires macOS, Xcode and an installed simulator runtime for simulator runs.

## Code and tests

[Feature modules](feature/) own application behavior and presentation;
[core UI](core/ui/) owns reusable visual components. `shared/` composes
the app and native hosts; dependency versions live in
[the version catalog](gradle/libs.versions.toml).

Run focused Kotlin tests with `./gradlew :feature:workout:testAndroidHostTest`.
On-disk Room tests use `:feature:workout:connectedAndroidTest` with an Android
device/emulator or `:feature:workout:iosSimulatorArm64Test` on macOS. Shared Compose
journeys use `:ui-test:connectedAndroidTest` and `:ui-test:iosSimulatorArm64Test`;
their test-host reopening checks do not simulate native process termination.

## Screenshot tests

To validate screenshots:
```bash
./gradlew :screenshot-test:validateDebugScreenshotTest
```

To record screenshots:

```bash
./gradlew :screenshot-test:updateDebugScreenshotTest
```

Updating references accepts the current rendered previews as the new baseline. Review the changed images before committing them.

For native iOS snapshots, select the shared `Core5x5Snapshots` scheme in Xcode
and run Test, or run from this repository root:

```bash
xcodebuild test \
  -project iosApp/Core5x5.xcodeproj \
  -scheme Core5x5Snapshots \
  -configuration Snapshot \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.6' \
  -parallel-testing-enabled NO \
  -only-testing:Core5x5SnapshotTests \
  CODE_SIGNING_ALLOWED=NO
```

This invokes Gradle with `-PiosSnapshots=true`. To record references, append
`CORE5X5_RECORD_SNAPSHOTS=YES`; recording deliberately reports failures. Review
the images in [the reference directory](iosApp/Core5x5SnapshotTests/__Snapshots__/ScreenSnapshotTests/),
then rerun with recording off. Match Xcode, SDK, device, runtime and display
settings; existing references use Xcode 27.1 (27A9275), the iOS 27.1 SDK and
iPhone 16 / iOS 18.6, with English/US and portrait orientation.

Fixtures and screenshot baselines live in this repository; builds and static
captures do not replace native interaction/lifecycle checks.

## License

[MIT](LICENSE)
