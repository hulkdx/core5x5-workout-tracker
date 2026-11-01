package com.hulkdx.core5x5.feature.settings.permissions

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberNotificationPermissionRequester(
    onFinished: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val latestOnFinished by rememberUpdatedState(onFinished)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { latestOnFinished() }

    return {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            latestOnFinished()
        } else {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) == PERMISSION_GRANTED) {
                latestOnFinished()
            } else {
                launcher.launch(permission)
            }
        }
    }
}
