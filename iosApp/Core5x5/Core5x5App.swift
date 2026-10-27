import Core5x5Shared
import SwiftUI
import UIKit

@main
struct Core5x5App: App {
    @UIApplicationDelegateAdaptor(Core5x5AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

@MainActor
final class Core5x5AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        IosApplicationKt.startIosApplication()
        return true
    }
}
