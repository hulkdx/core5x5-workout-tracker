import SnapshotTesting
import XCTest

@MainActor
final class ScreenSnapshotTests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
    }

    func testTodayA() { snapshot("todayA") }
    func testTodayB() { snapshot("todayB") }
    func testActiveWorkoutRepCounts() { snapshot("active") }
    func testRunningRest() { snapshot("rest") }
    func testExerciseEditor() { snapshot("editor") }
    func testWorkoutComplete() { snapshot("complete") }
    func testHistory() { snapshot("history") }
    func testSettings() { snapshot("settings") }
    func testSettingsLargeText() { snapshot("settingsLarge") }

    private func snapshot(
        _ scene: String,
        file: StaticString = #filePath,
        testName: String = #function,
        line: UInt = #line
    ) {
        let app = XCUIApplication()
        app.launchEnvironment["CORE5X5_SNAPSHOT_SCENE"] = scene
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        defer { app.terminate() }

        XCTAssertTrue(
            app.otherElements["snapshot-ready"].waitForExistence(timeout: 15),
            "Compose did not present the \(scene) fixture",
            file: file,
            line: line
        )

        // XCTest captures the displayed Metal surface; CALayer.render would miss Compose content.
        let image = app.screenshot().image
        let recording = ProcessInfo.processInfo.environment["CORE5X5_RECORD_SNAPSHOTS"] == "YES"
        withSnapshotTesting(record: recording ? .all : .never) {
            assertSnapshot(of: image, as: .image, file: file, testName: testName, line: line)
        }
    }
}
