package com.patrickzedler.grocy.feature.settings.impl.general

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.GroupTitle
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.OverflowMenuPopup
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.dialogListScrollbar
import com.patrickzedler.grocy.core.design.component.smallIconButtonSize
import com.patrickzedler.grocy.core.model.start.StartLayout
import com.patrickzedler.grocy.core.model.start.StartSection
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_add
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.action_save
import com.patrickzedler.grocy.core.resources.ic_rounded_drag_handle
import com.patrickzedler.grocy.core.resources.ic_rounded_horizontal_rule
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import com.patrickzedler.grocy.core.resources.ic_rounded_settings_backup_restore
import com.patrickzedler.grocy.core.resources.start_layout_available
import com.patrickzedler.grocy.core.resources.start_layout_divider
import com.patrickzedler.grocy.core.resources.start_layout_divider_description
import com.patrickzedler.grocy.core.resources.start_layout_drag
import com.patrickzedler.grocy.core.resources.start_layout_move_down
import com.patrickzedler.grocy.core.resources.start_layout_move_up
import com.patrickzedler.grocy.core.resources.start_layout_remove
import com.patrickzedler.grocy.core.resources.start_layout_reset
import com.patrickzedler.grocy.core.resources.start_layout_shown
import com.patrickzedler.grocy.core.resources.start_layout_title
import com.patrickzedler.grocy.core.ui.icon
import com.patrickzedler.grocy.core.ui.title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val KEY_RESET = "reset"
private const val KEY_SHOWN = "shown"
private const val KEY_AVAILABLE = "available"

/** What the dialog's one list shows, so a row dragged from one group to the other stays itself. */
private sealed interface DialogRow {
    val key: String

    data object Reset : DialogRow {
        override val key = KEY_RESET
    }

    data class Heading(override val key: String) : DialogRow

    data class Movable(
        val row: StartLayoutRow,
        val index: Int,
        val count: Int,
        val isShown: Boolean,
    ) : DialogRow {
        override val key = row.key
    }
}

private fun dialogRowsOf(draft: StartLayoutDraft): List<DialogRow> = buildList {
    add(DialogRow.Reset)
    add(DialogRow.Heading(KEY_SHOWN))
    draft.shown.forEachIndexed { index, row ->
        add(DialogRow.Movable(row, index, draft.shown.size, isShown = true))
    }
    add(DialogRow.Heading(KEY_AVAILABLE))
    val available = draft.available
    available.forEachIndexed { index, row ->
        add(DialogRow.Movable(row, index, available.size, isShown = false))
    }
}

private fun draftSaver(sections: List<StartSection>) = Saver<StartLayoutDraft, String>(
    save = { it.toLayout().stored() },
    restore = { StartLayoutDraft.of(StartLayout.of(it), sections) },
)

/**
 * Arranges the start screen: what it shows, then what is available, in one list, so a row can be
 * dragged from one to the other. Nothing is stored until "Save"; a turned device keeps the draft.
 */
@Composable
internal fun StartLayoutDialog(
    layout: StartLayout,
    sections: List<StartSection>,
    onSave: (StartLayout) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var draft by rememberSaveable(stateSaver = draftSaver(sections)) {
        mutableStateOf(StartLayoutDraft.of(layout, sections))
    }
    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        StartLayoutDialogContent(
            draft = draft,
            defaultDraft = remember(sections) {
                StartLayoutDraft.of(StartLayout.Default, sections)
            },
            onDraftChange = { draft = it },
            onSaveClick = { onSave(draft.toLayout()) },
            onCancelClick = onDismissRequest,
        )
    }
}

/** A drag in progress: where the row is held, in the list's viewport. */
private class RowDrag(
    private val listState: LazyListState,
    private val haptic: HapticFeedback,
    var draft: StartLayoutDraft,
    var onDraftChange: (StartLayoutDraft) -> Unit,
) {
    var key by mutableStateOf<String?>(null)
        private set

    private var top by mutableFloatStateOf(0f)

    private fun item(key: String?) = listState.layoutInfo.visibleItemsInfo.firstOrNull {
        it.key == key
    }

    /** How far the row is held away from its place in the list. */
    fun translationOf(rowKey: String): Float =
        if (rowKey == key) item(rowKey)?.let { top - it.offset } ?: 0f else 0f

    private val isOverAvailable: Boolean
        get() {
            val row = item(key) ?: return false
            val heading = item(KEY_AVAILABLE) ?: return false
            return top + row.size / 2f >= heading.offset
        }

    fun start(rowKey: String) {
        top = item(rowKey)?.offset?.toFloat() ?: return
        key = rowKey
        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    fun moveBy(amount: Float) {
        top += amount
        follow()
    }

    /** Keeps the held row in its place while the list scrolls below it, at the list's ends. */
    suspend fun scrollAtEnds() {
        while (key != null) {
            withFrameNanos {}
            val row = item(key) ?: continue
            val info = listState.layoutInfo
            val speed = when {
                top < info.viewportStartOffset + EDGE_PX -> -SCROLL_PX
                top + row.size > info.viewportEndOffset - EDGE_PX -> SCROLL_PX
                else -> 0f
            }
            if (speed != 0f && listState.scrollBy(speed) != 0f) follow()
        }
    }

    private fun follow() {
        val current = key ?: return
        val row = item(current) ?: return
        val new = if (isOverAvailable) {
            // Stays a row of the list until released, removing it now would end the gesture
            if (draft.isShown(current)) draft.placed(current, draft.shown.lastIndex) else draft
        } else {
            val others = draft.shown.map { it.key }.filter { it != current }
            val middles = others.mapNotNull { other ->
                item(other)?.let { other to it.offset + it.size / 2f }
            }.toMap()
            val index = dropIndex(others, middles, top + row.size / 2f)
                ?: if (others.isEmpty()) 0 else return
            draft.placed(current, index)
        }
        if (new != draft) {
            draft = new
            onDraftChange(new)
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    fun end() {
        val current = key ?: return
        if (isOverAvailable && draft.isShown(current)) {
            draft = draft.removed(current)
            onDraftChange(draft)
        }
        key = null
        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
    }

    private companion object {
        const val EDGE_PX = 96f
        const val SCROLL_PX = 12f
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StartLayoutDialogContent(
    draft: StartLayoutDraft,
    defaultDraft: StartLayoutDraft,
    onDraftChange: (StartLayoutDraft) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val drag = remember(listState) { RowDrag(listState, haptic, draft, onDraftChange) }
    drag.draft = draft
    drag.onDraftChange = onDraftChange
    LaunchedEffect(drag.key) { drag.scrollAtEnds() }

    ScrollableAlertDialogContent(
        title = { Text(stringResource(Res.string.start_layout_title)) },
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.confirm()
                    onSaveClick()
                },
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    haptic.confirm()
                    onCancelClick()
                },
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        isScrollableControlledByContent = true,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().dialogListScrollbar(listState),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            items(dialogRowsOf(draft), key = { it.key }) { row ->
                when (row) {
                    DialogRow.Reset -> ResetButton(
                        enabled = draft.toLayout() != defaultDraft.toLayout(),
                        onClick = { onDraftChange(defaultDraft) },
                    )
                    is DialogRow.Heading -> GroupTitle(
                        text = stringResource(
                            if (row.key == KEY_SHOWN) {
                                Res.string.start_layout_shown
                            } else {
                                Res.string.start_layout_available
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .padding(top = 16.dp),
                    )
                    is DialogRow.Movable -> MovableRow(row, drag, onDraftChange, draft)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LazyItemScope.ResetButton(enabled: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    FilledTonalButton(
        onClick = {
            haptic.confirm()
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().clickCursor(enabled = enabled),
        shapes = ButtonDefaults.shapes(),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_rounded_settings_backup_restore),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(Res.string.start_layout_reset))
    }
}

/** One composable for every moving row: a held row survives changing from one group to another. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LazyItemScope.MovableRow(
    row: DialogRow.Movable,
    drag: RowDrag,
    onDraftChange: (StartLayoutDraft) -> Unit,
    draft: StartLayoutDraft,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged = drag.key == row.key
    val moveUp = stringResource(Res.string.start_layout_move_up)
    val moveDown = stringResource(Res.string.start_layout_move_down)
    val remove = stringResource(Res.string.start_layout_remove)
    val add = stringResource(Res.string.action_add)
    val actions = buildList {
        if (row.isShown) {
            if (row.index > 0) add(moveUp to { onDraftChange(draft.moved(row.key, -1)) })
            if (row.index < row.count - 1) {
                add(moveDown to { onDraftChange(draft.moved(row.key, 1)) })
            }
            add(remove to { onDraftChange(draft.removed(row.key)) })
        } else {
            add(add to { onDraftChange(draft.added(row.key)) })
        }
    }
    val rowData = row.row
    val title = when (rowData) {
        is StartLayoutRow.Divider -> stringResource(Res.string.start_layout_divider)
        is StartLayoutRow.Section -> stringResource(rowData.section.title)
    }

    val haptic = LocalHapticFeedback.current
    var isMenuOpen by rememberSaveable(row.key) { mutableStateOf(false) }

    SegmentedListItem(
        // Material's dragged look needs the clickable item; a tap offers what the handle can't
        onClick = {
            haptic.confirm()
            isMenuOpen = true
        },
        shapes = ListItemDefaults.segmentedShapes(index = row.index, count = row.count),
        colors = SegmentedListDefaults.colors(),
        elevation = ListItemDefaults.elevation(),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .clickCursor()
            .then(if (isDragged) Modifier else Modifier.animateItem())
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer { translationY = drag.translationOf(row.key) }
            // Everything the handle does by dragging, also for screen readers
            .semantics {
                customActions = actions.map { (label, action) ->
                    CustomAccessibilityAction(label) {
                        action()
                        true
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DragHandle(row.key, drag, interactionSource)
                LeadingTrailingContentWrapper {
                    Icon(
                        painter = painterResource(
                            when (rowData) {
                                is StartLayoutRow.Divider -> Res.drawable.ic_rounded_horizontal_rule
                                is StartLayoutRow.Section -> rowData.section.icon
                            },
                        ),
                        contentDescription = null,
                    )
                }
            }
        },
        trailingContent = {
            RowMenu(
                isOpen = isMenuOpen,
                onOpenChange = { isMenuOpen = it },
                items = actions.map { (label, action) ->
                    AppBarMenuItem(text = label, onClick = action)
                },
            )
        },
        supportingContent = if (rowData is StartLayoutRow.Divider) {
            { Text(stringResource(Res.string.start_layout_divider_description)) }
        } else {
            null
        },
        content = { Text(title) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowMenu(isOpen: Boolean, onOpenChange: (Boolean) -> Unit, items: List<AppBarMenuItem>) {
    val haptic = LocalHapticFeedback.current
    val description = stringResource(Res.string.action_more_options)
    Box {
        HapticTooltipBox(text = description) {
            IconButton(
                onClick = {
                    haptic.confirm()
                    onOpenChange(!isOpen)
                },
                modifier = smallIconButtonSize(IconButtonWidthOption.Narrow).clickCursor(),
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_more_vert),
                    contentDescription = description,
                )
            }
        }
        OverflowMenuPopup(
            expanded = isOpen,
            groups = listOf(items),
            onDismissRequest = { onOpenChange(false) },
        )
    }
}

/** Tells the item it is dragged, which then takes Material's dragged shape, color and elevation. */
@Composable
private fun DragHandle(
    key: String,
    drag: RowDrag,
    interactionSource: MutableInteractionSource,
) {
    val description = stringResource(Res.string.start_layout_drag)
    var start by remember { mutableStateOf<DragInteraction.Start?>(null) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(HandleSize)
            .clickCursor()
            .padding(end = 4.dp)
            .pointerInput(key) {
                fun finish() {
                    start?.let { interactionSource.tryEmit(DragInteraction.Stop(it)) }
                    start = null
                    drag.end()
                }
                detectDragGestures(
                    onDragStart = {
                        val interaction = DragInteraction.Start()
                        start = interaction
                        interactionSource.tryEmit(interaction)
                        drag.start(key)
                    },
                    onDragEnd = ::finish,
                    onDragCancel = ::finish,
                    onDrag = { change, amount ->
                        change.consume()
                        drag.moveBy(amount.y)
                    },
                )
            },
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_rounded_drag_handle),
            contentDescription = description,
        )
    }
}

private val HandleSize = 40.dp
