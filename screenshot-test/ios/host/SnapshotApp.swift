import SwiftUI

@main
struct SnapshotApp: App {
    var body: some Scene {
        WindowGroup {
            SnapshotContentView()
                .ignoresSafeArea()
                .preferredColorScheme(.dark)
                .statusBarHidden(true)
        }
    }
}
