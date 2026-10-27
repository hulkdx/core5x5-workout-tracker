package com.hulkdx.core5x5.core.common

/** Opens OS settings on explicit user actions. Launch failures propagate to the caller. */
interface SystemSettingsLauncher {
    /** Android action; offer only when separate precise-timing access is available. */
    fun openPreciseTimingSettings()
}
