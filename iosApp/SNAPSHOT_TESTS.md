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

No iOS reference PNGs have been generated yet. To record the first references,
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
verification. The initial destination is iPhone 16 / iOS 18.6; Xcode 26.1.1 is
available in the development environment. Those tools have not yet compiled or
executed this suite. Xcode package resolution currently stops because the local
Xcode/Apple SDK license has not been accepted. The developer must review and
accept it with `sudo xcodebuild -license` before resolving packages or running
the suite; no `Package.resolved` or iOS PNGs exist yet.

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

The snapshot host exposes a readiness identifier after two Compose frame ticks;
XCTest waits for it before capturing the presented app, including Metal-rendered
content. The status bar is hidden in this configuration to exclude changing
clock/battery indicators. Native safe-area layout remains present, but system
status-bar appearance is outside this suite's coverage. These fixed-state
screenshots do not exercise navigation, persistence, timer recovery, keyboards,
or modal-sheet interactions and do not replace native acceptance checks.
