package com.hulkdx.core5x5.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.feature.history.navigation.HistoryKey
import com.hulkdx.core5x5.feature.history.navigation.WorkoutDetailKey
import com.hulkdx.core5x5.feature.settings.navigation.SettingsKey
import com.hulkdx.core5x5.feature.workout.navigation.ActiveWorkoutKey
import com.hulkdx.core5x5.feature.workout.navigation.TodayKey
import com.hulkdx.core5x5.feature.workout.navigation.WorkoutCompleteKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class FeatureNavigationTest {
    private val roots = mapOf<TopLevelTab, NavKey>(
        TopLevelTab.TODAY to TodayKey,
        TopLevelTab.HISTORY to HistoryKey,
        TopLevelTab.SETTINGS to SettingsKey,
    )
    private val serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class))
    // Exercise the same KMP serializer used by rememberNavBackStack without Android Bundle stubs.
    private val json = Json { serializersModule = navigationSavedStateConfiguration.serializersModule }

    @Test
    fun allFeatureKeysRoundTripWithLargeLongIds() {
        val id = Long.MAX_VALUE
        listOf(
            NavBackStack<NavKey>(TodayKey, HistoryKey, SettingsKey),
            NavBackStack<NavKey>(TodayKey, ActiveWorkoutKey(id)),
            NavBackStack<NavKey>(TodayKey, WorkoutCompleteKey(id)),
            NavBackStack<NavKey>(HistoryKey, WorkoutDetailKey(id)),
        ).forEach { original ->
            val restored = roundTrip(original)
            assertNotSame(original, restored)
            assertEquals(original.toList(), restored.toList())
        }
    }

    @Test
    fun selectedTabUsesTheKmpMutableStateSerializerWithTheFeatureConfiguration() {
        val selectedTabSerializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
        roots.values.forEach { key ->
            val selectedTab = mutableStateOf<NavKey>(key)
            val saved = json.encodeToString(selectedTabSerializer, selectedTab)
            val restored = json.decodeFromString(selectedTabSerializer, saved)
            assertNotSame(selectedTab, restored)
            assertEquals(key, restored.value)
        }
    }

    @Test
    fun unregisteredKeysMissingArgumentsAndInvalidIdentifiersAreRejected() {
        assertFailsWith<SerializationException> {
            json.decodeFromString(serializer, """[{"type":"removed-feature"}]""")
        }
        assertFailsWith<SerializationException> {
            json.decodeFromString(serializer, """[{"type":"active"}]""")
        }
        assertFailsWith<IllegalArgumentException> { ActiveWorkoutKey(0) }
        assertFailsWith<IllegalArgumentException> { WorkoutCompleteKey(-1) }
        assertFailsWith<IllegalArgumentException> { WorkoutDetailKey(0) }
    }

    private fun roundTrip(stack: NavBackStack<NavKey>) =
        json.decodeFromString(serializer, json.encodeToString(serializer, stack))
}
