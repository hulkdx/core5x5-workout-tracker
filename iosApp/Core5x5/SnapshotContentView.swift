#if SNAPSHOT_TESTING
import Core5x5Shared
import SwiftUI
import UIKit

// This host and the Kotlin fixtures exist only in the Snapshot configuration.
struct SnapshotContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let scene = ProcessInfo.processInfo.environment["CORE5X5_SNAPSHOT_SCENE"] ?? "todayA"
        weak var snapshotController: UIViewController?
        let controller = SnapshotViewControllerKt.SnapshotViewController(scene: scene) {
            guard let view = snapshotController?.view, let window = view.window else { return }
            view.accessibilityValue = String(describing: window.safeAreaInsets.bottom)
            view.accessibilityIdentifier = "snapshot-ready"
        }
        snapshotController = controller
        controller.overrideUserInterfaceStyle = .dark
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
#endif
