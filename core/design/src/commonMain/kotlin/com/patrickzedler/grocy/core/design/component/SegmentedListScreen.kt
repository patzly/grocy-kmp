package com.patrickzedler.grocy.core.design.component

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.patrickzedler.grocy.core.design.StatusBarColorEffect
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_reload
import org.jetbrains.compose.resources.stringResource

/**
 * The standard screen: surfaceContainer background, a large top app bar that turns
 * surfaceContainerHighest when scrolled, and an [InsetLazyColumn] for [segmentedGroup]s.
 * The list shrinks with the keyboard, so a focused text field always stays above it.
 *
 * [navigationIcon] is usually a [NavigateBackButton], [actions] are [AppBarIconButton]s.
 * [floatingToolbar] (a [FloatingToolbarWithFab]) floats above the bottom edge and hides while
 * the list scrolls down; the list ends above it. It must apply the given modifier itself: the
 * scroll behavior measures how far to move it within the screen high box it is placed in.
 * [itemSpacing] is the space between list items, the segmented gap for long lists whose items
 * are one group each. With [onRefresh] the list can be pulled down to download its data again
 * (Material's contained loading indicator) or through "Reload" in the app bar's overflow menu,
 * which keyboards and screen readers reach.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedListScreen(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    snackbarHostState: SnackbarHostState? = null,
    floatingToolbar: (@Composable (FloatingToolbarScrollBehavior, Modifier) -> Unit)? = null,
    itemSpacing: Dp = SegmentedListDefaults.GroupSpacing,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
    )
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

    // The last item can scroll above the toolbar, snackbars appear above it
    val toolbarSpace = if (floatingToolbar != null) {
        FloatingToolbarDefaults.ContainerSize + FloatingToolbarDefaults.ScreenOffset
    } else {
        0.dp
    }

    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(
                if (floatingToolbar != null) {
                    Modifier.nestedScroll(toolbarScrollBehavior)
                } else {
                    Modifier
                },
            ),
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
                actions = {
                    actions()
                    // Pulling is a gesture; the menu refreshes for keyboards and screen readers
                    if (onRefresh != null) {
                        val reload = stringResource(Res.string.action_reload)
                        AppBarOverflowMenu(listOf(AppBarMenuItem(reload, onRefresh)))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = scrolledContainerColor,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            snackbarHostState?.let {
                SnackbarHost(it, Modifier.padding(bottom = toolbarSpace))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        // Below the list only the part of the navigation bar the keyboard leaves visible
        val bottomInsets = WindowInsets.navigationBars
            .exclude(WindowInsets.keyboard)
            .asPaddingValues()
        val topPadding = padding.calculateTopPadding() + SegmentedListDefaults.GroupSpacing
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh?.invoke() },
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            enabled = onRefresh != null,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = padding.calculateTopPadding()),
                )
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                if (floatingToolbar != null) {
                    // First for the accessibility order, so its actions are reached without the
                    // list. A direct child of this box: hiding moves it below the bottom edge.
                    floatingToolbar(
                        toolbarScrollBehavior,
                        Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                            .zIndex(1f),
                    )
                }
                InsetLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        // Before consuming the scaffold padding: the list ends above the keyboard
                        .windowInsetsPadding(WindowInsets.keyboard)
                        .consumeWindowInsets(padding),
                    state = listState,
                    contentPadding = remember(topPadding, bottomInsets, toolbarSpace) {
                        ScreenContentPadding(
                            top = topPadding,
                            bottomInsets = bottomInsets,
                            spacing = SegmentedListDefaults.GroupSpacing + toolbarSpace,
                        )
                    },
                    verticalArrangement = Arrangement.spacedBy(itemSpacing),
                    content = content,
                )
            }
        }
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
