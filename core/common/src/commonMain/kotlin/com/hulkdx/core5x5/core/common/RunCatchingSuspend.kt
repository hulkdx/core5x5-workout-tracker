package com.hulkdx.core5x5.core.common

import kotlinx.coroutines.CancellationException

/** Captures exceptions while preserving cancellation and allowing serious errors to propagate. */
suspend fun <T> runCatchingSuspend(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
