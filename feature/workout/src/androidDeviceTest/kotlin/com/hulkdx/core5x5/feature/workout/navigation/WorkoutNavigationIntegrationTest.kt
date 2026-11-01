package com.hulkdx.core5x5.feature.workout.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.room3.Room
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hulkdx.core5x5.core.navigation.CoreNavigator
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.navigation.rememberCoreNavigator
import com.hulkdx.core5x5.core.navigation.rememberNavigationState
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.feature.workout.data.RoomWorkoutRepository
import com.hulkdx.core5x5.feature.workout.data.WorkoutDatabase
import com.hulkdx.core5x5.feature.workout.data.buildWorkoutDatabase
import com.hulkdx.core5x5.feature.workout.di.workoutModule
import com.hulkdx.core5x5.feature.workout.domain.Workout
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
class WorkoutNavigationIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "navigation-${UUID.randomUUID()}.db"
    private lateinit var database: WorkoutDatabase
    private lateinit var repository: RoomWorkoutRepository
    private lateinit var navigator: CoreNavigator
    private var workoutId = 0L
    private var showContent by mutableStateOf(true)

    @Before
    fun setUp() {
        database = Room.databaseBuilder<WorkoutDatabase>(context, databaseName).buildWorkoutDatabase()
        repository = RoomWorkoutRepository(database.unfinishedWorkoutDao())
        workoutId = runBlocking { repository.startWorkout(Workout.A).id }
    }

    @After
    fun tearDown() {
        compose.runOnIdle { showContent = false }
        compose.waitForIdle()
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun systemBackDismissesTheEditorBeforePoppingTheIdentifiedActiveWorkout() {
        compose.setContent { if (showContent) TestHost() }
        compose.runOnIdle { navigator.navigate(ActiveWorkoutKey(workoutId)) }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Edit").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Edit").onFirst().performClick()
        compose.onNodeWithText("History").assertDoesNotExist()
        compose.onNodeWithText("Settings").assertDoesNotExist()
        compose.onNodeWithContentDescription("Cancel exercise editing").assertExists()

        pressBack()

        compose.onNodeWithContentDescription("Cancel exercise editing").assertDoesNotExist()
        compose.runOnIdle { assertEquals(ActiveWorkoutKey(workoutId), navigator.state.currentKey) }

        pressBack()

        compose.runOnIdle { assertEquals(TodayKey, navigator.state.currentKey) }
        compose.onNodeWithText("History").assertExists()
        compose.onNodeWithText("Settings").assertExists()
        assertEquals(workoutId, runBlocking { repository.getUnfinishedWorkout()?.id })
    }

    @Composable
    private fun TestHost() {
        KoinApplication(configuration = koinConfiguration {
            modules(workoutModule, module {
                single<WorkoutDatabase> { database }
                single<TrainingPreferencesRepository> { TestPreferences() }
            })
        }) {
            AppTheme {
                navigator = rememberCoreNavigator(rememberNavigationState(
                    startKey = TodayKey,
                    tabDestinations = mapOf(TopLevelTab.TODAY to TodayKey),
                    configuration = SavedStateConfiguration { serializersModule = workoutNavigationSerializers },
                ))
                NavDisplay(
                    backStack = navigator.state.backStacks.getValue(TodayKey),
                    onBack = navigator::goBack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider { workoutEntries(navigator) },
                )
            }
        }
    }

    private class TestPreferences : TrainingPreferencesRepository {
        override val preferences = MutableStateFlow(TrainingPreferences())
        override suspend fun getPreferences() = preferences.value
        override suspend fun setWeightUnit(unit: WeightUnit) {
            preferences.value = preferences.value.copy(weightUnit = unit)
        }
        override suspend fun setRestDurationSeconds(seconds: Long) {
            preferences.value = preferences.value.copy(restDurationSeconds = seconds)
        }
        override suspend fun setRestNotificationsEnabled(enabled: Boolean) {
            preferences.value = preferences.value.copy(restNotificationsEnabled = enabled)
        }
        override suspend fun setRestSoundEnabled(enabled: Boolean) {
            preferences.value = preferences.value.copy(restSoundEnabled = enabled)
        }
    }
}
