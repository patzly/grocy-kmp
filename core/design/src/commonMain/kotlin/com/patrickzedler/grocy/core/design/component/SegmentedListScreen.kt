package com.patrickzedler.grocy.core.design.component

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import com.patrickzedler.grocy.core.design.StatusBarColorEffect

/**
 * The standard screen: surfaceContainer background, a large top app bar that turns
 * surfaceContainerHighest when scrolled, and an [InsetLazyColumn] for [segmentedGroup]s.
 * The list shrinks with the keyboard, so a focused text field always stays above it.
 *
 * [navigationIcon] is usually a [NavigateBackButton], [actions] are [AppBarIconButton]s.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedListScreen(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    // The same color the large app bar computes from its collapsed fraction
    StatusBarColorEffect {
        lerp(
            containerColor,
            scrolledContainerColor,
            FastOutLinearInEasing.transform(scrollBehavior.state.collapsedFraction),
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = scrolledContainerColor,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        // Below the list only the part of the navigation bar the keyboard leaves visible
        val bottomInsets = WindowInsets.navigationBars
            .exclude(WindowInsets.keyboard)
            .asPaddingValues()
        val topPadding = padding.calculateTopPadding() + SegmentedListDefaults.GroupSpacing

        InsetLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // Before consuming the scaffold padding, so the list ends right above the keyboard
                .windowInsetsPadding(WindowInsets.keyboard)
                .consumeWindowInsets(padding),
            state = listState,
            contentPadding = remember(topPadding, bottomInsets) {
                ScreenContentPadding(
                    top = topPadding,
                    bottomInsets = bottomInsets,
                    spacing = SegmentedListDefaults.GroupSpacing,
                )
            },
            verticalArrangement = Arrangement.spacedBy(SegmentedListDefaults.GroupSpacing),
            content = content,
        )
    }
}

/**
 * Icon button of a top app bar. surfaceContainerHighest like the scrolled bar, so both become
 * one surface when the content scrolls below it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppBarIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    narrow: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current

    HapticTooltipBox(
        text = contentDescription,
        modifier = modifier,
        position = TooltipPosition.Below,
    ) {
        FilledIconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = if (narrow) {
                Modifier
                    .minimumInteractiveComponentSize()
                    .size(
                        IconButtonDefaults.smallContainerSize(
                            IconButtonDefaults.IconButtonWidthOption.Narrow,
                        ),
                    )
            } else {
                Modifier
            },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
            )
        }
    }
}
