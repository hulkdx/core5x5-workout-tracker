@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import com.hulkdx.core5x5.app.AppRoot
import com.hulkdx.core5x5.core.preferences.data.PersistedTrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.data.PreferencesStorage
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.di.appModule
import com.hulkdx.core5x5.feature.settings.domain.AppMetadata
import com.hulkdx.core5x5.feature.workout.data.RoomWorkoutRepository
import com.hulkdx.core5x5.feature.workout.data.WorkoutDatabase
import com.hulkdx.core5x5.feature.workout.domain.RestTimerRules
import kotlinx.coroutines.runBlocking
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

fun runAppUiTest(width: Dp? = null, test: AppUiTest.() -> Unit) = runComposeUiTest {
    val app = AppUiTest(this, width)
    try {
        app.launch()
        app.test()
    } finally {
        app.close()
    }
}

class AppUiTest(val ui: ComposeUiTest, private val width: Dp? = null) {
    private val storage = createTestDatabase()
    private val owner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
    private var now = 1_800_000_000_000L
    private var showContent by mutableStateOf(true)
    private var repository = RoomWorkoutRepository(storage.database.unfinishedWorkoutDao()) { now }
    private var application: KoinApplication? = null

    private var rejectPreferenceWrites = false
    var failNextUriOpen = false
    val openedUris = mutableListOf<String>()
    private val preferences = object : PreferencesStorage by storage.preferencesStorage {
        override suspend fun write(value: String) {
            check(!rejectPreferenceWrites) { "Injected preference write failure" }
            storage.preferencesStorage.write(value)
        }
    }
    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            if (failNextUriOpen) {
                failNextUriOpen = false
                error("Injected URI failure")
            }
            openedUris += uri
        }
    }

    private fun createApplication(): KoinApplication {
        val overrides = module {
            single<WorkoutDatabase> { storage.database }
            single { repository }
            single<TrainingPreferencesRepository> { PersistedTrainingPreferencesRepository(preferences) }
            single<AppMetadata> { AppMetadata { "UI test" } }
            factory { RestTimerRules { now } }
        }
        return koinApplication { modules(appModule, overrides) }
    }

    fun launch() {
        application = createApplication()
        ui.setContent {
            if (showContent) {
                KoinIsolatedContext(context = requireNotNull(application)) {
                    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, LocalUriHandler provides uriHandler) {
                        AppTheme {
                            Box(if (width == null) Modifier.fillMaxSize() else Modifier.width(width).fillMaxHeight()) {
                                AppRoot()
                            }
                        }
                    }
                }
            }
        }
        awaitReady()
    }

    private fun awaitReady() =
        awaitNode((hasTestTag("start-workout") or hasTestTag("resume-workout")) and isEnabled())

    fun restart() {
        ui.runOnIdle { showContent = false }
        ui.waitForIdle()
        owner.viewModelStore.clear()
        application?.close()
        storage.reopen()
        repository = RoomWorkoutRepository(storage.database.unfinishedWorkoutDao()) { now }
        application = createApplication()
        ui.runOnIdle { showContent = true }
        awaitReady()
    }

    fun blockSetWrites() = installFailure("UPDATE", "unfinished_workout_set")
    fun blockFinishing() = installFailure("UPDATE OF completedAtEpochMillis", "unfinished_workout")
    fun blockEditing() = installFailure("UPDATE", "unfinished_workout_exercise")
    fun blockSwitching() = installFailure("INSERT", "unfinished_workout_set")
    private fun installFailure(operation: String, table: String) = runBlocking {
        storage.database.useWriterConnection {
            it.executeSQL("CREATE TRIGGER ui_test_failure BEFORE $operation ON $table BEGIN SELECT RAISE(ABORT, 'UI test failure'); END")
        }
    }
    fun allowWorkoutWrites() = runBlocking {
        storage.database.useWriterConnection { it.executeSQL("DROP TRIGGER ui_test_failure") }
    }
    fun blockPreferenceWrites() { rejectPreferenceWrites = true }
    fun allowPreferenceWrites() { rejectPreferenceWrites = false }

    fun awaitNode(matcher: SemanticsMatcher) = ui.onNode(matcher).also {
        ui.waitUntil(timeoutMillis = 5_000) { ui.onAllNodes(matcher).fetchSemanticsNodes().size == 1 }
    }

    fun awaitAbsent(tag: String) {
        ui.waitUntil(timeoutMillis = 5_000) { ui.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty() }
    }

    // Error rows are inserted asynchronously and may not yet be composed by a lazy list.
    fun scrollToNode(listTag: String, matcher: SemanticsMatcher) = ui.onNode(matcher).also {
        ui.waitUntil(timeoutMillis = 5_000) {
            try {
                ui.onNodeWithTag(listTag).performScrollToNode(matcher)
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    val nowEpochMillis: Long get() = now
    fun advanceTimeBy(millis: Long) = ui.runOnIdle { now += millis }

    // Storage reads supplement UI assertions; actions always go through Compose controls.
    fun savedWorkout() = runBlocking {
        repository.getCompletedWorkouts().singleOrNull()?.let { repository.getCompletedWorkout(it.id) }
    }

    fun unfinishedWorkout() = runBlocking { repository.getUnfinishedWorkout() }

    fun close() {
        try {
            ui.runOnIdle { showContent = false }
            ui.waitForIdle()
        } finally {
            try {
                owner.viewModelStore.clear()
            } finally {
                try {
                    application?.close()
                } finally {
                    storage.close()
                }
            }
        }
    }
}
