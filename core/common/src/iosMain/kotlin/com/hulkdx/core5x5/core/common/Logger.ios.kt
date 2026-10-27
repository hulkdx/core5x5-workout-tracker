package com.hulkdx.core5x5.core.common

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLog
import platform.Foundation.NSString

actual object Logger {
    @OptIn(ExperimentalForeignApi::class)
    @Suppress("CAST_NEVER_SUCCEEDS")
    actual fun warn(tag: String, message: String, error: Throwable) {
        // Bridge explicitly: a Kotlin String in C varargs would be a UTF-8 pointer.
        NSLog("%@", "$tag: $message\n${error.stackTraceToString()}" as NSString)
    }
}
