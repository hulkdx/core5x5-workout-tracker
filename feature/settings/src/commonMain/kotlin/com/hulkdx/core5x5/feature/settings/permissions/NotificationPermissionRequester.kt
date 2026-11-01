package com.hulkdx.core5x5.feature.settings.permissions

import androidx.compose.runtime.Composable

/** Registers a platform permission request and returns its launch action. */
@Composable
expect fun rememberNotificationPermissionRequester(
    onFinished: () -> Unit = {},
): () -> Unit
