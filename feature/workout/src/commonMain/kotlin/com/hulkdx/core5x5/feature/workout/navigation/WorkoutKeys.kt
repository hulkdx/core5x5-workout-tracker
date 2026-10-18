package com.hulkdx.core5x5.feature.workout.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("today")
data object TodayKey : NavKey

@Serializable
@SerialName("active")
data class ActiveWorkoutKey(val workoutId: Long) : NavKey {
    init { require(workoutId > 0) { "A workout ID must be positive" } }
}

@Serializable
@SerialName("complete")
data class WorkoutCompleteKey(val workoutId: Long) : NavKey {
    init { require(workoutId > 0) { "A workout ID must be positive" } }
}

val workoutNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(TodayKey::class, TodayKey.serializer())
        subclass(ActiveWorkoutKey::class, ActiveWorkoutKey.serializer())
        subclass(WorkoutCompleteKey::class, WorkoutCompleteKey.serializer())
    }
}
