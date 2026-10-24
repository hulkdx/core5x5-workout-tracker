import Core5x5Screenshots
import SwiftUI
import UIKit

// Only the dedicated snapshot app target compiles this host.
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
