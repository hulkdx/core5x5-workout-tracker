package com.hulkdx.core5x5.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

internal class AppBackStackTest {
    @Test
    fun completionRestoresTheIdentifiedSessionAndReturnsDirectlyToToday() {
        val id = Int.MAX_VALUE.toLong() + 42L
        val original = mutableStateListOf<AppDestination>(
            AppDestination.Today,
            AppDestination.WorkoutComplete(id),
        )
        val serialized = with(AppBackStackSaver) { SaverScope { it is String }.save(original) }
        val restored = requireNotNull(AppBackStackSaver.restore(requireNotNull(serialized)))

        assertNotSame(original, restored)
        assertEquals(original.toList(), restored.toList())
        assertEquals(AppDestination.WorkoutComplete(id), restored.removeLastOrNull())
        assertEquals(listOf(AppDestination.Today), restored.toList())
    }

    @Test
    fun activeDestinationRestoresTheOriginalIdNeededToRecoverACommittedSave() {
        val original = mutableStateListOf<AppDestination>(
            AppDestination.Today,
            AppDestination.ActiveWorkout(42L),
        )
        val serialized = with(AppBackStackSaver) { SaverScope { it is String }.save(original) }
        val restored = requireNotNull(AppBackStackSaver.restore(requireNotNull(serialized)))

        assertEquals(AppDestination.ActiveWorkout(42L), restored.last())
        restored[restored.lastIndex] = AppDestination.WorkoutComplete(42L)
        assertTrue(restored.none { it is AppDestination.ActiveWorkout })
        restored.removeLastOrNull()
        assertEquals(AppDestination.Today, restored.single())
    }
}
