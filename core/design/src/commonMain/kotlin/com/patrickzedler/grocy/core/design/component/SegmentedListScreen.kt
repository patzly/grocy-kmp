package com.patrickzedler.grocy.core.design.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.patrickzedler.grocy.core.design.LocalPointerKind
import com.patrickzedler.grocy.core.design.StatusBarColorEffect
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_reload
import com.patrickzedler.grocy.core.resources.ic_rounded_refresh
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The standard screen: surfaceContainer background, a large top app bar that turns
 * surfaceContainerHighest when scrolled, and an [InsetLazyColumn] for [segmentedGroup]s.
 * Screens with [onRefresh] get a small centered app bar that stays in place: collapsing it would
 * fight with pulling to refresh. So do all screens while a mouse is used, whose wheel cannot
 * collapse the large bar reliably. It always has its scrolled color, so the loading indicator
 * visibly comes out from below it.
 * The list shrinks with the keyboard, so a focused text field always stays above it.
 *
 * [navigationIcon] is usually a [NavigateBackButton], [actions] are [AppBarIconButton]s,
 * [menuItems] go into the overflow menu at the end of the bar, after [actionMenuItems] (the
 * actions on the screen's object) as a group of their own.
 * [isBesideList]: the screen is the detail pane beside its list, so it takes the list's small
 * bar in its color, which lines both bars up.
 * [floatingToolbar] (a [FloatingToolbarWithFab]) floats above the bottom edge and hides while
 * the list scrolls down; the list ends above it. It must apply the given modifier itself: the
 * scroll behavior measures how far to move it within the screen high box it is placed in.
 * [itemSpacing] is the space between list items, the segmented gap for long lists whose items
 * are one group each. With [onRefresh] the list can be pulled down to download its data again
 * (Material's contained loading indicator), or through "Reload" in the app bar's overflow menu
 * for keyboards and screen readers. With a mouse, which cannot pull (its wheel would only show
 * the indicator), pulling is off and "Reload" is a button of the app bar.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedListScreen(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    menuItems: List<AppBarMenuItem> = emptyList(),
    actionMenuItems: List<AppBarMenuItem> = emptyList(),
    isBesideList: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    snackbarHostState: SnackbarHostState? = null,
    floatingToolbar: (@Composable (FloatingToolbarScrollBehavior, Modifier) -> Unit)? = null,
    itemSpacing: Dp = SegmentedListDefaults.GroupSpacing,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val isRefreshable = onRefresh != null
    val usesMouse = LocalPointerKind.current.usesMouse
    // A mouse wheel has no gesture end the large bar could settle on, it stayed half collapsed
    val isSmallBar = isRefreshable || usesMouse || isBesideList
    val scrollBehavior = if (isSmallBar) {
        TopAppBarDefaults.pinnedScrollBehavior(listState)
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    }
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
    )
    val scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val containerColor = if (isRefreshable || isBesideList) {
        scrolledContainerColor
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    // The small bar switches its color once content lies below it, animated like the bar does
    val isOverlapped by remember(scrollBehavior) {
        derivedStateOf { scrollBehavior.state.overlappedFraction > OVERLAP_THRESHOLD }
    }
    val smallBarColor by animateColorAsState(
        targetValue = if (isOverlapped) scrolledContainerColor else containerColor,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    StatusBarColorEffect {
        if (isSmallBar) {
            smallBarColor
        } else {
            // The same color the large app bar computes from its collapsed fraction
            lerp(
                containerColor,
                scrolledContainerColor,
                FastOutLinearInEasing.transform(scrollBehavior.state.collapsedFraction),
            )
        }
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
            val titleContent: @Composable () -> Unit = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Material 3 Expressive: the expanded title starts at the edge, the collapsed one is
            // centered. Compose aligns both rows alike, so the bar centers its titles and the
            // expanded row's title fills its width with the text at its start, which places it
            // exactly where a start aligned title would be.
            val collapsedTitleStyle = MaterialTheme.typography.titleLarge
            val largeTitleContent: @Composable () -> Unit = {
                val isExpandedRow = LocalTextStyle.current.fontSize != collapsedTitleStyle.fontSize
                Text(
                    text = title,
                    modifier = if (isExpandedRow) Modifier.fillMaxWidth() else Modifier,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val allActions: @Composable RowScope.() -> Unit = {
                actions()
                val reload = stringResource(Res.string.action_reload)
                if (onRefresh != null && usesMouse) {
                    AppBarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_refresh),
                        contentDescription = reload,
                        onClick = onRefresh,
                    )
                }
                // Pulling is a gesture; the menu refreshes for keyboards and screen readers
                val reloadItems = if (onRefresh != null && !usesMouse) {
                    listOf(AppBarMenuItem(reload, onRefresh))
                } else {
                    emptyList()
                }
                AppBarOverflowMenu(groups = listOf(reloadItems, actionMenuItems, menuItems))
            }
            val colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = scrolledContainerColor,
            )
            if (isSmallBar) {
                CenterAlignedTopAppBar(
                    title = titleContent,
                    navigationIcon = navigationIcon,
                    actions = allActions,
                    colors = colors,
                    scrollBehavior = scrollBehavior,
                )
            } else {
                LargeFlexibleTopAppBar(
                    title = largeTitleContent,
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    navigationIcon = navigationIcon,
                    actions = allActions,
                    colors = colors,
                    scrollBehavior = scrollBehavior,
                )
            }
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
            enabled = onRefresh != null && !usesMouse,
            indicator = {
                // Slides out from below the app bar
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
            }.clickCursor(),
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

// Material switches the small app bar's color once content scrolled below it
private const val OVERLAP_THRESHOLD = 0.01f
