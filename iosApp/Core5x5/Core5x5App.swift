import SwiftUI

@main
struct Core5x5App: App {
    var body: some Scene {
        WindowGroup {
            #if SNAPSHOT_TESTING
            SnapshotContentView()
                .ignoresSafeArea()
                .preferredColorScheme(.dark)
                .statusBarHidden(true)
            #else
            ContentView()
            #endif
        }
    }
}
