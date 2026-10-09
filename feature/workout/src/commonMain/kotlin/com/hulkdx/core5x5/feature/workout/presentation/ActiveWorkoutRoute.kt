package com.hulkdx.core5x5.feature.workout.presentation

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ActiveWorkoutRoute(
    workoutId: Long,
    onBack: () -> Unit,
    onWorkoutCompleted: (Long) -> Unit,
    weightUnit: WeightUnit = WeightUnit.KG,
) {
    val viewModel: ActiveWorkoutViewModel = koinViewModel(parameters = { parametersOf(workoutId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }

    LaunchedEffect(uiState.requestedCompletedWorkoutId) {
        uiState.requestedCompletedWorkoutId?.let { workoutId ->
            viewModel.onCompletionRequestHandled()
            onWorkoutCompleted(workoutId)
        }
    }

    BackHandler(enabled = uiState.exerciseEdit != null) { viewModel.dismissExerciseEdit() }

    val edit = uiState.exerciseEdit
    if (edit != null) {
        ExerciseEditScreen(
            state = edit, onDismiss = viewModel::dismissExerciseEdit, onSave = viewModel::saveExerciseEdit,
            onNameChanged = viewModel::editName, onWeightChanged = viewModel::adjustWeight,
            onSetsChanged = viewModel::adjustSets, onRepsChanged = viewModel::adjustReps,
            onRestSelected = viewModel::selectRest, onOpenCustomRest = viewModel::openCustomRest,
            onCustomRestChanged = viewModel::editCustomRest, onApplyCustomRest = viewModel::applyCustomRest,
        )
    } else ActiveWorkoutScreen(
        uiState = uiState,
        weightUnit = weightUnit,
        onBack = onBack,
        onFinishWorkout = viewModel::finishWorkout,
        onRetryLoad = viewModel::loadWorkout,
        onToggleSet = viewModel::toggleSet,
        onEditExercise = { viewModel.openExerciseEdit(it, weightUnit) },
    )
}
