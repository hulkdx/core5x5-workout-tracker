package com.hulkdx.core5x5.core.common

import android.util.Log

actual object Logger {
    actual fun warn(tag: String, message: String, error: Throwable) {
        Log.w(tag, message, error)
    }
}
