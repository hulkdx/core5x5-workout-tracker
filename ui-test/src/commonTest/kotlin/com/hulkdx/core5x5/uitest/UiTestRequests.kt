package com.hulkdx.core5x5.uitest

import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSource
import com.hulkdx.core5x5.feature.workout.data.RoomWorkoutRepository
import com.hulkdx.core5x5.feature.workout.domain.ExerciseEdit
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.CompletableDeferred

enum class UiTestOperation {
    LOAD_ACTIVE, LOAD_PRESCRIPTION, START, SWITCH, COMPLETE_SET, SAVE_REPS, RESET_SET,
    EDIT, FINISH, LOAD_COMPLETE, LOAD_DETAIL, READ_PREFERENCES, WRITE_PREFERENCES,
}

class UiTestRequest(
    paused: Boolean,
    private val fail: Boolean,
    private val afterCommit: Boolean,
) {
    private val started = CompletableDeferred<Unit>()
    private val released = CompletableDeferred<Unit>().apply { if (!paused) complete(Unit) }
    val hasStarted: Boolean get() = started.isCompleted

    fun release() { released.complete(Unit) }

    suspend fun <T> execute(block: suspend () -> T): T {
        started.complete(Unit)
        released.await()
        check(!fail || afterCommit) { "Injected request failure" }
        val result = block()
        check(!fail) { "Injected lost reply after commit" }
        return result
    }
}

/** One-shot faults suspend at the repository boundary; all successful work uses real storage. */
class UiTestRequests {
    private val next = mutableMapOf<UiTestOperation, UiTestRequest>()
    private val requests = mutableListOf<UiTestRequest>()
    private val blocked = mutableSetOf<UiTestOperation>()

    fun releaseAll() { requests.forEach { it.release() } }
    fun block(operation: UiTestOperation) { blocked += operation }
    fun allow(operation: UiTestOperation) { blocked -= operation }

    fun pauseNext(operation: UiTestOperation): UiTestRequest =
        enqueue(operation, paused = true)

    fun failNext(operation: UiTestOperation, afterCommit: Boolean = false) {
        enqueue(operation, fail = true, afterCommit = afterCommit)
    }

    private fun enqueue(
        operation: UiTestOperation,
        paused: Boolean = false,
        fail: Boolean = false,
        afterCommit: Boolean = false,
    ): UiTestRequest {
        check(operation !in next) { "A request is already queued for $operation" }
        return UiTestRequest(paused, fail, afterCommit).also {
            next[operation] = it
            requests += it
        }
    }

    suspend fun <T> execute(operation: UiTestOperation, block: suspend () -> T): T {
        check(operation !in blocked) { "Injected request failure for $operation" }
        val request = next.remove(operation)
        return if (request == null) block() else request.execute(block)
    }
}

class UiTestWorkoutRepository(
    private val delegate: RoomWorkoutRepository,
    private val requests: UiTestRequests,
) : WorkoutRepository by delegate, CompletedWorkoutSource by delegate {
    override suspend fun getUnfinishedWorkout() = requests.execute(UiTestOperation.LOAD_ACTIVE) {
        delegate.getUnfinishedWorkout()
    }
    override suspend fun getNextWorkoutPrescription(workoutOverride: Workout?) =
        requests.execute(UiTestOperation.LOAD_PRESCRIPTION) { delegate.getNextWorkoutPrescription(workoutOverride) }

    override suspend fun startWorkout(workout: Workout) = requests.execute(UiTestOperation.START) {
        delegate.startWorkout(workout)
    }
    override suspend fun switchWorkout(workoutId: Long, workout: Workout) = requests.execute(UiTestOperation.SWITCH) {
        delegate.switchWorkout(workoutId, workout)
    }
    override suspend fun completeSetAndStartRest(
        workoutId: Long, exercisePosition: Int, setPosition: Int, restDurationMillis: Long,
    ) = requests.execute(UiTestOperation.COMPLETE_SET) {
        delegate.completeSetAndStartRest(workoutId, exercisePosition, setPosition, restDurationMillis)
    }
    override suspend fun setCompletedReps(
        workoutId: Long, exercisePosition: Int, setPosition: Int, completedReps: Int,
    ) = requests.execute(UiTestOperation.SAVE_REPS) {
        delegate.setCompletedReps(workoutId, exercisePosition, setPosition, completedReps)
    }
    override suspend fun undoSetCompletion(workoutId: Long, exercisePosition: Int, setPosition: Int) =
        requests.execute(UiTestOperation.RESET_SET) { delegate.undoSetCompletion(workoutId, exercisePosition, setPosition) }

    override suspend fun editExercise(workoutId: Long, exercisePosition: Int, edit: ExerciseEdit) =
        requests.execute(UiTestOperation.EDIT) { delegate.editExercise(workoutId, exercisePosition, edit) }

    override suspend fun finalizeWorkout(workoutId: Long) = requests.execute(UiTestOperation.FINISH) {
        delegate.finalizeWorkout(workoutId)
    }
    override suspend fun getCompletedWorkout(workoutId: Long) = requests.execute(UiTestOperation.LOAD_COMPLETE) {
        delegate.getCompletedWorkout(workoutId)
    }
    override suspend fun getCompletedWorkoutById(workoutId: Long) = requests.execute(UiTestOperation.LOAD_DETAIL) {
        delegate.getCompletedWorkoutById(workoutId)
    }
}
