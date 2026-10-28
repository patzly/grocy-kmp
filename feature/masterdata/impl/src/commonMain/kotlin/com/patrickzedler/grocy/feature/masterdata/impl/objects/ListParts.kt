package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_clear
import com.patrickzedler.grocy.core.resources.action_retry
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_offline
import com.patrickzedler.grocy.core.resources.ic_rounded_close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Disabled objects are greyed out like Grocy's muted rows. */
internal const val DisabledAlpha = 0.6f

/**
 * The search below the filter chips, a segmented item of its own that springs open from its top
 * edge, so it can be focused (keyboard included) right away without scrolling the list.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.searchItem(
    isVisible: Boolean,
    state: TextFieldState,
    onCloseClick: () -> Unit,
) {
    insetItem(key = "search") {
        // Group spacing around the search, which springs open below the chips
        Column(Modifier.padding(top = SearchSpacing)) {
            val motionScheme = MaterialTheme.motionScheme
            AnimatedVisibility(
                visible = isVisible,
                // From the top: the focused field then lies in view while it grows, so focusing
                // it right away (keyboard included) does not scroll the list
                enter = expandVertically(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    expandFrom = Alignment.Top,
                ) + fadeIn(motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(motionScheme.defaultEffectsSpec()),
            ) {
                SearchField(
                    state = state,
                    onCloseClick = onCloseClick,
                    // The keyboard closes with the start of the closing, not after it
                    isClosing = transition.targetState != EnterExitState.Visible,
                    modifier = Modifier.padding(
                        top = ListItemDefaults.SegmentedGap,
                        bottom = SearchSpacing + ListItemDefaults.SegmentedGap,
                    ),
                )
            }
        }
    }
}

/**
 * Opens the search at the top of the list, once the list scrolled up: a search opened while
 * scrolling took the focus when the list passed it, and bringing the field into view stopped
 * the scrolling short of the top.
 */
internal fun CoroutineScope.showSearchAtTop(listState: LazyListState, showSearch: () -> Unit) {
    launch {
        try {
            listState.animateScrollToItem(0)
        } finally {
            showSearch()
        }
    }
}

/**
 * The space below the filter chips of a list without search: the same group gap that the
 * closed [searchItem] leaves.
 */
internal fun LazyListScope.filterGapItem() {
    insetItem(key = "filter-gap") {
        Spacer(Modifier.height(SearchSpacing))
    }
}

/**
 * What an empty list shows instead of its items: the first download, its failure, or that
 * nothing (or nothing matching the filters) exists.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.emptyListItem(
    hasData: Boolean,
    refresh: RefreshState,
    emptyText: @Composable () -> String,
    filteredEmptyText: @Composable () -> String,
    onRetryClick: () -> Unit,
) {
    when {
        !hasData && refresh.isRefreshing -> insetItem(key = "loading") {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }

        !hasData && refresh.error != null -> insetItem(key = "error") {
            MessageItem(
                text = stringResource(
                    if (refresh.error == RefreshError.OFFLINE) {
                        Res.string.data_offline
                    } else {
                        Res.string.data_load_failed
                    },
                ),
                actionText = stringResource(Res.string.action_retry),
                onActionClick = onRetryClick,
            )
        }

        else -> insetItem(key = "empty") {
            MessageItem(text = if (hasData) filteredEmptyText() else emptyText())
        }
    }
}

@Composable
private fun SearchField(
    state: TextFieldState,
    onCloseClick: () -> Unit,
    isClosing: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isClosing) {
        if (isClosing) focusManager.clearFocus() else focusRequester.requestFocus()
    }

    SegmentedContainer(index = 0, count = 1, modifier = modifier) {
        RoundedTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(Res.string.action_search)) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        haptic.confirm()
                        onCloseClick()
                    },
                    modifier = Modifier.clickCursor(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_close),
                        contentDescription = stringResource(Res.string.action_clear),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
    }
}

@Composable
internal fun MessageItem(
    text: String,
    actionText: String? = null,
    onActionClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    SegmentedContainer(index = 0, count = 1) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (actionText != null) {
            Button(
                onClick = {
                    haptic.confirm()
                    onActionClick()
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
                    .clickCursor(),
            ) {
                Text(actionText)
            }
        }
    }
}

// The list puts the segmented gap between items: chips, gap, this, gap, items make a group gap
private val SearchSpacing =
    SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap * 2
