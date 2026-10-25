@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavKey
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import com.hulkdx.core5x5.app.AppRoot
import com.hulkdx.core5x5.core.preferences.data.PersistedTrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.data.PreferencesStorage
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSource
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.di.appModule
import com.hulkdx.core5x5.feature.settings.domain.AppMetadata
import com.hulkdx.core5x5.feature.workout.data.RoomWorkoutRepository
import com.hulkdx.core5x5.feature.workout.data.WorkoutDatabase
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.RestTimerRules
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.runBlocking
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

fun runAppUiTest(
    width: Dp? = null,
    height: Dp? = null,
    prepare: AppUiTest.() -> Unit = {},
    waitForReady: Boolean = true,
    test: AppUiTest.() -> Unit,
) = runComposeUiTest {
    val app = AppUiTest(this, width, height)
    try {
        app.prepare()
        app.launch(waitForReady)
        app.test()
    } finally {
        app.close()
    }
}

class AppUiTest(
    val ui: ComposeUiTest,
    private val width: Dp? = null,
    private val height: Dp? = null,
) {
    val requests = UiTestRequests()
    private val storage = createTestDatabase()
    private val owner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
    private var now = 1_800_000_000_000L
    private var showContent by mutableStateOf(true)
    private var repository = RoomWorkoutRepository(storage.database.unfinishedWorkoutDao()) { now }
    private var application: KoinApplication? = null
    private var restoredDestination: NavKey? = null

    private var rejectPreferenceWrites = false
    private var rejectPreferenceReads = false
    var failNextUriOpen = false
    val openedUris = mutableListOf<String>()
    private val preferences = object : PreferencesStorage by storage.preferencesStorage {
        override suspend fun read() = requests.execute(UiTestOperation.READ_PREFERENCES) {
            check(!rejectPreferenceReads) { "Injected preference read failure" }
            storage.preferencesStorage.read()
        }
        override suspend fun write(value: String) {
            check(!rejectPreferenceWrites) { "Injected preference write failure" }
            requests.execute(UiTestOperation.WRITE_PREFERENCES) { storage.preferencesStorage.write(value) }
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
        val controlled = UiTestWorkoutRepository(repository, requests)
        val overrides = module {
            single<WorkoutDatabase> { storage.database }
            single { repository }
            single<WorkoutRepository> { controlled }
            single<CompletedWorkoutSource> { controlled }
            single<TrainingPreferencesRepository> { PersistedTrainingPreferencesRepository(preferences) }
            single<AppMetadata> { AppMetadata { "UI test" } }
            factory { RestTimerRules { now } }
        }
        return koinApplication { modules(appModule, overrides) }
    }

    fun launch(waitForReady: Boolean = true) {
        application = createApplication()
        ui.setContent {
            if (showContent) {
                KoinIsolatedContext(context = requireNotNull(application)) {
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides owner,
                        LocalUriHandler provides uriHandler,
                    ) {
                        AppTheme {
                            var viewport = Modifier.fillMaxSize()
                            if (width != null || height != null) {
                                viewport = if (width == null) Modifier else Modifier.width(width)
                                viewport = if (height == null) viewport.fillMaxHeight() else viewport.height(height)
                            }
                            Box(viewport) {
                                val destination = restoredDestination
                                if (destination == null) AppRoot() else RestoredApp(destination)
                            }
                        }
                    }
                }
            }
        }
        if (waitForReady) awaitReady()
    }

    private fun awaitReady() =
        awaitNode((hasTestTag("start-workout") or hasTestTag("resume-workout")) and isEnabled())

    fun restart(destination: NavKey? = null, waitForReady: Boolean = true) {
        ui.runOnIdle { showContent = false }
        ui.waitForIdle()
        owner.viewModelStore.clear()
        application?.close()
        storage.reopen()
        repository = RoomWorkoutRepository(storage.database.unfinishedWorkoutDao()) { now }
        restoredDestination = destination
        application = createApplication()
        ui.runOnIdle { showContent = true }
        if (waitForReady) awaitReady()
    }

    fun pauseNext(operation: UiTestOperation): UiTestRequest = ui.runOnIdle { requests.pauseNext(operation) }
    fun failNext(operation: UiTestOperation, afterCommit: Boolean = false) =
        ui.runOnIdle { requests.failNext(operation, afterCommit) }
    fun allowRequests(operation: UiTestOperation) = ui.runOnIdle { requests.allow(operation) }
    fun awaitStarted(request: UiTestRequest) = ui.waitUntil(timeoutMillis = 5_000) { request.hasStarted }
    fun release(request: UiTestRequest) = ui.runOnIdle { request.release() }

    // Only legacy data unavailable through today's UI is seeded directly in the isolated database.
    fun seedLegacyName(exercise: Exercise, name: String) = runBlocking {
        val dao = storage.database.unfinishedWorkoutDao()
        val session = requireNotNull(dao.getUnfinishedWorkout())
        val saved = dao.getExercises(session.id).single { it.exercise == exercise }
        check(dao.updateExercise(saved.copy(customName = name)) == 1)
    }

    // Stored extreme values select the native numeric-input path, unreachable through wheel taps.
    fun seedLegacyRestDuration(exercise: Exercise, durationMillis: Long) = runBlocking {
        require(durationMillis > 0)
        val dao = storage.database.unfinishedWorkoutDao()
        val session = requireNotNull(dao.getUnfinishedWorkout())
        val saved = dao.getExercises(session.id).single { it.exercise == exercise }
        check(dao.updateExercise(saved.copy(restDurationMillis = durationMillis)) == 1)
    }

    fun blockSetWrites() = installFailure("UPDATE", "unfinished_workout_set")
    fun blockFinishing() = installFailure("UPDATE OF completedAtEpochMillis", "unfinished_workout")
    fun blockEditing() = installFailure("UPDATE", "unfinished_workout_exercise")
    fun blockSetRemoval() = installFailure("DELETE", "unfinished_workout_set")
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
    fun blockPreferenceReads() { rejectPreferenceReads = true }
    fun allowPreferenceReads() { rejectPreferenceReads = false }

    fun awaitNode(matcher: SemanticsMatcher) = ui.onNode(matcher).also {
        ui.waitUntil(timeoutMillis = 5_000) { ui.onAllNodes(matcher).fetchSemanticsNodes().size == 1 }
    }

    fun awaitAbsent(tag: String) {
        ui.waitUntil(timeoutMillis = 5_000) { ui.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty() }
    }

    // Error rows are inserted asynchronously and may not yet be composed by a lazy list.
    fun scrollToNode(
        listTag: String,
        matcher: SemanticsMatcher,
        itemMatcher: SemanticsMatcher = matcher,
    ) = ui.onNode(matcher).also {
        try {
            ui.waitUntil(timeoutMillis = 5_000) {
                try {
                    ui.onNodeWithTag(listTag).performScrollToNode(itemMatcher)
                    val target = ui.onAllNodes(matcher).fetchSemanticsNodes().singleOrNull()
                    if (target == null) {
                        false
                    } else {
                        // ScrollToNode positions the lazy item. A descendant can still be
                        // below the viewport when that item is taller than the viewport.
                        val list = ui.onNodeWithTag(listTag)
                        val viewport = list.fetchSemanticsNode().boundsInRoot
                        val top = target.positionInRoot.y
                        val bottom = top + target.size.height
                        val delta = when {
                            bottom > viewport.bottom -> bottom - viewport.bottom
                            top < viewport.top -> top - viewport.top
                            else -> 0f
                        }
                        if (delta != 0f) list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, delta) }
                        true
                    }
                } catch (_: AssertionError) {
                    false
                }
            }
        } catch (timeout: ComposeTimeoutException) {
            throw AssertionError("Unable to scroll $listTag to ${matcher.description}\n" +
                ui.onNodeWithTag(listTag).printToString(), timeout)
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
            ui.runOnIdle { requests.releaseAll() }
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
