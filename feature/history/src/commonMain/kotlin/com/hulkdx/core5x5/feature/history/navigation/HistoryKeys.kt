package com.hulkdx.core5x5.feature.history.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("history")
data object HistoryKey : NavKey

@Serializable
@SerialName("detail")
data class WorkoutDetailKey(val workoutId: Long) : NavKey {
    init { require(workoutId > 0) { "A workout ID must be positive" } }
}

val historyNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(HistoryKey::class, HistoryKey.serializer())
        subclass(WorkoutDetailKey::class, WorkoutDetailKey.serializer())
    }
}
