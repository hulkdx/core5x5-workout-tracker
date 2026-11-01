import XCTest

/// Exercises the installed production host; shared rules are covered by Compose/Kotlin tests.
@MainActor
final class RestAlertDeliveryTests: XCTestCase {
    private let app = XCUIApplication(bundleIdentifier: "com.hulkdx.core5x5.verification")
    private let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        let allow = springboard.alerts.buttons["Allow"]
        if allow.waitForExistence(timeout: 3) { allow.tap() }
    }

    func testProductionHostLaunchesAndKeepsAnUnfinishedWorkoutAfterRestart() {
        let start = element("start-workout", containing: "Start Workout")
        let resume = element("resume-workout", containing: "Resume Workout")
        XCTAssertTrue(start.waitForExistence(timeout: 15) || resume.exists, app.debugDescription)
        if start.exists { start.tap() } else { resume.tap() }
        XCTAssertTrue(element("workout-title", containing: "Workout").waitForExistence(timeout: 10), app.debugDescription)
        app.terminate()
        app.launch()
        XCTAssertTrue(resume.waitForExistence(timeout: 10), app.debugDescription)
        resume.tap()
        XCTAssertTrue(element("workout-title", containing: "Workout").waitForExistence(timeout: 10))
    }

    func testForegroundExpiryKeepsTheVisualTimerWithoutABanner() {
        startShortRest()
        waitForRestComplete()
        XCTAssertFalse(springboard.staticTexts["Set complete"].exists)
    }

    func testBackgroundExpiryNotificationTapOpensTheResumableWorkout() {
        startShortRest()
        XCUIDevice.shared.press(.home)
        openExpiryNotificationAndResume(requireResumeEntry: false)
    }

    func testNotificationSurvivesProcessTerminationAndOpensTheNormalEntry() {
        startShortRest()
        XCUIDevice.shared.press(.home)
        app.terminate()
        openExpiryNotificationAndResume(requireResumeEntry: true)
    }

    private func startShortRest() {
        let resume = element("resume-workout", containing: "Resume Workout")
        if resume.waitForExistence(timeout: 5) {
            resume.tap()
        } else {
            element("start-workout", containing: "Start").tap()
        }
        let title = element("workout-title", containing: "Workout")
        XCTAssertTrue(title.waitForExistence(timeout: 10), app.debugDescription)
        // Reset this disposable session through the real program-switch flow.
        let previousTitle = title.label
        element("workout-switch", containing: "Switch to").tap()
        let changed = XCTNSPredicateExpectation(predicate: NSPredicate(format: "label != %@", previousTitle), object: title)
        XCTAssertEqual(XCTWaiter.wait(for: [changed], timeout: 5), .completed)
        element("edit-exercise", containing: "Edit").tap()
        element("edit-rest:2", containing: "Custom").tap()
        let oneMinute = element("duration-value:Minutes:1", containing: "1 Minutes")
        if oneMinute.exists && oneMinute.isHittable { oneMinute.tap() }
        element("duration-value:Minutes:0", containing: "0 Minutes").tap()
        // Reuse the saved override across tests, moving only through visible wheel items.
        for seconds in stride(from: 8, through: 0, by: -2) {
            let value = element("duration-value:Seconds:\(seconds)", containing: "\(seconds) Seconds")
            if value.exists && value.isHittable { value.tap() }
        }
        for seconds in stride(from: 2, through: 10, by: 2) {
            let value = element("duration-value:Seconds:\(seconds)", containing: "\(seconds) Seconds")
            XCTAssertTrue(value.waitForExistence(timeout: 5), app.debugDescription)
            value.tap()
        }
        element("custom-rest-apply", containing: "Apply").tap()
        element("edit-save-changes", containing: "Save Changes").tap()
        let firstSet = element("", containing: "Squat, set 1,")
        XCTAssertTrue(firstSet.waitForExistence(timeout: 5), app.debugDescription)
        firstSet.tap()
        XCTAssertTrue(element("rest-timer", containing: "Rest,").waitForExistence(timeout: 5), app.debugDescription)
    }

    private func waitForRestComplete() {
        let rest = element("rest-timer", containing: "Rest")
        let expired = XCTNSPredicateExpectation(predicate: NSPredicate(format: "label CONTAINS[c] %@", "Rest complete"), object: rest)
        XCTAssertEqual(XCTWaiter.wait(for: [expired], timeout: 20), .completed, app.debugDescription)
    }

    private func openExpiryNotificationAndResume(requireResumeEntry: Bool) {
        let notification = springboard.staticTexts["Set complete"].firstMatch
        XCTAssertTrue(notification.waitForExistence(timeout: 25), springboard.debugDescription)
        notification.tap()
        let resume = element("resume-workout", containing: "Resume Workout")
        if resume.waitForExistence(timeout: 3) {
            resume.tap()
        } else {
            // A warm tap returns to the existing Active Workout; cold entry offers Resume.
            XCTAssertFalse(requireResumeEntry, app.debugDescription)
        }
        waitForRestComplete()
    }

    private func element(_ identifier: String, containing label: String) -> XCUIElement {
        if !identifier.isEmpty {
            let tagged = app.descendants(matching: .any).matching(identifier: identifier).firstMatch
            if tagged.exists { return tagged }
        }
        return app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS[c] %@", label)).firstMatch
    }
}
