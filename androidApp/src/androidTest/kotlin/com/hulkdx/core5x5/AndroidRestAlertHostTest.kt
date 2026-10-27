package com.hulkdx.core5x5

import android.accessibilityservice.AccessibilityService
import android.Manifest
import android.app.AlarmManager
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.KeyEvent
import android.view.Surface
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertChannels
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertAlarm
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertIntent
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertNotification
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertPlatform
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertStateStore
import com.hulkdx.core5x5.core.restalerts.data.PersistedRestAlertScheduler
import com.hulkdx.core5x5.core.restalerts.data.RestAlertPlatform
import com.hulkdx.core5x5.core.restalerts.data.RestAlertState
import com.hulkdx.core5x5.core.restalerts.data.RestAlertStateStore
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import com.hulkdx.core5x5.core.common.Logger
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertScheduler
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.app.AndroidRestAlertReceiver
import com.hulkdx.core5x5.app.launchAndroidRestAlertReceiverWork
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.UUID
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.dsl.koinApplication
import com.hulkdx.core5x5.di.androidRestAlertModule
import com.hulkdx.core5x5.di.appModule
import kotlin.coroutines.CoroutineContext

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AndroidRestAlertHostTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun applicationOwnsStableChannelsAndOneGraphAcrossActivityRecreation() {
        val channels = notificationManager.notificationChannels.map { it.id }.toSet()
        assertEquals(setOf(AndroidRestAlertChannels.AUDIBLE, AndroidRestAlertChannels.SILENT), channels)

        context.getSharedPreferences("startup_permissions", Context.MODE_PRIVATE).edit()
            .putBoolean("notification_permission_requested", true)
            .putBoolean("precise_timing_explanation_shown", true)
            .commit()

        val koin = GlobalContext.get()
        val scheduler = koin.get<PersistedRestAlertScheduler>()
        val scope = koin.get<CoroutineScope>()
        assertSame(scheduler, koin.get<RestAlertScheduler>())

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity {
                assertSame(scheduler, koin.get<PersistedRestAlertScheduler>())
                assertSame(scope, koin.get<CoroutineScope>())
            }
            scenario.recreate()
            scenario.onActivity {
                assertSame(scheduler, koin.get<RestAlertScheduler>())
                assertSame(scope, koin.get<CoroutineScope>())
            }
        } finally {
            scenario.close()
        }

        val receiver = context.packageManager.getReceiverInfo(
            ComponentName(context, AndroidRestAlertReceiver::class.java),
            0,
        )
        assertFalse(receiver.exported)
    }

    @Test
    fun isolatedApplicationScopeContainsChildFailuresAndClosesWithItsGraph() {
        val application = koinApplication {
            androidContext(context)
            modules(appModule, androidRestAlertModule)
        }
        val scope = application.koin.get<CoroutineScope>()
        val job = checkNotNull(scope.coroutineContext[Job])
        val failure = IllegalStateException("child failed")
        val child = scope.async { throw failure }

        try {
            val caught = runBlocking {
                try {
                    child.await()
                    null
                } catch (error: IllegalStateException) {
                    error
                }
            }
            assertSame(failure, caught)
            assertTrue(job.isActive)
        } finally {
            application.close()
        }

        assertTrue(job.isCancelled)
    }

    @Test
    fun notificationUsesTheRequestedChannelCopyIconAndNormalLauncherIntent() =
        withNotificationPermission {
            notificationManager.cancel(REST_NOTIFICATION_ID)
            assertTrue("A prior rest notification remained active", awaitNoRestNotification())
            AndroidRestAlertNotification.show(context, soundEnabled = true)

            val audible = checkNotNull(awaitRestNotification())
            assertEquals(AndroidRestAlertChannels.AUDIBLE, audible.notification.channelId)
            assertEquals("Set complete", audible.notification.extras.getString(Notification.EXTRA_TITLE))
            assertEquals("Ready for your next set.", audible.notification.extras.getString(Notification.EXTRA_TEXT))
            assertEquals(android.R.drawable.ic_lock_idle_alarm, audible.notification.smallIcon.resId)
            assertTrue(audible.notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
            assertEquals(normalLauncherPendingIntent(), audible.notification.contentIntent)

            notificationManager.cancel(REST_NOTIFICATION_ID)
            assertTrue("The audible rest notification remained active", awaitNoRestNotification())
            AndroidRestAlertNotification.show(context, soundEnabled = false)
            AndroidRestAlertNotification.show(context, soundEnabled = false)
            val silent = checkNotNull(awaitRestNotification())
            assertEquals(AndroidRestAlertChannels.SILENT, silent.notification.channelId)
            assertNull(silent.notification.sound)
            assertNull(silent.notification.vibrate)
            assertEquals(0, silent.notification.defaults and Notification.DEFAULT_SOUND)
            assertEquals(1, notificationManager.activeNotifications.count { it.id == REST_NOTIFICATION_ID })
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun notificationTapFromSystemShadeOpensNormalEntryWithWorkoutResumable() = withNotificationPermission {
        val startupPreferences = context.getSharedPreferences("startup_permissions", Context.MODE_PRIVATE)
        val hadRequestedPermission = startupPreferences.contains("notification_permission_requested")
        val hadShownTimingExplanation = startupPreferences.contains("precise_timing_explanation_shown")
        val previouslyRequestedPermission = startupPreferences.getBoolean("notification_permission_requested", false)
        val previouslyShownTimingExplanation = startupPreferences.getBoolean("precise_timing_explanation_shown", false)
        val workouts = GlobalContext.get().get<WorkoutRepository>()
        val existing = runBlocking { workouts.getUnfinishedWorkout() }
        if (existing != null) runBlocking { workouts.finalizeWorkout(existing.id) }
        val started = runBlocking { workouts.startWorkout(Workout.A) }
        val activityMonitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        notificationManager.cancel(REST_NOTIFICATION_ID)
        startupPreferences.edit()
            .putBoolean("notification_permission_requested", true)
            .putBoolean("precise_timing_explanation_shown", true)
            .commit()

        try {
            assertTrue("A prior rest notification remained active", awaitNoRestNotification())
            AndroidRestAlertNotification.show(context, soundEnabled = false)
            assertNotNull(awaitRestNotification())
            assertTrue(
                "Could not open the notification shade",
                instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS),
            )
            waitForSystemSettingsText("Set complete")
            clickSystemSettingsText("Set complete")

            val openedActivity = instrumentation.waitForMonitorWithTimeout(activityMonitor, 10_000)
            try {
                assertNotNull("The notification tap did not launch the normal app entry", openedActivity)
                waitForSystemSettingsText("Resume Workout")
            } finally {
                openedActivity?.finish()
            }
        } finally {
            instrumentation.removeMonitor(activityMonitor)
            notificationManager.cancel(REST_NOTIFICATION_ID)
            runBlocking { workouts.finalizeWorkout(started.id) }
            startupPreferences.edit().apply {
                if (hadRequestedPermission) putBoolean("notification_permission_requested", previouslyRequestedPermission)
                else remove("notification_permission_requested")
                if (hadShownTimingExplanation) putBoolean("precise_timing_explanation_shown", previouslyShownTimingExplanation)
                else remove("precise_timing_explanation_shown")
            }.commit()
        }
    }

    @Test
    fun deniedRuntimeNotificationPermissionSuppressesPosting() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        notificationManager.cancel(REST_NOTIFICATION_ID)
        val deniedContext = object : ContextWrapper(context) {
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
                if (permission == Manifest.permission.POST_NOTIFICATIONS) {
                    PackageManager.PERMISSION_DENIED
                } else {
                    super.checkPermission(permission, pid, uid)
                }
        }

        AndroidRestAlertNotification.show(deniedContext, soundEnabled = true)
        assertNull(activeRestNotification())
    }

    @Test
    fun channelsAreCreatedWhenNotificationPermissionIsDenied() {
        val deniedContext = object : ContextWrapper(context) {
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
                if (permission == Manifest.permission.POST_NOTIFICATIONS) {
                    PackageManager.PERMISSION_DENIED
                } else {
                    super.checkPermission(permission, pid, uid)
                }
        }

        notificationManager.deleteNotificationChannel(AndroidRestAlertChannels.AUDIBLE)
        notificationManager.deleteNotificationChannel(AndroidRestAlertChannels.SILENT)
        try {
            AndroidRestAlertChannels.create(deniedContext)

            assertEquals(
                setOf(AndroidRestAlertChannels.AUDIBLE, AndroidRestAlertChannels.SILENT),
                notificationManager.notificationChannels.map { it.id }.toSet(),
            )
        } finally {
            // Leave the isolated verification app in its normal startup state.
            AndroidRestAlertChannels.create(context)
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.R)
    fun systemSettingsChannelRegistrationPreservesChoicesAndSoundOffStaysSilentBeforeApi31() =
        verifySystemSettingsChannelRegistrationPreservesChoicesAndSoundOffStaysSilent()

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun systemSettingsChannelRegistrationPreservesChoicesAndSoundOffStaysSilentFromApi33() =
        verifySystemSettingsChannelRegistrationPreservesChoicesAndSoundOffStaysSilent()

    private fun verifySystemSettingsChannelRegistrationPreservesChoicesAndSoundOffStaysSilent() =
        withNotificationPermission {
        notificationManager.cancel(REST_NOTIFICATION_ID)

        try {
            resetRestAlertChannels()
            setChannelAlertingInSystemSettings(AndroidRestAlertChannels.AUDIBLE, expectedAlerting = false)
            setChannelAlertingInSystemSettings(AndroidRestAlertChannels.SILENT, expectedAlerting = true)

            assertFalse(
                waitForChannelAlerting(AndroidRestAlertChannels.AUDIBLE, expectedAlerting = false).importance >
                    NotificationManager.IMPORTANCE_LOW,
            )
            assertTrue(waitForChannelAlerting(AndroidRestAlertChannels.SILENT, expectedAlerting = true).sound != null)

            AndroidRestAlertChannels.create(context)
            assertFalse(
                waitForChannelAlerting(AndroidRestAlertChannels.AUDIBLE, expectedAlerting = false).importance >
                    NotificationManager.IMPORTANCE_LOW,
            )
            assertTrue(waitForChannelAlerting(AndroidRestAlertChannels.SILENT, expectedAlerting = true).sound != null)

            AndroidRestAlertNotification.show(context, soundEnabled = false)
            val notification = checkNotNull(awaitRestNotification())
            assertEquals(AndroidRestAlertChannels.SILENT, notification.notification.channelId)
            assertNull(notification.notification.sound)
            assertNull(notification.notification.vibrate)
            assertEquals(0, notification.notification.defaults and Notification.DEFAULT_SOUND)
        } finally {
            notificationManager.cancel(REST_NOTIFICATION_ID)
            resetRestAlertChannels()
        }
    }

    @Test
    fun alarmsScheduleAtSupportedApisAndCancelOnlyTheirIdentity() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val receiver = AndroidRestAlertReceiver::class.java
        val first = RestAlertId(71, System.currentTimeMillis() + 5 * 60_000)
        val second = RestAlertId(72, first.deadlineEpochMillis + 1)
        val bestEffort = RestAlertId(73, first.deadlineEpochMillis + 2)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            try {
                AndroidRestAlertAlarm.schedule(context, receiver, first)
                assertTrue(alarmPendingIntent(first) != null)
                AndroidRestAlertAlarm.cancel(context, receiver, first)
                AndroidRestAlertAlarm.cancel(context, receiver, first)
                assertTrue(alarmPendingIntent(first) != null)
            } finally {
                AndroidRestAlertAlarm.cancel(context, receiver, first)
            }
            return
        }

        val originalExactAccess = alarmManager.canScheduleExactAlarms()
        try {
            if (!originalExactAccess) {
                assertFalse(alarmManager.canScheduleExactAlarms())
                AndroidRestAlertAlarm.schedule(context, receiver, bestEffort)
                assertTrue(alarmPendingIntent(bestEffort) != null)
                AndroidRestAlertAlarm.cancel(context, receiver, bestEffort)
                // AlarmManager.cancel removes the alarm; Android retains its PendingIntent token.
                assertTrue(alarmPendingIntent(bestEffort) != null)

                // Grant only after the denied path. Revoking this special access can stop
                // the process on newer Android versions, so leave the isolated test app granted.
                setExactAlarmAccess(true)
            }

            assertTrue(alarmManager.canScheduleExactAlarms())
            AndroidRestAlertAlarm.schedule(context, receiver, first)
            AndroidRestAlertAlarm.schedule(context, receiver, first)
            AndroidRestAlertAlarm.schedule(context, receiver, second)
            val firstPendingIntent = alarmPendingIntent(first)
            assertTrue(firstPendingIntent != null)
            assertEquals(firstPendingIntent, alarmPendingIntent(first))
            assertTrue(alarmPendingIntent(second) != null)

            AndroidRestAlertAlarm.cancel(context, receiver, first)
            AndroidRestAlertAlarm.cancel(context, receiver, first)
            assertTrue(alarmPendingIntent(first) != null)
            assertTrue(alarmPendingIntent(second) != null)
            val absent = RestAlertId(74, first.deadlineEpochMillis)
            AndroidRestAlertAlarm.cancel(context, receiver, absent)
            assertNull(alarmPendingIntent(absent))
            assertTrue(alarmPendingIntent(second) != null)
        } finally {
            AndroidRestAlertAlarm.cancel(context, receiver, first)
            AndroidRestAlertAlarm.cancel(context, receiver, second)
            AndroidRestAlertAlarm.cancel(context, receiver, bestEffort)
        }
    }

    @Test
    fun receiverPresentsExpiredRestAndFinishesTheOrderedBroadcast() = withNotificationPermission {
        notificationManager.cancel(REST_NOTIFICATION_ID)
        val koin = GlobalContext.get()
        val workouts = koin.get<WorkoutRepository>()
        val preferences = koin.get<TrainingPreferencesRepository>()
        val stateStore = AndroidRestAlertStateStore(context)
        val existing = runBlocking { workouts.getUnfinishedWorkout() }
        if (existing != null) runBlocking { workouts.finalizeWorkout(existing.id) }
        val started = runBlocking { workouts.startWorkout(Workout.A) }

        try {
            runBlocking {
                preferences.setRestNotificationsEnabled(true)
                preferences.setRestSoundEnabled(false)
            }
            val resting = requireNotNull(
                runBlocking { workouts.completeSetAndStartRest(started.id, 0, 0, 500) },
            )
            val deadline = checkNotNull(resting.restTimer?.deadlineEpochMillis)
            val id = RestAlertId(resting.id, deadline)
            runBlocking { delay((deadline - System.currentTimeMillis() + 100).coerceAtLeast(0L)) }
            runBlocking { stateStore.write(RestAlertState(id, RestAlertState.Status.ARMED)) }

            val completed = CountDownLatch(1)
            val finalReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    completed.countDown()
                }
            }
            context.sendOrderedBroadcast(
                AndroidRestAlertIntent.create(context, AndroidRestAlertReceiver::class.java, id),
                null,
                finalReceiver,
                Handler(Looper.getMainLooper()),
                Activity.RESULT_OK,
                null,
                null,
            )

            assertTrue("Receiver did not finish its ordered broadcast", completed.await(10, TimeUnit.SECONDS))
            assertEquals(RestAlertState.Status.HANDLED, runBlocking { stateStore.read() }?.status)
            val notification = checkNotNull(awaitRestNotification())
            assertEquals(AndroidRestAlertChannels.SILENT, notification.notification.channelId)
        } finally {
            runBlocking { workouts.finalizeWorkout(started.id) }
            notificationManager.cancel(REST_NOTIFICATION_ID)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P, maxSdkVersion = Build.VERSION_CODES.R)
    fun expiredRestNotifiesFromTodayActiveWorkoutSettingsHistoryAndBackgroundBeforeApi31() =
        verifyRestExpiryAcrossScreensAndBackground()

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun expiredRestNotifiesFromTodayActiveWorkoutSettingsHistoryAndBackgroundFromApi33() =
        verifyRestExpiryAcrossScreensAndBackground()

    private fun verifyRestExpiryAcrossScreensAndBackground() = withNotificationPermission {
        val startupPreferences = context.getSharedPreferences("startup_permissions", Context.MODE_PRIVATE)
        val hadRequestedPermission = startupPreferences.contains("notification_permission_requested")
        val hadShownTimingExplanation = startupPreferences.contains("precise_timing_explanation_shown")
        val previouslyRequestedPermission = startupPreferences.getBoolean("notification_permission_requested", false)
        val previouslyShownTimingExplanation = startupPreferences.getBoolean("precise_timing_explanation_shown", false)
        val workouts = GlobalContext.get().get<WorkoutRepository>()
        val preferences = GlobalContext.get().get<TrainingPreferencesRepository>()
        val scheduler = GlobalContext.get().get<PersistedRestAlertScheduler>()
        val previousPreferences = runBlocking { preferences.getPreferences() }
        val existing = runBlocking { workouts.getUnfinishedWorkout() }
        if (existing != null) runBlocking { workouts.finalizeWorkout(existing.id) }
        val started = runBlocking { workouts.startWorkout(Workout.A) }

        startupPreferences.edit()
            .putBoolean("notification_permission_requested", true)
            .putBoolean("precise_timing_explanation_shown", true)
            .commit()
        runBlocking {
            preferences.setRestNotificationsEnabled(true)
            preferences.setRestSoundEnabled(false)
        }
        notificationManager.cancel(REST_NOTIFICATION_ID)

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForVisibleText("Resume Workout")
            deliverRestExpiryAndAssertNotification(workouts, scheduler, started.id, setPosition = 0)

            clickVisibleText("Resume Workout")
            waitForVisibleText("Switch to B")
            deliverRestExpiryAndAssertNotification(workouts, scheduler, started.id, setPosition = 1)
            waitForVisibleText("Rest complete")

            clickVisibleText("Back to Today")
            waitForVisibleText("Resume Workout")
            clickVisibleText("Settings")
            waitForVisibleText("Sound")
            deliverRestExpiryAndAssertNotification(workouts, scheduler, started.id, setPosition = 2)

            clickVisibleText("History")
            waitForHeadingText("History")
            deliverRestExpiryAndAssertNotification(workouts, scheduler, started.id, setPosition = 3)

            assertTrue(
                "Could not move the app to the background",
                instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME),
            )
            awaitActivityLosesWindowFocus(scenario)
            deliverRestExpiryAndAssertNotification(workouts, scheduler, started.id, setPosition = 4)

            val launcher = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launcher)
            waitForHeadingText("History")
        } finally {
            scenario.close()
            runBlocking { scheduler.cancel() }
            notificationManager.cancel(REST_NOTIFICATION_ID)
            assertTrue("A rest notification remained active after cleanup", awaitNoRestNotification())
            runBlocking { workouts.finalizeWorkout(started.id) }
            runBlocking {
                preferences.setRestNotificationsEnabled(previousPreferences.restNotificationsEnabled)
                preferences.setRestSoundEnabled(previousPreferences.restSoundEnabled)
            }
            startupPreferences.edit().apply {
                if (hadRequestedPermission) putBoolean("notification_permission_requested", previouslyRequestedPermission)
                else remove("notification_permission_requested")
                if (hadShownTimingExplanation) putBoolean("precise_timing_explanation_shown", previouslyShownTimingExplanation)
                else remove("precise_timing_explanation_shown")
            }.commit()
        }
    }

    @Test
    fun hostOrchestratedColdStartVerificationStages() {
        val stage = InstrumentationRegistry.getArguments().getString("restAlertHostStage")
        // The Gradle suite includes this method as a no-op; the host script supplies a stage
        // because force-stopping the target app would otherwise kill its instrumentation runner.
        if (stage == null) return
        check(stage in setOf(
            "prepare-channels", "verify-channels", "prepare-notification",
            "prepare-cold-receiver", "verify-cold-receiver", "cleanup",
        )) { "Unsupported cold-start verification stage: $stage" }

        when (stage) {
            "prepare-channels" -> prepareColdRestartChannelChoices()
            "verify-channels" -> verifyColdRestartChannelChoices()
            "prepare-notification" -> prepareColdNotificationTap()
            "prepare-cold-receiver" -> prepareColdReceiverDelivery()
            "verify-cold-receiver" -> verifyColdReceiverDelivery()
            "cleanup" -> cleanupColdStartVerification()
        }
    }

    @Test
    fun receiverWorkFinishesOnlyAfterSlowSchedulerStorageCompletes() {
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        val id = RestAlertId(sessionId = 91, deadlineEpochMillis = System.currentTimeMillis() - 1_000)
        val request = RestAlertRequest(id = id, backgroundEnabled = true, soundEnabled = false)
        val storageWriteStarted = CountDownLatch(1)
        val releaseStorageWrite = CompletableDeferred<Unit>()
        val receiverFinished = CountDownLatch(1)
        val notificationPresented = CountDownLatch(1)
        var savedState: RestAlertState? = RestAlertState(id, RestAlertState.Status.ARMED)
        val stateStore = object : RestAlertStateStore {
            override suspend fun read(): RestAlertState? = savedState

            override suspend fun write(state: RestAlertState) {
                if (state.status == RestAlertState.Status.HANDLED) {
                    storageWriteStarted.countDown()
                    releaseStorageWrite.await()
                }
                savedState = state
            }
        }
        val platform = object : RestAlertPlatform {
            override suspend fun schedule(request: RestAlertRequest, wasArmed: Boolean) = Unit
            override suspend fun cancel(id: RestAlertId) = Unit
            override suspend fun cancelNotification() = Unit
        }
        val scheduler = PersistedRestAlertScheduler(
            stateStore = stateStore,
            platform = platform,
            nowEpochMillis = { System.currentTimeMillis() },
        )

        try {
            launchAndroidRestAlertReceiverWork(
                scope = scope,
                work = {
                    scheduler.handleExpiry(
                        id = id,
                        validate = { request },
                        present = { notificationPresented.countDown() },
                    )
                },
                onFailure = { throw AssertionError("Slow scheduler storage failed", it) },
                onFinished = receiverFinished::countDown,
            )

            assertTrue("Scheduler did not reach its HANDLED state write", storageWriteStarted.await(5, TimeUnit.SECONDS))
            assertFalse("The receiver finished while scheduler storage was still blocked", receiverFinished.await(100, TimeUnit.MILLISECONDS))
            assertFalse("The notification was presented before HANDLED storage completed", notificationPresented.await(100, TimeUnit.MILLISECONDS))
            releaseStorageWrite.complete(Unit)
            assertTrue("The notification was not presented after scheduler storage completed", notificationPresented.await(5, TimeUnit.SECONDS))
            assertTrue("The receiver did not finish after scheduler storage completed", receiverFinished.await(5, TimeUnit.SECONDS))
            assertEquals(RestAlertState.Status.HANDLED, savedState?.status)
        } finally {
            releaseStorageWrite.complete(Unit)
            scope.coroutineContext[Job]?.cancel()
        }
    }

    @Test
    fun cancellingRestNotificationLeavesOtherNotificationsAndCanRepeat() = withNotificationPermission {
        notificationManager.cancel(REST_NOTIFICATION_ID)
        notificationManager.cancel(OTHER_NOTIFICATION_ID)
        AndroidRestAlertNotification.show(context, soundEnabled = false)
        notificationManager.notify(
            OTHER_NOTIFICATION_ID,
            Notification.Builder(context, AndroidRestAlertChannels.AUDIBLE)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("Unrelated test notification")
                .build(),
        )

        val platform = AndroidRestAlertPlatform(context, AndroidRestAlertReceiver::class.java)
        try {
            runBlocking { platform.cancelNotification() }
            runBlocking { platform.cancelNotification() }

            assertTrue("The rest notification remained active after cancellation", awaitNoRestNotification())
            assertTrue(notificationManager.activeNotifications.any { it.id == OTHER_NOTIFICATION_ID })
        } finally {
            notificationManager.cancel(REST_NOTIFICATION_ID)
            notificationManager.cancel(OTHER_NOTIFICATION_ID)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun aFirstLaunchNotificationDenialDoesNotAskForPreciseTiming() {
        val startupPreferences = context.getSharedPreferences("startup_permissions", Context.MODE_PRIVATE)
        val hadRequestedPermission = startupPreferences.contains("notification_permission_requested")
        val hadShownTimingExplanation = startupPreferences.contains("precise_timing_explanation_shown")
        val previouslyRequestedPermission = startupPreferences.getBoolean("notification_permission_requested", false)
        val previouslyShownTimingExplanation = startupPreferences.getBoolean("precise_timing_explanation_shown", false)
        val permissionWasGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val preciseTimingWasAllowed = alarmManager.canScheduleExactAlarms()
        val display = checkNotNull(
            context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY),
        )
        val originalRotation = display.rotation

        try {
            clearNotificationPermissionFlags()
            instrumentation.uiAutomation.revokeRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            startupPreferences.edit()
                .putBoolean("notification_permission_requested", false)
                .putBoolean("precise_timing_explanation_shown", false)
                .commit()
            if (preciseTimingWasAllowed) setExactAlarmAccess(allowed = false)
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForSystemSettingsText("Don’t allow")
                assertTrue(instrumentation.uiAutomation.setRotation(Surface.ROTATION_90))
                waitForSystemSettingsText("Don’t allow")
                assertTrue(instrumentation.uiAutomation.setRotation(originalRotation))
                clickSystemSettingsText("Don’t allow")

                assertSystemTextNotVisible("Open settings")
                assertSystemTextNotVisible("Not now")
                assertFalse(
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED,
                )
                assertFalse(startupPreferences.getBoolean("precise_timing_explanation_shown", false))
            } finally {
                scenario.close()
            }

            val reopenedScenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                assertSystemTextNotVisible("Don’t allow")
                assertSystemTextNotVisible("Open settings")
                assertSystemTextNotVisible("Not now")
                assertFalse(
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED,
                )
                assertTrue(startupPreferences.getBoolean("notification_permission_requested", false))
                assertFalse(startupPreferences.getBoolean("precise_timing_explanation_shown", false))
            } finally {
                reopenedScenario.close()
            }
        } finally {
            instrumentation.uiAutomation.setRotation(originalRotation)
            clearNotificationPermissionFlags()
            if (permissionWasGranted) {
                instrumentation.uiAutomation.grantRuntimePermission(
                    context.packageName,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            } else {
                instrumentation.uiAutomation.revokeRuntimePermission(
                    context.packageName,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
            if (preciseTimingWasAllowed) setExactAlarmAccess(allowed = true)
            startupPreferences.edit().apply {
                if (hadRequestedPermission) putBoolean("notification_permission_requested", previouslyRequestedPermission)
                else remove("notification_permission_requested")
                if (hadShownTimingExplanation) putBoolean("precise_timing_explanation_shown", previouslyShownTimingExplanation)
                else remove("precise_timing_explanation_shown")
            }.commit()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun aGrantedNotificationPermissionShowsPreciseTimingExplanationOnce() {
        val startupPreferences = context.getSharedPreferences("startup_permissions", Context.MODE_PRIVATE)
        val hadRequestedPermission = startupPreferences.contains("notification_permission_requested")
        val hadShownTimingExplanation = startupPreferences.contains("precise_timing_explanation_shown")
        val previouslyRequestedPermission = startupPreferences.getBoolean("notification_permission_requested", false)
        val previouslyShownTimingExplanation = startupPreferences.getBoolean("precise_timing_explanation_shown", false)
        val permissionWasGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        try {
            clearNotificationPermissionFlags()
            startupPreferences.edit()
                .putBoolean("notification_permission_requested", false)
                .putBoolean("precise_timing_explanation_shown", false)
                .commit()
            assertFalse(
                "The isolated verification app should start without exact-alarm access",
                alarmManager.canScheduleExactAlarms(),
            )

            val settingsScenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                if (!permissionWasGranted) {
                    waitForSystemSettingsText("Allow")
                    clickSystemSettingsText("Allow")
                }
                waitForSystemSettingsText("Open settings")
                waitForSystemSettingsText("Not now")
                settingsScenario.recreate()
                waitForSystemSettingsText("Not now")
                clickSystemSettingsText("Not now")
            } finally {
                settingsScenario.close()
            }

            val reopenedScenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                assertSystemTextNotVisible("Open settings")
                assertSystemTextNotVisible("Not now")
                assertTrue(
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED,
                )
                assertTrue(startupPreferences.getBoolean("notification_permission_requested", false))
                assertTrue(startupPreferences.getBoolean("precise_timing_explanation_shown", false))
            } finally {
                reopenedScenario.close()
            }

            startupPreferences.edit().putBoolean("precise_timing_explanation_shown", false).commit()
            val settingsReturnScenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                waitForSystemSettingsText("Open settings")
                clickSystemSettingsText("Open settings")
                waitForSystemSettingsText("Allow setting alarms and reminders")
                pressSystemBack()

                val permissions = GlobalContext.get().get<RestAlertPermissions>()
                val refreshTimeout = SystemClock.uptimeMillis() + 5_000
                while (
                    permissions.status.value.preciseTimingAllowed != false &&
                    SystemClock.uptimeMillis() < refreshTimeout
                ) {
                    SystemClock.sleep(50)
                }
                assertEquals(false, permissions.status.value.preciseTimingAllowed)
                assertSystemTextNotVisible("Open settings")
            } finally {
                settingsReturnScenario.close()
            }
        } finally {
            clearNotificationPermissionFlags()
            startupPreferences.edit().apply {
                if (hadRequestedPermission) putBoolean("notification_permission_requested", previouslyRequestedPermission)
                else remove("notification_permission_requested")
                if (hadShownTimingExplanation) putBoolean("precise_timing_explanation_shown", previouslyShownTimingExplanation)
                else remove("precise_timing_explanation_shown")
            }.commit()
        }
    }

    @Test
    fun receiverFinishesAfterHandlerLookupFailure() {
        val dispatcher = QueuedDispatcher()
        val scope = CoroutineScope(Job() + dispatcher)
        val failure = IllegalStateException("handler lookup failed")
        var loggedFailure: Throwable? = null
        var finishCount = 0

        launchAndroidRestAlertReceiverWork(
            scope = scope,
            work = { throw failure },
            onFailure = { loggedFailure = it },
            onFinished = { finishCount++ },
            dispatcher = dispatcher,
        )
        dispatcher.runPending()

        assertSame(failure, loggedFailure)
        assertEquals(1, finishCount)
    }

    @Test
    fun receiverFinishesWhenApplicationScopeIsCancelledBeforeWorkStarts() {
        val dispatcher = QueuedDispatcher()
        val owner = Job()
        val scope = CoroutineScope(owner + dispatcher)
        var workStarted = false
        var finishCount = 0

        launchAndroidRestAlertReceiverWork(
            scope = scope,
            work = { workStarted = true },
            onFailure = { throw AssertionError("Cancellation is not an ordinary failure", it) },
            onFinished = { finishCount++ },
            dispatcher = dispatcher,
        )
        owner.cancel()
        dispatcher.runPending()

        assertFalse(workStarted)
        assertEquals(1, finishCount)
    }

    @Test
    fun androidWarningOutputRetainsTagMessageAndThrowableWithPercentCharacters() {
        val marker = UUID.randomUUID().toString()
        val tag = "RestAlertLogTest"
        val message = "warning $marker contains 100% %@ %s"
        val failure = IllegalStateException("stack $marker contains 50%")

        Logger.warn(tag, message, failure)

        val fileDescriptor = instrumentation.uiAutomation.executeShellCommand("logcat -d -s $tag:W")
        val logcat = ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor).bufferedReader().use { it.readText() }
        assertTrue(logcat, logcat.contains(message))
        assertTrue(logcat, logcat.contains("java.lang.IllegalStateException"))
        assertTrue(logcat, logcat.contains(failure.message.orEmpty()))
    }

    private fun activeRestNotification() =
        notificationManager.activeNotifications.firstOrNull { it.id == REST_NOTIFICATION_ID }

    private fun awaitActivityLosesWindowFocus(scenario: ActivityScenario<MainActivity>) {
        val timeoutAt = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < timeoutAt) {
            var hasWindowFocus = true
            scenario.onActivity { hasWindowFocus = it.hasWindowFocus() }
            if (!hasWindowFocus) return
            SystemClock.sleep(100)
        }
        throw AssertionError("The app Activity still has window focus after moving Home")
    }

    private fun deliverRestExpiryAndAssertNotification(
        workouts: WorkoutRepository,
        scheduler: PersistedRestAlertScheduler,
        workoutId: Long,
        setPosition: Int,
    ) {
        notificationManager.cancel(REST_NOTIFICATION_ID)
        assertTrue("A previous rest notification remained active", awaitNoRestNotification())

        val resting = requireNotNull(
            runBlocking { workouts.completeSetAndStartRest(workoutId, 0, setPosition, 1_500) },
        )
        val id = RestAlertId(resting.id, checkNotNull(resting.restTimer?.deadlineEpochMillis))
        val request = RestAlertRequest(id = id, backgroundEnabled = true, soundEnabled = false)
        runBlocking { scheduler.schedule(request) }
        AndroidRestAlertAlarm.cancel(context, AndroidRestAlertReceiver::class.java, id)
        runBlocking { delay((id.deadlineEpochMillis - System.currentTimeMillis() + 50).coerceAtLeast(0L)) }

        val completed = CountDownLatch(1)
        val finalReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                completed.countDown()
            }
        }
        context.sendOrderedBroadcast(
            AndroidRestAlertIntent.create(context, AndroidRestAlertReceiver::class.java, id),
            null,
            finalReceiver,
            Handler(Looper.getMainLooper()),
            Activity.RESULT_OK,
            null,
            null,
        )

        assertTrue("Receiver did not finish for expired rest $id", completed.await(10, TimeUnit.SECONDS))
        assertEquals(RestAlertState.Status.HANDLED, runBlocking { AndroidRestAlertStateStore(context).read() }?.status)
        val notification = checkNotNull(awaitRestNotification())
        assertEquals(AndroidRestAlertChannels.SILENT, notification.notification.channelId)
        assertEquals("Set complete", notification.notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Ready for your next set.", notification.notification.extras.getString(Notification.EXTRA_TEXT))

        notificationManager.cancel(REST_NOTIFICATION_ID)
        assertTrue("The rest notification remained active after cancellation", awaitNoRestNotification())
    }

    private fun prepareColdRestartChannelChoices() = withNotificationPermission {
        saveColdStartState()
        resetRestAlertChannels()
        setChannelAlertingInSystemSettings(AndroidRestAlertChannels.AUDIBLE, expectedAlerting = false)
        setChannelAlertingInSystemSettings(AndroidRestAlertChannels.SILENT, expectedAlerting = true)
        assertFalse(isChannelAlerting(AndroidRestAlertChannels.AUDIBLE))
        assertTrue(isChannelAlerting(AndroidRestAlertChannels.SILENT))

        hostVerificationPreferences().edit().putInt(KEY_CHANNEL_PROCESS_ID, android.os.Process.myPid()).commit()
    }

    private fun verifyColdRestartChannelChoices() {
        val previousPid = hostVerificationPreferences().getInt(KEY_CHANNEL_PROCESS_ID, -1)
        assertTrue("The host test must prepare the channel choices first", previousPid > 0)
        assertNotEquals("The app process was not restarted", previousPid, android.os.Process.myPid())

        assertFalse(isChannelAlerting(AndroidRestAlertChannels.AUDIBLE))
        assertTrue(isChannelAlerting(AndroidRestAlertChannels.SILENT))
        AndroidRestAlertChannels.create(context)
        assertFalse(isChannelAlerting(AndroidRestAlertChannels.AUDIBLE))
        assertTrue(isChannelAlerting(AndroidRestAlertChannels.SILENT))
    }

    private fun prepareColdNotificationTap() {
        saveColdStartState()
        grantNotificationPermissionForColdStart()
        prepareColdStartWorkout()
        notificationManager.cancel(REST_NOTIFICATION_ID)
        AndroidRestAlertNotification.show(context, soundEnabled = false)
        assertNotNull("The production notification did not post", awaitRestNotification())
    }

    private fun prepareColdReceiverDelivery() {
        saveColdStartState()
        grantNotificationPermissionForColdStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setExactAlarmAccess(allowed = true)

        val workouts = GlobalContext.get().get<WorkoutRepository>()
        val started = prepareColdStartWorkout(workouts)
        val resting = requireNotNull(runBlocking { workouts.completeSetAndStartRest(started.id, 0, 0, 20_000) })
        val id = RestAlertId(resting.id, checkNotNull(resting.restTimer?.deadlineEpochMillis))
        val permission = GlobalContext.get().get<RestAlertPermissions>()
        runBlocking { permission.refreshStatus() }
        assertEquals(
            "Notification permission must be granted for the cold delivery test",
            RestNotificationPermission.GRANTED,
            permission.status.value.notificationPermission,
        )
        runBlocking {
            GlobalContext.get().get<PersistedRestAlertScheduler>().schedule(
                RestAlertRequest(id = id, backgroundEnabled = true, soundEnabled = false),
            )
        }
        hostVerificationPreferences().edit().putInt(KEY_RECEIVER_PROCESS_ID, android.os.Process.myPid()).commit()
    }

    private fun verifyColdReceiverDelivery() {
        val previousPid = hostVerificationPreferences().getInt(KEY_RECEIVER_PROCESS_ID, -1)
        assertTrue("The host test must schedule a cold receiver delivery first", previousPid > 0)
        assertNotEquals("The receiver did not start a fresh app process", previousPid, android.os.Process.myPid())

        val state = checkNotNull(runBlocking { AndroidRestAlertStateStore(context).read() })
        assertEquals(RestAlertState.Status.HANDLED, state.status)
        val deliveryDelay = System.currentTimeMillis() - state.id.deadlineEpochMillis
        assertTrue("Cold receiver delivery was more than 10 seconds late: ${deliveryDelay}ms", deliveryDelay in 0..10_000)

        val notification = checkNotNull(awaitRestNotification())
        assertEquals("Set complete", notification.notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Ready for your next set.", notification.notification.extras.getString(Notification.EXTRA_TEXT))
    }

    private fun prepareColdStartWorkout(
        workouts: WorkoutRepository = GlobalContext.get().get<WorkoutRepository>(),
    ): UnfinishedWorkout {
        val existing = runBlocking { workouts.getUnfinishedWorkout() }
        if (existing != null) runBlocking { workouts.finalizeWorkout(existing.id) }
        val started = runBlocking { workouts.startWorkout(Workout.A) }
        hostVerificationPreferences().edit().putLong(KEY_WORKOUT_ID, started.id).commit()

        val preferences = GlobalContext.get().get<TrainingPreferencesRepository>()
        runBlocking {
            preferences.setRestNotificationsEnabled(true)
            preferences.setRestSoundEnabled(false)
        }
        return started
    }

    private fun grantNotificationPermissionForColdStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    private fun saveColdStartState() {
        val host = hostVerificationPreferences()
        if (host.contains(KEY_STATE_SAVED)) return

        val preferences = runBlocking { GlobalContext.get().get<TrainingPreferencesRepository>().getPreferences() }
        val startup = context.getSharedPreferences("startup_permissions", Context.MODE_PRIVATE)
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        host.edit()
            .putBoolean(KEY_STATE_SAVED, true)
            .putBoolean(KEY_REST_NOTIFICATIONS, preferences.restNotificationsEnabled)
            .putBoolean(KEY_REST_SOUND, preferences.restSoundEnabled)
            .putBoolean(KEY_PERMISSION_GRANTED, permissionGranted)
            .putBoolean(KEY_HAD_STARTUP_REQUESTED, startup.contains("notification_permission_requested"))
            .putBoolean(KEY_STARTUP_REQUESTED, startup.getBoolean("notification_permission_requested", false))
            .putBoolean(KEY_HAD_TIMING_EXPLANATION, startup.contains("precise_timing_explanation_shown"))
            .putBoolean(KEY_TIMING_EXPLANATION, startup.getBoolean("precise_timing_explanation_shown", false))
            .commit()
        startup.edit()
            .putBoolean("notification_permission_requested", true)
            .putBoolean("precise_timing_explanation_shown", true)
            .commit()
    }

    private fun cleanupColdStartVerification() {
        val host = hostVerificationPreferences()
        val workoutId = host.getLong(KEY_WORKOUT_ID, -1)
        if (workoutId > 0) {
            runBlocking { GlobalContext.get().get<WorkoutRepository>().finalizeWorkout(workoutId) }
        }
        runBlocking { GlobalContext.get().get<PersistedRestAlertScheduler>().cancel() }
        notificationManager.cancel(REST_NOTIFICATION_ID)

        if (host.getBoolean(KEY_STATE_SAVED, false)) {
            val preferences = GlobalContext.get().get<TrainingPreferencesRepository>()
            runBlocking {
                preferences.setRestNotificationsEnabled(host.getBoolean(KEY_REST_NOTIFICATIONS, true))
                preferences.setRestSoundEnabled(host.getBoolean(KEY_REST_SOUND, true))
            }
            val startup = context.getSharedPreferences("startup_permissions", Context.MODE_PRIVATE)
            startup.edit().apply {
                if (host.getBoolean(KEY_HAD_STARTUP_REQUESTED, false)) {
                    putBoolean("notification_permission_requested", host.getBoolean(KEY_STARTUP_REQUESTED, false))
                } else {
                    remove("notification_permission_requested")
                }
                if (host.getBoolean(KEY_HAD_TIMING_EXPLANATION, false)) {
                    putBoolean("precise_timing_explanation_shown", host.getBoolean(KEY_TIMING_EXPLANATION, false))
                } else {
                    remove("precise_timing_explanation_shown")
                }
            }.commit()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !host.getBoolean(KEY_PERMISSION_GRANTED, false)
            ) {
                instrumentation.uiAutomation.revokeRuntimePermission(
                    context.packageName,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
        }

        resetRestAlertChannels()
        host.edit().clear().commit()
    }

    private fun hostVerificationPreferences() =
        context.getSharedPreferences(HOST_VERIFICATION_PREFERENCES, Context.MODE_PRIVATE)

    private fun awaitRestNotification(): android.service.notification.StatusBarNotification? {
        val deadline = SystemClock.uptimeMillis() + 3_000
        while (SystemClock.uptimeMillis() < deadline) {
            activeRestNotification()?.let { return it }
            SystemClock.sleep(50)
        }
        return activeRestNotification()
    }

    private fun awaitNoRestNotification(): Boolean {
        val deadline = SystemClock.uptimeMillis() + 3_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (activeRestNotification() == null) return true
            SystemClock.sleep(50)
        }
        return activeRestNotification() == null
    }

    private fun normalLauncherPendingIntent(): PendingIntent {
        val launcher = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        return checkNotNull(
            PendingIntent.getActivity(
                context,
                0,
                launcher,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    private fun waitForSystemSettingsText(expectedText: String): AccessibilityNodeInfo {
        val timeoutAt = SystemClock.uptimeMillis() + 10_000
        var latestTree = ""
        while (SystemClock.uptimeMillis() < timeoutAt) {
            for (root in accessibilityRoots()) {
                val match = findAccessibilityNodesByText(root, expectedText).firstOrNull()
                if (match != null) return match
                latestTree = describeAccessibilityTree(root)
            }
            SystemClock.sleep(100)
        }
        throw AssertionError("System Settings did not show '$expectedText'. Current tree: $latestTree")
    }

    private fun waitForVisibleText(expectedText: String): AccessibilityNodeInfo =
        waitForSystemSettingsText(expectedText)

    private fun clickVisibleText(expectedText: String) = clickSystemSettingsText(expectedText)

    private fun waitForHeadingText(expectedText: String): AccessibilityNodeInfo {
        val timeoutAt = SystemClock.uptimeMillis() + 10_000
        var latestTree = ""
        while (SystemClock.uptimeMillis() < timeoutAt) {
            for (root in accessibilityRoots()) {
                val match = findAccessibilityNodesByText(root, expectedText).firstOrNull { it.isHeading }
                if (match != null) return match
                latestTree = describeAccessibilityTree(root)
            }
            SystemClock.sleep(100)
        }
        throw AssertionError("The app did not show heading '$expectedText'. Current tree: $latestTree")
    }

    private fun assertSystemTextNotVisible(expectedText: String) {
        val timeoutAt = SystemClock.uptimeMillis() + 1_000
        while (SystemClock.uptimeMillis() < timeoutAt) {
            assertFalse(
                "Unexpectedly found '$expectedText' in the current accessibility windows",
                accessibilityRoots().any { root ->
                    findAccessibilityNodesByText(root, expectedText).isNotEmpty()
                },
            )
            SystemClock.sleep(100)
        }
    }

    private fun setChannelAlertingInSystemSettings(channelId: String, expectedAlerting: Boolean) {
        if (isChannelAlerting(channelId) == expectedAlerting) return
        val channelName = notificationManager.getNotificationChannel(channelId).name.toString()
        openChannelSettings(channelId)
        waitForSystemSettingsText(channelName)
        clickSystemSettingsText(if (expectedAlerting) "Default" else "Silent")
        waitForChannelAlerting(channelId, expectedAlerting)
        pressSystemBack()
    }

    private fun resetRestAlertChannels() {
        notificationManager.deleteNotificationChannel(AndroidRestAlertChannels.AUDIBLE)
        notificationManager.deleteNotificationChannel(AndroidRestAlertChannels.SILENT)
        AndroidRestAlertChannels.create(context)
    }

    private fun openChannelSettings(channelId: String) {
        context.startActivity(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
    }

    private fun clickSystemSettingsText(expectedText: String) {
        val timeoutAt = SystemClock.uptimeMillis() + 10_000
        var latestTree = ""
        while (SystemClock.uptimeMillis() < timeoutAt) {
            for (root in accessibilityRoots()) {
                val matches = findAccessibilityNodesByText(root, expectedText)
                val exactMatches = matches.filter { node ->
                    node.text?.toString()?.equals(expectedText, ignoreCase = true) == true ||
                        node.contentDescription?.toString()?.equals(expectedText, ignoreCase = true) == true
                }
                for (match in exactMatches + matches.filterNot(exactMatches::contains)) {
                    var node: AccessibilityNodeInfo? = match
                    while (node != null && !node.isClickable) node = node.parent
                    if (node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return
                }
                latestTree = describeAccessibilityTree(root)
            }
            SystemClock.sleep(100)
        }
        throw AssertionError("No clickable Settings node found for '$expectedText'. Current tree: $latestTree")
    }

    private fun findAccessibilityNodesByText(
        node: AccessibilityNodeInfo,
        expectedText: String,
    ): List<AccessibilityNodeInfo> = buildList {
        val matchesText = node.text?.toString()?.contains(expectedText, ignoreCase = true) == true
        val matchesDescription = node.contentDescription?.toString()?.contains(expectedText, ignoreCase = true) == true
        if (matchesText || matchesDescription) add(node)
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { addAll(findAccessibilityNodesByText(it, expectedText)) }
        }
    }

    private fun accessibilityRoots(): List<AccessibilityNodeInfo> = instrumentation.uiAutomation.windows
        .mapNotNull { it.root }
        .ifEmpty { listOfNotNull(instrumentation.uiAutomation.rootInActiveWindow) }

    private fun isChannelAlerting(channelId: String): Boolean = notificationManager
        .getNotificationChannel(channelId)
        .importance > NotificationManager.IMPORTANCE_LOW

    private fun waitForChannelAlerting(
        channelId: String,
        expectedAlerting: Boolean,
    ): android.app.NotificationChannel {
        val timeoutAt = SystemClock.uptimeMillis() + 5_000
        var latestChannel: android.app.NotificationChannel? = null
        while (SystemClock.uptimeMillis() < timeoutAt) {
            val channel = notificationManager.getNotificationChannel(channelId)
            latestChannel = channel
            val isAlerting = channel.importance > NotificationManager.IMPORTANCE_LOW
            if (isAlerting == expectedAlerting) return channel
            SystemClock.sleep(100)
        }
        throw AssertionError(
            "Expected channel=$channelId alerting=$expectedAlerting, found " +
                "importance=${latestChannel?.importance}, sound=${latestChannel?.sound}",
        )
    }

    private fun describeAccessibilityTree(node: AccessibilityNodeInfo): String = buildString {
        node.className?.let { append(it).append(' ') }
        node.viewIdResourceName?.let { append(it).append('=') }
        node.text?.let { append(it).append(' ') }
        node.contentDescription?.let { append(it).append(' ') }
        if (node.isClickable) append("clickable ")
        if (node.isCheckable) append("checkable checked=").append(node.isChecked).append(' ')
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { append(describeAccessibilityTree(it)) }
        }
    }

    private fun pressSystemBack() {
        val automation = instrumentation.uiAutomation
        automation.injectInputEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK), true)
        automation.injectInputEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK), true)
    }

    private fun alarmPendingIntent(id: RestAlertId): PendingIntent? = PendingIntent.getBroadcast(
        context,
        0,
        AndroidRestAlertIntent.create(context, AndroidRestAlertReceiver::class.java, id),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun setExactAlarmAccess(allowed: Boolean) {
        val mode = if (allowed) "allow" else "ignore"
        val fileDescriptor = instrumentation.uiAutomation.executeShellCommand(
            "cmd appops set ${context.packageName} SCHEDULE_EXACT_ALARM $mode",
        )
        ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor).bufferedReader().use { it.readText() }
    }

    private fun clearNotificationPermissionFlags() {
        val fileDescriptor = instrumentation.uiAutomation.executeShellCommand(
            "pm clear-permission-flags ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS} user-set user-fixed",
        )
        ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor).bufferedReader().use { it.readText() }
    }

    private inline fun withNotificationPermission(block: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            block()
            return
        }
        val wasGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!wasGranted) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        try {
            block()
        } finally {
            notificationManager.cancel(REST_NOTIFICATION_ID)
        }
    }

    private companion object {
        const val REST_NOTIFICATION_ID = 1
        const val OTHER_NOTIFICATION_ID = 101
        const val HOST_VERIFICATION_PREFERENCES = "rest_alert_host_verification"
        const val KEY_CHANNEL_PROCESS_ID = "channel_process_id"
        const val KEY_RECEIVER_PROCESS_ID = "receiver_process_id"
        const val KEY_WORKOUT_ID = "workout_id"
        const val KEY_STATE_SAVED = "state_saved"
        const val KEY_REST_NOTIFICATIONS = "rest_notifications_enabled"
        const val KEY_REST_SOUND = "rest_sound_enabled"
        const val KEY_PERMISSION_GRANTED = "notification_permission_granted"
        const val KEY_HAD_STARTUP_REQUESTED = "had_notification_permission_requested"
        const val KEY_STARTUP_REQUESTED = "notification_permission_requested"
        const val KEY_HAD_TIMING_EXPLANATION = "had_timing_explanation_shown"
        const val KEY_TIMING_EXPLANATION = "timing_explanation_shown"
    }
}

private class QueuedDispatcher : CoroutineDispatcher() {
    private val pending = ArrayDeque<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        pending.addLast(block)
    }

    fun runPending() {
        while (pending.isNotEmpty()) pending.removeFirst().run()
    }
}
