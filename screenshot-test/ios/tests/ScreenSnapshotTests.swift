import SnapshotTesting
import UIKit
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
    func testSettingsUnits() { snapshot("settingsUnits") }
    func testSettingsUnitsSaveError() { snapshot("settingsUnitsError") }
    func testSettingsAbout() { snapshot("settingsAbout") }
    func testSettingsAboutError() { snapshot("settingsAboutError") }
    func testSettingsLicense() { snapshot("settingsLicense") }

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

        let root = app.otherElements["snapshot-ready"]
        XCTAssertTrue(
            root.waitForExistence(timeout: 15),
            "Compose did not present the \(scene) fixture",
            file: file,
            line: line
        )

        // XCTest captures the displayed Metal surface; CALayer.render would miss Compose content.
        let screenshot = app.screenshot().image
        guard let pixels = screenshot.cgImage,
              let value = root.value as? String,
              let bottomInset = Double(value), bottomInset > 0,
              app.frame.height > 0 else {
            XCTFail("Missing screenshot or native bottom safe inset", file: file, line: line)
            return
        }
        // The OS home indicator can fade independently of app idleness. Exclude only
        // its native safe-area strip; the app still lays out with the real inset.
        let pixelsPerPoint = Double(pixels.height) / app.frame.height
        let bounds = CGRect(
            x: 0, y: 0, width: pixels.width,
            height: pixels.height - Int((bottomInset * pixelsPerPoint).rounded(.up))
        )
        guard let content = pixels.cropping(to: bounds) else {
            XCTFail("Could not exclude the system home-indicator strip", file: file, line: line)
            return
        }
        let image = UIImage(cgImage: content, scale: screenshot.scale, orientation: screenshot.imageOrientation)
        let testFile = URL(fileURLWithPath: "\(file)")
        let snapshotDirectory = testFile.deletingLastPathComponent()
            .appendingPathComponent("Snapshots")
            .appendingPathComponent(testFile.deletingPathExtension().lastPathComponent)
            .path
        let recording = ProcessInfo.processInfo.environment["CORE5X5_RECORD_SNAPSHOTS"] == "YES"
        withSnapshotTesting(record: recording ? .all : .never) {
            if let failure = verifySnapshot(
                of: image, as: .image, snapshotDirectory: snapshotDirectory,
                file: file, testName: testName, line: line
            ) {
                XCTFail(failure, file: file, line: line)
            }
        }
    }
}
