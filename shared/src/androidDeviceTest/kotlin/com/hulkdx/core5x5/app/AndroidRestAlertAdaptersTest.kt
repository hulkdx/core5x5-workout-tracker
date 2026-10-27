package com.hulkdx.core5x5.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import com.hulkdx.core5x5.core.preferences.data.AndroidStartupPermissionPreferences
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertIntent
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertChannels
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertStateStore
import com.hulkdx.core5x5.core.restalerts.data.RestAlertState
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidRestAlertAdaptersTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefix = "rest-alert-test-${UUID.randomUUID()}"
    private val context = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            base.getSharedPreferences("$prefix-$name", mode)
    }

    @AfterTest
    fun cleanUp() {
        base.deleteSharedPreferences("$prefix-rest_alerts")
        base.deleteSharedPreferences("$prefix-startup_permissions")
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.deleteNotificationChannel(AndroidRestAlertChannels.AUDIBLE)
        notificationManager.deleteNotificationChannel(AndroidRestAlertChannels.SILENT)
    }

    @Test
    fun alarmIntentRoundTripsFullWidthValuesAndKeepsAnExplicitStableIdentity() {
        for (deadline in listOf(Long.MIN_VALUE, 0L, Long.MAX_VALUE)) {
            val id = RestAlertId(Long.MAX_VALUE, deadline)
            val intent = AndroidRestAlertIntent.create(context, AndroidRestAlertReceiver::class.java, id)
            assertEquals(id, AndroidRestAlertIntent.decode(intent))
            assertEquals(AndroidRestAlertReceiver::class.java.name, intent.component?.className)
            assertTrue(intent.filterEquals(AndroidRestAlertIntent.create(context, AndroidRestAlertReceiver::class.java, id)))
            assertFalse(intent.filterEquals(AndroidRestAlertIntent.create(context, AndroidRestAlertReceiver::class.java, id.copy(sessionId = 1))))
        }
    }

    @Test
    fun alarmIntentRejectsForeignActionsUrisAndMalformedIdentities() {
        val valid = AndroidRestAlertIntent.create(context, AndroidRestAlertReceiver::class.java, RestAlertId(7, 2_000))
        assertNull(AndroidRestAlertIntent.decode(Intent(valid).setAction("foreign.action")))
        assertNull(AndroidRestAlertIntent.decode(Intent(valid).setData(null)))
        for (uri in listOf(
            "foreign://rest-alert/7/2000", "core5x5://foreign/7/2000",
            "core5x5://rest-alert/7", "core5x5://rest-alert/7/2000/extra",
            "core5x5://rest-alert/0/2000", "core5x5://rest-alert/-1/2000",
            "core5x5://rest-alert/session/2000", "core5x5://rest-alert/7/deadline",
            "core5x5://rest-alert/9223372036854775808/2000",
            "core5x5://rest-alert/7/9223372036854775808",
        )) {
            assertNull(AndroidRestAlertIntent.decode(Intent(valid).setData(Uri.parse(uri))), uri)
        }
    }

    @Test
    fun alertStateSurvivesStoreRecreationAndMalformedRecordsReadAsAbsent() = runTest {
        assertNull(AndroidRestAlertStateStore(context).read())
        for (status in RestAlertState.Status.entries) {
            val state = RestAlertState(RestAlertId(Long.MAX_VALUE, Long.MIN_VALUE), status)
            AndroidRestAlertStateStore(context).write(state)
            assertEquals(state, AndroidRestAlertStateStore(context).read())
        }
        context.getSharedPreferences("rest_alerts", Context.MODE_PRIVATE).edit()
            .putString("current_alert", "malformed").commit()
        assertNull(AndroidRestAlertStateStore(context).read())
    }

    @Test
    fun failedStateCommitPropagatesInsteadOfReportingDurability() = runTest {
        val failingContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val preferences = context.getSharedPreferences(name, mode)
                return object : SharedPreferences by preferences {
                    override fun edit(): SharedPreferences.Editor {
                        val editor = preferences.edit()
                        return object : SharedPreferences.Editor by editor {
                            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                                editor.putString(key, value)
                                return this
                            }
                            override fun commit(): Boolean = false
                        }
                    }
                }
            }
        }
        assertFailsWith<IllegalStateException> {
            AndroidRestAlertStateStore(failingContext).write(RestAlertState(RestAlertId(7, 2_000), RestAlertState.Status.ARMED))
        }
        assertNull(AndroidRestAlertStateStore(context).read())
    }

    @Test
    fun startupPromptHistoryPersistsSeparatelyFromTimingExplanationAndAlertState() = runTest {
        val initial = AndroidStartupPermissionPreferences(context)
        assertFalse(initial.notificationPermissionRequested)
        assertFalse(initial.preciseTimingExplanationShown)
        initial.markNotificationPermissionRequested()
        val recreated = AndroidStartupPermissionPreferences(context)
        assertTrue(recreated.notificationPermissionRequested)
        assertFalse(recreated.preciseTimingExplanationShown)
        recreated.markPreciseTimingExplanationShown()
        assertTrue(AndroidStartupPermissionPreferences(context).preciseTimingExplanationShown)
        assertNull(AndroidRestAlertStateStore(context).read())
    }

    @Test
    fun channelsUseStableSoundDefaultsAndRecreationPreservesExistingImportance() {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        try {
            AndroidRestAlertChannels.create(context)

            assertEquals(
                setOf(AndroidRestAlertChannels.AUDIBLE, AndroidRestAlertChannels.SILENT),
                notificationManager.notificationChannels
                    .map { it.id }
                    .filter { it.startsWith("rest_alerts_") }
                    .toSet(),
            )
            val audible = notificationManager.getNotificationChannel(AndroidRestAlertChannels.AUDIBLE)
            val silent = notificationManager.getNotificationChannel(AndroidRestAlertChannels.SILENT)
            assertEquals("Rest notifications", audible.name)
            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, audible.importance)
            assertEquals(Settings.System.DEFAULT_NOTIFICATION_URI, audible.sound)
            assertEquals("Silent rest notifications", silent.name)
            assertEquals(NotificationManager.IMPORTANCE_LOW, silent.importance)
            assertNull(silent.sound)

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    AndroidRestAlertChannels.AUDIBLE,
                    "Rest notifications",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { setSound(null, null) },
            )
            AndroidRestAlertChannels.create(context)

            val customized = notificationManager.getNotificationChannel(AndroidRestAlertChannels.AUDIBLE)
            assertEquals(NotificationManager.IMPORTANCE_LOW, customized.importance)
            assertEquals(Settings.System.DEFAULT_NOTIFICATION_URI, customized.sound)
        } finally {
            notificationManager.deleteNotificationChannel(AndroidRestAlertChannels.AUDIBLE)
            notificationManager.deleteNotificationChannel(AndroidRestAlertChannels.SILENT)
        }
    }

    @Test
    fun permissionSnapshotStartsUnknownAndRefreshesFromTheCurrentAndroidState() = runTest {
        val permissions = AndroidRestAlertPermissions(context)
        assertEquals(RestAlertPermissionStatus(), permissions.status.value)

        permissions.refreshStatus()

        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val notificationsEnabled = runtimePermissionGranted &&
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        val preciseTimingAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            null
        }
        assertEquals(
            RestAlertPermissionStatus(
                notificationPermission = if (notificationsEnabled) {
                    RestNotificationPermission.GRANTED
                } else {
                    RestNotificationPermission.DENIED
                },
                preciseTimingAllowed = preciseTimingAllowed,
            ),
            permissions.status.value,
        )
    }
}
