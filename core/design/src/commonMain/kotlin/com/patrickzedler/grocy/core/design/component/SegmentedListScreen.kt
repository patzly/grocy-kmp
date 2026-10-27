package com.patrickzedler.grocy.core.design.component

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.patrickzedler.grocy.core.design.LocalPointerKind
import com.patrickzedler.grocy.core.design.StatusBarColorEffect
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_reload
import com.patrickzedler.grocy.core.resources.ic_rounded_refresh
import kotlin.math.roundToInt
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
 * [subtitle] (e.g. a summary of the list) shows below the title in onSurfaceVariant.
 * [navigationIcon] is usually a [NavigateBackButton], [actions] are [AppBarIconButton]s or
 * menus, together with "Reload" and the overflow menu one standard button group whose pressed
 * button grows; [menuItems] go into the overflow menu at the end of the bar, after
 * [actionMenuItems] (the actions on the screen's object) as a group of their own.
 * [pane]: the screen is one of two panes of a list-detail layout ([ScreenPane]): a small bar in
 * the background's color that lines both bars up, the list on a rounded surface below it.
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
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: List<AppBarAction> = emptyList(),
    menuItems: List<AppBarMenuItem> = emptyList(),
    actionMenuItems: List<AppBarMenuItem> = emptyList(),
    pane: ScreenPane? = null,
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
    val isSmallBar = isRefreshable || usesMouse || pane != null
    val scrollBehavior = if (isSmallBar) {
        TopAppBarDefaults.pinnedScrollBehavior(listState)
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    }
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
    )
    // Content of a pane never scrolls below the bar, which keeps the scrolled color
    val scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val containerColor = if (isRefreshable || pane != null) {
        scrolledContainerColor
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    // The small bar switches its color once content lies below it, animated like the bar does
    val isOverlapped by remember(scrollBehavior) {
        derivedStateOf { scrollBehavior.state.overlappedFraction > OVERLAP_THRESHOLD }
    }
    val smallBarFraction by animateFloatAsState(
        targetValue = if (isOverlapped || containerColor == scrolledContainerColor) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    // How far the bar has turned into its scrolled color, the large one like Material computes it
    val scrolledFraction = {
        if (isSmallBar) {
            smallBarFraction
        } else {
            FastOutLinearInEasing.transform(scrollBehavior.state.collapsedFraction)
        }
    }
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    StatusBarColorEffect { lerp(surfaceContainer, scrolledContainerColor, scrolledFraction()) }

    // The last item can scroll above the toolbar, snackbars appear above it
    val toolbarSpace = if (floatingToolbar != null) {
        FloatingToolbarDefaults.ContainerSize + FloatingToolbarDefaults.ScreenOffset
    } else {
        0.dp
    }

    // Snackbars keep a little more distance to the toolbar than its own screen offset
    val snackbarSpace = if (floatingToolbar != null) toolbarSpace + SnackbarToolbarGap else 0.dp

    Scaffold(
        modifier = modifier
            .then(if (pane != null) Modifier.paneMargin(pane) else Modifier)
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
            // The subtitle lines up with its title the same way
            val collapsedSubtitleStyle = MaterialTheme.typography.labelMedium
            val subtitleContent: (@Composable () -> Unit)? = subtitle?.let { text ->
                {
                    val isExpandedRow =
                        LocalTextStyle.current.fontSize != collapsedSubtitleStyle.fontSize
                    Text(
                        text = text,
                        modifier = if (isExpandedRow) Modifier.fillMaxWidth() else Modifier,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val reload = stringResource(Res.string.action_reload)
            val reloadIcon = painterResource(Res.drawable.ic_rounded_refresh)
            val allActions: @Composable RowScope.() -> Unit = {
                // Pulling is a gesture; the menu refreshes for keyboards and screen readers
                val reloadItems = if (onRefresh != null && !usesMouse) {
                    listOf(AppBarMenuItem(reload, onClick = onRefresh))
                } else {
                    emptyList()
                }
                val menuGroups = listOf(reloadItems, actionMenuItems, menuItems)
                val shownActions = buildList {
                    addAll(actions)
                    if (onRefresh != null && usesMouse) {
                        add(
                            AppBarAction { modifier, interactionSource ->
                                AppBarIconButton(
                                    painter = reloadIcon,
                                    contentDescription = reload,
                                    onClick = onRefresh,
                                    modifier = modifier,
                                    interactionSource = interactionSource,
                                )
                            },
                        )
                    }
                    if (menuGroups.any { it.isNotEmpty() }) {
                        add(
                            AppBarAction(narrow = true) { modifier, interactionSource ->
                                AppBarOverflowMenu(
                                    groups = menuGroups,
                                    modifier = modifier,
                                    interactionSource = interactionSource,
                                )
                            },
                        )
                    }
                }
                AppBarActions(shownActions)
            }
            val colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = scrolledContainerColor,
            )
            if (isSmallBar && subtitleContent != null) {
                TopAppBar(
                    title = titleContent,
                    subtitle = subtitleContent,
                    navigationIcon = navigationIcon,
                    actions = allActions,
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    colors = colors,
                    scrollBehavior = scrollBehavior,
                )
            } else if (isSmallBar) {
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
                    subtitle = subtitleContent,
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
                // Above the toolbar, following it while it slides out and back in, read during
                // placement so scrolling never recomposes
                SnackbarHost(
                    hostState = it,
                    modifier = Modifier.offset {
                        val state = toolbarScrollBehavior.state
                        val lift = snackbarLift(
                            toolbarSpace = snackbarSpace.toPx(),
                            toolbarOffset = state.offset,
                            offsetLimit = state.offsetLimit,
                        )
                        IntOffset(x = 0, y = -lift)
                    },
                )
            }
        },
        containerColor = if (pane != null) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) { padding ->
        // Below the list only the part of the navigation bar the keyboard leaves visible; a pane's
        // surface ends above both
        val bottomInsets = if (pane != null) {
            PaddingValues()
        } else {
            WindowInsets.navigationBars.exclude(WindowInsets.keyboard).asPaddingValues()
        }
        // A pane's surface starts below the bar, so do its list and the loading indicator
        val barPadding = if (pane != null) 0.dp else padding.calculateTopPadding()
        val topPadding = barPadding + SegmentedListDefaults.GroupSpacing
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh?.invoke() },
            modifier = if (pane != null) {
                Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .paneSurface()
            } else {
                Modifier.fillMaxSize()
            },
            state = pullToRefreshState,
            enabled = onRefresh != null && !usesMouse,
            indicator = {
                // Slides out from below the app bar
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = barPadding),
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
                // Over the list's part between the app bar and the navigation bar (a pane's
                // surface), beside the cutouts at the sides
                Box(
                    Modifier
                        .matchParentSize()
                        .windowInsetsPadding(WindowInsets.keyboard)
                        .padding(top = barPadding)
                        .padding(bottomInsets)
                        .then(
                            if (pane != null) {
                                Modifier
                            } else {
                                Modifier.windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                                )
                            },
                        )
                        .verticalScrollbar(listState),
                )
            }
        }
    }
}

/**
 * A button of a top app bar beside the overflow menu: [content] puts the modifier on its
 * outermost layout and passes the interaction source to its [AppBarIconButton]; a [narrow] one
 * (a menu's three dots) keeps less room at the bar's end.
 */
@Immutable
class AppBarAction(
    val narrow: Boolean = false,
    val content: @Composable (Modifier, MutableInteractionSource) -> Unit,
)

/**
 * The actions of a top app bar as a standard button group: 12.dp between the visible buttons,
 * the pressed one grows. Its edges keep the room of single buttons' touch targets, so the
 * buttons stay where single buttons would be.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppBarActions(actions: List<AppBarAction>) {
    if (actions.isEmpty()) return
    fun room(action: AppBarAction) = touchTargetRoom(
        if (action.narrow) IconButtonWidthOption.Narrow else IconButtonWidthOption.Uniform,
    )

    ButtonGroup(
        // A bar's few buttons always fit
        overflowIndicator = {},
        modifier = Modifier.padding(start = room(actions.first()), end = room(actions.last())),
    ) {
        actions.forEach { action -> animatedItem(content = action.content) }
    }
}

/**
 * Icon button of a top app bar on surfaceContainerHighest, which stands out on the
 * surfaceContainer bar (High was a single tone step above it) and becomes one surface with the
 * scrolled bar.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppBarIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    narrow: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
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
            modifier = smallIconButtonSize(
                if (narrow) IconButtonWidthOption.Narrow else IconButtonWidthOption.Uniform,
            ).clickCursor(),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSource,
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
            )
        }
    }
}

private val SnackbarToolbarGap = 4.dp

// Material switches the small app bar's color once content scrolled below it
private const val OVERLAP_THRESHOLD = 0.01f

/**
 * How far snackbars lie above the bottom: the [toolbarSpace] of a floating toolbar, less the
 * part it has slid out of the screen ([toolbarOffset] from 0 to its negative [offsetLimit]).
 */
internal fun snackbarLift(toolbarSpace: Float, toolbarOffset: Float, offsetLimit: Float): Int {
    val hidden = if (offsetLimit == 0f) 0f else (toolbarOffset / offsetLimit).coerceIn(0f, 1f)
    return (toolbarSpace * (1 - hidden)).roundToInt()
}
