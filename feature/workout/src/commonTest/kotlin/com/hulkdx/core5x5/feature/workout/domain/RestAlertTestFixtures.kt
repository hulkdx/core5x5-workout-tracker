package com.hulkdx.core5x5.feature.workout.domain

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertScheduler
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

fun restAlertWorkout(id: Long = 7, deadline: Long? = 2_000) = UnfinishedWorkout(
    workout = Workout.A, startedAtEpochMillis = 100, exercises = emptyList(), id = id,
    restTimer = deadline?.let(::RestTimer),
)

class RecordingRestAlertScheduler : RestAlertScheduler {
    val requests = mutableListOf<RestAlertRequest>()
    var cancellations = 0
    var failure: Throwable? = null

    override suspend fun schedule(request: RestAlertRequest) {
        failure?.let { throw it }
        requests += request
    }

    override suspend fun cancel() {
        failure?.let { throw it }
        cancellations++
    }
}

class RestAlertTestPermissions : RestAlertPermissions {
    override val status = MutableStateFlow(RestAlertPermissionStatus(RestNotificationPermission.GRANTED, true))
    var refreshes = 0
    var refreshFailure: Throwable? = null

    override suspend fun refreshStatus() {
        refreshes++
        refreshFailure?.let { throw it }
    }
}

/** Alert observation may read/subscribe, but must never write the workout. */
class RestAlertTestWorkouts(var saved: UnfinishedWorkout? = restAlertWorkout()) : WorkoutRepository {
    val changes = MutableSharedFlow<Unit>()
    var observationFailure: Throwable? = null
    var readFailure: Throwable? = null
    var readGate: CompletableDeferred<Unit>? = null
    var reads = 0
    var observations = 0
    var activeObservers = 0

    override fun observeWorkoutChanges() = flow {
        observations++
        activeObservers++
        try {
            observationFailure?.let { throw it }
            emit(Unit)
            emitAll(changes)
        } finally {
            activeObservers--
        }
    }

    override suspend fun getUnfinishedWorkout(): UnfinishedWorkout? {
        reads++
        readGate?.await()
        readFailure?.let { throw it }
        return saved
    }

    override suspend fun getCompletedWorkout(workoutId: Long): CompletedWorkout? = error("Unexpected completed read")
    override suspend fun getResumablePreviousWorkout(): CompletedWorkout? = error("Unexpected previous read")
    override suspend fun reopenWorkout(workoutId: Long, completedAtEpochMillis: Long): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun getNextWorkout(): Workout = error("Unexpected program read")
    override suspend fun getNextWorkoutPrescription(workoutOverride: Workout?): WorkoutPrescription = error("Unexpected prescription read")
    override suspend fun startWorkout(workout: Workout): UnfinishedWorkout = error("Unexpected write")
    override suspend fun switchWorkout(workoutId: Long, workout: Workout): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun finalizeWorkout(workoutId: Long): Boolean = error("Unexpected write")
    override suspend fun setSetCompleted(exercisePosition: Int, setPosition: Int, isCompleted: Boolean): Boolean = error("Unexpected write")
    override suspend fun editExercise(workoutId: Long, exercisePosition: Int, edit: ExerciseEdit): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun undoSetCompletion(workoutId: Long, exercisePosition: Int, setPosition: Int): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun setCompletedReps(workoutId: Long, exercisePosition: Int, setPosition: Int, completedReps: Int): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun completeSetAndStartRest(workoutId: Long, exercisePosition: Int, setPosition: Int, restDurationMillis: Long): UnfinishedWorkout? = error("Unexpected write")
}
