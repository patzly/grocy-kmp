package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow

/**
 * The standard screen: surfaceContainer background, a large top app bar that turns
 * surfaceContainerHighest when scrolled, and an [InsetLazyColumn] for [segmentedGroup]s.
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        InsetLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding),
            state = listState,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + SegmentedListDefaults.GroupSpacing,
                bottom = padding.calculateBottomPadding() + SegmentedListDefaults.GroupSpacing,
            ),
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
        positioning = TooltipAnchorPosition.Below,
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
