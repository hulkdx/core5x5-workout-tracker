package com.hulkdx.core5x5.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

class AndroidSystemSettingsLauncher(
    private val context: Context,
) : SystemSettingsLauncher {
    override fun openPreciseTimingSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
