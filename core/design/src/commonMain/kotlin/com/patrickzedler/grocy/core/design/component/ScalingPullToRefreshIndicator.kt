package com.patrickzedler.grocy.core.design.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Pull to refresh indicator that starts right below the app bar and grows with the pull, like in
 * Google's apps. Material's own one slides out of the app bar, which has the screen's color while
 * nothing is scrolled, so it would appear out of nowhere.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScalingPullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.graphicsLayer {
            // The state holds the threshold while refreshing, also for a refresh from the menu
            val fraction = state.distanceFraction.coerceIn(0f, 1f)
            scaleX = fraction
            scaleY = fraction
            alpha = if (fraction > 0f) 1f else 0f
            translationY = fraction * IndicatorTravel.toPx()
        },
    ) {
        Crossfade(
            targetState = isRefreshing,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        ) { refreshing ->
            if (refreshing) {
                ContainedLoadingIndicator()
            } else {
                ContainedLoadingIndicator(progress = { state.distanceFraction.coerceIn(0f, 1f) })
            }
        }
    }
}

// How far the indicator moves down while growing, so it does not touch the app bar
private val IndicatorTravel = 16.dp
