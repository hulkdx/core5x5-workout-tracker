package com.hulkdx.core5x5.app

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.feature.workout.domain.RestAlertObserver

class AppLifecycleHandler(
    private val restAlertPermissions: RestAlertPermissions,
    private val restAlertObserver: RestAlertObserver,
) {
    suspend fun onResume() {
        restAlertPermissions.onResume()
        restAlertObserver.onResume()
    }
}
