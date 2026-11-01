package com.hulkdx.core5x5.core.common

expect object Logger {
    fun warn(tag: String, message: String, error: Throwable)
}
