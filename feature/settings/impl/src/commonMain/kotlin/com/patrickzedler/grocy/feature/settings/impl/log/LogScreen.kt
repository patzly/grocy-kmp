package com.patrickzedler.grocy.feature.settings.impl.log

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.AppBarAction
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.ButtonGroupOverflowIndicator
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.component.smallIconButtonSize
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_copy_to_clipboard
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.action_reload
import com.patrickzedler.grocy.core.resources.action_send_feedback
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import com.patrickzedler.grocy.core.resources.ic_rounded_refresh
import com.patrickzedler.grocy.core.resources.log_empty
import com.patrickzedler.grocy.core.resources.log_report_crash
import com.patrickzedler.grocy.core.resources.title_log
import com.patrickzedler.grocy.core.resources.title_logcat
import com.patrickzedler.grocy.core.ui.AppDialog
import com.patrickzedler.grocy.core.ui.AppDialogHost
import com.patrickzedler.grocy.core.ui.plainTextClipEntry
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Reload sits in the app bar, so this screen has no overflow menu like the settings. */
@Composable
internal fun LogScreen(
    viewModel: LogViewModel,
    menuViewModel: SettingsMenuViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val unlockState by menuViewModel.unlockState.collectAsStateWithLifecycle()
    var activeDialog by rememberSaveable { mutableStateOf<AppDialog?>(null) }

    AppDialogHost(
        dialog = activeDialog,
        unlockState = unlockState,
        onDialogChange = { activeDialog = it },
    )

    LogContent(
        source = viewModel.source,
        log = viewModel.log,
        onBackClick = onBackClick,
        onReloadClick = viewModel::reload,
        onCopyClick = { text ->
            scope.launch { clipboard.setClipEntry(plainTextClipEntry(text, label = "logcat")) }
        },
        onFeedbackClick = { activeDialog = AppDialog.FEEDBACK },
        modifier = modifier,
    )
}

@Composable
private fun LogContent(
    source: LogSource,
    log: String?,
    onBackClick: () -> Unit,
    onReloadClick: () -> Unit,
    onCopyClick: (String) -> Unit,
    onFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListScreen(
        title = stringResource(
            when (source) {
                LogSource.LOGCAT -> Res.string.title_logcat
                LogSource.BROWSER -> Res.string.title_log
            },
        ),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        actions = listOf(
            AppBarAction { actionModifier, interactionSource ->
                AppBarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_refresh),
                    contentDescription = stringResource(Res.string.action_reload),
                    // A reload already running shows its indicator
                    onClick = { if (log != null) onReloadClick() },
                    modifier = actionModifier,
                    interactionSource = interactionSource,
                )
            },
        ),
    ) {
        logGroup(log = log, onCopyClick = onCopyClick, onFeedbackClick = onFeedbackClick)
    }
}

private fun LazyListScope.logGroup(
    log: String?,
    onCopyClick: (String) -> Unit,
    onFeedbackClick: () -> Unit,
) {
    segmentedGroup(key = "log") {
        SegmentedContainer(
            index = 0,
            count = 2,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentedTexts(supporting = stringResource(Res.string.log_report_crash))
                LogActions(
                    canCopy = !log.isNullOrEmpty(),
                    onCopyClick = { log?.let(onCopyClick) },
                    onFeedbackClick = onFeedbackClick,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
        SegmentedContainer(index = 1, count = 2) {
            LogText(log = log)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogActions(
    canCopy: Boolean,
    onCopyClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val copyInteractionSource = remember { MutableInteractionSource() }
    val feedbackInteractionSource = remember { MutableInteractionSource() }
    val copyText = stringResource(Res.string.action_copy_to_clipboard)
    val feedbackText = stringResource(Res.string.action_send_feedback)
    val onCopy = {
        haptic.confirm()
        onCopyClick()
    }
    val onFeedback = {
        haptic.confirm()
        onFeedbackClick()
    }

    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupOverflowIndicator {
                OverflowIndicator(
                    isShowing = { menuState.isShowing },
                    show = menuState::show,
                    dismiss = menuState::dismiss,
                )
            }
        },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        customItem(
            buttonGroupContent = {
                val contentPadding = ButtonDefaults.TextButtonContentPadding
                val layoutDirection = LocalLayoutDirection.current

                FilledTonalButton(
                    onClick = onCopy,
                    enabled = canCopy,
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(0.dp),
                    interactionSource = copyInteractionSource,
                    modifier = Modifier.animateWidth(
                        interactionSource = copyInteractionSource,
                        compressionLimit = contentPadding.calculateStartPadding(layoutDirection),
                    ).clickCursor(enabled = canCopy),
                ) {
                    ButtonText(text = copyText, contentPadding = contentPadding)
                }
            },
            menuContent = { menuState ->
                DropdownMenuItem(
                    text = { Text(copyText) },
                    modifier = Modifier.clickCursor(enabled = canCopy),
                    enabled = canCopy,
                    onClick = {
                        menuState.dismiss()
                        onCopy()
                    },
                )
            },
        )
        customItem(
            buttonGroupContent = {
                val contentPadding = ButtonDefaults.TextButtonContentPadding
                val layoutDirection = LocalLayoutDirection.current

                Button(
                    onClick = onFeedback,
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(0.dp),
                    interactionSource = feedbackInteractionSource,
                    modifier = Modifier.animateWidth(
                        interactionSource = feedbackInteractionSource,
                        compressionLimit = contentPadding.calculateEndPadding(layoutDirection),
                    ).clickCursor(),
                ) {
                    ButtonText(text = feedbackText, contentPadding = contentPadding)
                }
            },
            menuContent = { menuState ->
                DropdownMenuItem(
                    text = { Text(feedbackText) },
                    modifier = Modifier.clickCursor(),
                    onClick = {
                        menuState.dismiss()
                        onFeedback()
                    },
                )
            },
        )
    }
}

/** Text of a button that grows when pressed, the padding is on the text instead of the button. */
@Composable
private fun ButtonText(text: String, contentPadding: PaddingValues) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        modifier = Modifier
            .wrapContentWidth(unbounded = true)
            .padding(contentPadding),
    )
}

/** Shown in place of the buttons that do not fit, e.g. with a large font. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OverflowIndicator(
    isShowing: () -> Boolean,
    show: () -> Unit,
    dismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val contentDescription = stringResource(Res.string.action_more_options)

    HapticTooltipBox(text = contentDescription) {
        FilledIconButton(
            onClick = {
                haptic.confirm()
                if (isShowing()) dismiss() else show()
            },
            modifier = smallIconButtonSize().clickCursor(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_more_vert),
                contentDescription = contentDescription,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogText(log: String?) {
    when {
        log == null -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator()
        }

        log.isEmpty() -> SegmentedTexts(
            supporting = stringResource(Res.string.log_empty),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        // Long lines stay readable as they are, the text scrolls sideways
        else -> SelectionContainer {
            Text(
                text = log,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
    }
}

@Preview
@Composable
private fun LogScreenPreview() {
    GrocyTheme {
        LogContent(
            source = LogSource.LOGCAT,
            log = "E/AndroidRuntime: FATAL EXCEPTION: main\n" +
                "E/AndroidRuntime: java.lang.IllegalStateException: Preview",
            onBackClick = {},
            onReloadClick = {},
            onCopyClick = {},
            onFeedbackClick = {},
        )
    }
}
