package com.hulkdx.core5x5

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        App { darkTheme, followSystem ->
            controller.overrideUserInterfaceStyle = when {
                followSystem -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
                darkTheme -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
                else -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            }
        }
    }
    return controller
}
