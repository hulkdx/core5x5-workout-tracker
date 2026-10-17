# iOS screenshot tests

Select the shared **Core5x5Snapshots** scheme and run Test in Xcode, or run the
command below from the Gradle project root. These are XCTest UI tests using
[Point-Free SnapshotTesting 1.19.6](https://github.com/pointfreeco/swift-snapshot-testing/releases/tag/1.19.6)
to compare actual simulator app screenshots. The Android preview screenshot
suite is independent.

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

This command runs `:shared:embedAndSignAppleFrameworkForXcode -PiosSnapshots=true`
through the existing Xcode build phase. Agents must obtain task-specific approval
before running it, including the recording variant below.

## Recording and reviewing references

Nine visually reviewed iOS reference PNGs are included. To update references,
append `CORE5X5_RECORD_SNAPSHOTS=YES` to the command above. In Xcode, temporarily
set that build setting to `YES` for the test run. Recording deliberately reports
test failures so that newly written images cannot be mistaken for a successful
comparison. Review every image before accepting it, then rerun with recording off.
Normal verification uses `.never` and fails on missing or changed images without
replacing any reference.

Images are stored beside the Swift tests in
`Core5x5SnapshotTests/__Snapshots__/ScreenSnapshotTests/`. Keep reviewed PNGs and
the generated shared `Package.resolved` in the application repository. Do not
commit DerivedData, `.xcresult` reports, or simulator state. Use exactly the same
Xcode version, device, iOS runtime, and display settings for recording and
verification. The references were recorded with Xcode 27.1 (27A9275), the iOS
27.1 SDK, and iPhone 16 / iOS 18.6. Each compared image is 1178 × 2454px after
excluding the system home-indicator strip. Comparisons use exact pixel matching.

The UI-test target intentionally clears `OTHER_SWIFT_FLAGS` to avoid Xcode's
inherited `Testing=_Testing_Unavailable` alias, which conflicts with
SnapshotTesting's Swift Testing dependency. The workaround is confined to this
target; see the [upstream discussion](https://github.com/pointfreeco/swift-snapshot-testing/discussions/901).

## Coverage and isolation

Nine cases cover Today A/B, Active Workout with full/partial reps and incomplete
sets, a fixed running rest timer, exercise editing, partial workout completion,
populated History, Settings, and Settings with explicit Compose font scale 2.
The remaining cases force font scale 1; all launches use English/US and portrait.
Larger text currently uses the same phone viewport; small-phone coverage is not
yet included.

The `Snapshot` configuration uses the separate `com.hulkdx.core5x5.snapshots`
bundle ID and compiles `src/iosSnapshot/kotlin` in Shared and the three features
only when `-PiosSnapshots=true` is present. Each feature constructs its internal
screen state in its own module. Shared wraps top-level screens in the production
navigation container, using native safe insets. These fixtures start no Koin
graph, database, preferences, ViewModel, or real timer. Normal Debug and Release
builds exclude the snapshot host/fixtures; SnapshotTesting links only to the UI
test target. Do not distribute the Snapshot configuration.

The snapshot host exposes a readiness identifier and the native window's bottom
safe inset after two Compose frame ticks. XCTest waits for it before capturing
the presented app, including Metal-rendered content. SwiftUI hides the status bar
to exclude changing clock/battery indicators. The comparison crops only the
bottom safe-area strip containing the OS home indicator, whose visibility can
change independently of app idleness. App layout still uses the real full window
and native safe insets; system status-bar/home-indicator appearance is outside
this suite's coverage. The larger-text Settings reference exposes existing
inside-word wrapping of History/Settings navigation labels; accepting that
baseline does not approve the layout issue. These fixed-state
screenshots do not exercise navigation, persistence, timer recovery, keyboards,
or modal-sheet interactions and do not replace native acceptance checks.
