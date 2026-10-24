package com.hulkdx.core5x5.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.unveilIn
import androidx.compose.animation.veilOut
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEvent.SwipeEdge
import androidx.navigation3.runtime.NavKey

/** Recognize retained nested entries as well as roots when changing between independent tabs. */
fun isTabSwitch(initialContentKey: Any, targetContentKey: Any, tabContentKeys: Map<NavKey, List<Any>>): Boolean {
    val initialTab = tabContentKeys.entries.firstOrNull { initialContentKey in it.value }?.key
    val targetTab = tabContentKeys.entries.firstOrNull { targetContentKey in it.value }?.key
    return initialTab != null && targetTab != null && initialTab != targetTab
}

//
// copied from NavDisplay.ios.kt
//

fun <T : Any> appDefaultTransitionSpec():
        AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(500, easing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f)),
        ),
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            targetOffset = { it / 4 },
            animationSpec = tween(500, easing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f)),
        ) + veilOut(
            animationSpec = tween(500, easing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f)),
        ),
    )
}

fun <T : Any> appDefaultPopTransitionSpec():
        AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            initialOffset = { it / 4 },
            animationSpec = tween(500, easing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f)),
        ) + unveilIn(
            animationSpec = tween(500, easing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f))
        ),
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(500, easing = CubicBezierEasing(0.2833f, 0.99f, 0.31833f, 0.99f)),
        ),
    )
}

fun <T : Any> appDefaultPredictivePopTransitionSpec():
        AnimatedContentTransitionScope<Scene<T>>.(@SwipeEdge Int) -> ContentTransform = { edge ->
    val towards = if (edge == EDGE_LEFT) {
        AnimatedContentTransitionScope.SlideDirection.Right
    } else {
        AnimatedContentTransitionScope.SlideDirection.Left
    }
    ContentTransform(
        slideIntoContainer(
            towards = towards,
            initialOffset = { it / 4 },
            animationSpec = tween(500, easing = LinearEasing),
        ) + unveilIn(
            animationSpec = tween(500, easing = LinearEasing),
        ),
        slideOutOfContainer(
            towards = towards,
            animationSpec = tween(500, easing = LinearEasing),
        ),
    )
}
