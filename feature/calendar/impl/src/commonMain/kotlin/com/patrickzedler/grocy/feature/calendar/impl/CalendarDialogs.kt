@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.calendar.CalendarColorSetting
import com.patrickzedler.grocy.core.model.calendar.argbOfHexColor
import com.patrickzedler.grocy.core.model.calendar.normalizedHexColor
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.action_copy_to_clipboard
import com.patrickzedler.grocy.core.resources.action_ok
import com.patrickzedler.grocy.core.resources.calendar_color_code
import com.patrickzedler.grocy.core.resources.calendar_color_invalid
import com.patrickzedler.grocy.core.resources.calendar_configure_colors
import com.patrickzedler.grocy.core.resources.calendar_share
import com.patrickzedler.grocy.core.resources.calendar_share_hint
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.masterdata_batteries
import com.patrickzedler.grocy.core.resources.masterdata_chores
import com.patrickzedler.grocy.core.resources.masterdata_products
import com.patrickzedler.grocy.core.resources.meal_plan_title
import com.patrickzedler.grocy.core.resources.tasks_title
import com.patrickzedler.grocy.core.ui.plainTextClipEntry
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Grocy's "Share/Integrate calendar (iCal)": the public [link] to copy and as QR code. */
@Composable
internal fun CalendarSharingDialog(link: String, onDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val title = stringResource(Res.string.calendar_share)
    ScrollableAlertDialog(onDismissRequest = onDismiss) {
        ScrollableAlertDialogContent(
            title = { Text(title) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        // Started right away, closing the dialog cancels its scope
                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            clipboard.setClipEntry(plainTextClipEntry(link, title))
                        }
                        onDismiss()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_copy_to_clipboard))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismiss()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                Text(
                    text = stringResource(Res.string.calendar_share_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                SegmentedContainer(index = 0, count = 2) {
                    Text(text = link, style = MaterialTheme.typography.bodyMedium)
                }
                SegmentedContainer(index = 1, count = 2) {
                    // Dark on white in every theme, or scanners do not read it
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.White)
                            .padding(16.dp),
                    ) {
                        Image(
                            painter = rememberQrCodePainter(link),
                            contentDescription = title,
                            modifier = Modifier.size(QrCodeSize),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grocy's "Configure colors": a color per kind of events the server has. A color saves as soon as
 * it is chosen and the events take it, like Grocy's settings.
 */
@Composable
internal fun CalendarColorsDialog(
    state: CalendarUiState,
    onColorChange: (CalendarColorSetting, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var editing by rememberSaveable { mutableStateOf<CalendarColorSetting?>(null) }
    val settings = state.colorSettings
    ScrollableAlertDialog(onDismissRequest = onDismiss) {
        ScrollableAlertDialogContent(
            title = { Text(stringResource(Res.string.calendar_configure_colors)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismiss()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_ok))
                }
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                settings.forEachIndexed { index, setting ->
                    val value = setting.valueOf(state.settings)
                    SegmentedListItem(
                        onClick = {
                            haptic.confirm()
                            editing = setting
                        },
                        shapes = ListItemDefaults.segmentedShapes(index, settings.size),
                        colors = SegmentedListDefaults.colors(),
                        modifier = Modifier.fillMaxWidth().clickCursor(),
                        verticalAlignment = Alignment.CenterVertically,
                        leadingContent = { ColorMark(value) },
                        supportingContent = { Text(value) },
                        content = { Text(stringResource(setting.label)) },
                    )
                }
            }
        }
    }
    editing?.let { setting ->
        CalendarColorDialog(
            title = stringResource(setting.label),
            color = setting.valueOf(state.settings),
            onConfirm = { color ->
                editing = null
                onColorChange(setting, color)
            },
            onDismiss = { editing = null },
        )
    }
}

private val CalendarColorSetting.label: StringResource
    get() = when (this) {
        CalendarColorSetting.PRODUCTS -> Res.string.masterdata_products
        CalendarColorSetting.TASKS -> Res.string.tasks_title
        CalendarColorSetting.CHORES -> Res.string.masterdata_chores
        CalendarColorSetting.BATTERIES -> Res.string.masterdata_batteries
        CalendarColorSetting.MEAL_PLAN -> Res.string.meal_plan_title
    }

@Composable
private fun ColorMark(hex: String) {
    Box(
        modifier = Modifier
            .size(ColorMarkSize)
            .clip(CircleShape)
            .background(if (argbOfHexColor(hex) != null) colorRolesOf(hex).color else Color.Transparent)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    )
}

/** Grocy's color input: a color of the palette or any other typed as "#rrggbb". */
@Composable
private fun CalendarColorDialog(
    title: String,
    color: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val text = rememberTextFieldState(color)
    var showsError by remember { mutableStateOf(false) }
    val chosen = normalizedHexColor(text.text.toString())
    fun confirm() {
        if (chosen != null) onConfirm(chosen) else showsError = true
    }
    ScrollableAlertDialog(onDismissRequest = onDismiss) {
        ScrollableAlertDialogContent(
            title = { Text(title) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        confirm()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismiss()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                SegmentedContainer(index = 0, count = 2) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CalendarPalette.forEach { swatch ->
                            ColorSwatch(
                                hex = swatch,
                                isSelected = swatch == chosen,
                                onClick = {
                                    haptic.confirm()
                                    showsError = false
                                    text.setTextAndPlaceCursorAtEnd(swatch)
                                },
                            )
                        }
                    }
                }
                SegmentedContainer(index = 1, count = 2) {
                    RoundedTextField(
                        state = text,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.calendar_color_code)) },
                        leadingIcon = { ColorMark(chosen.orEmpty()) },
                        isError = showsError && chosen == null,
                        supportingText = if (showsError && chosen == null) {
                            { Text(stringResource(Res.string.calendar_color_invalid)) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                        onKeyboardAction = { confirm() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val roles = colorRolesOf(hex)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(SwatchSize)
            .clip(CircleShape)
            .background(roles.color)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .clickCursor()
            .semantics { contentDescription = hex },
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_check),
                contentDescription = null,
                tint = roles.content,
            )
        }
    }
}

// Grocy's default colors first, then the rest of the colors of its theme
internal val CalendarPalette = listOf(
    "#007bff",
    "#28a745",
    "#ffc107",
    "#17a2b8",
    "#6c757d",
    "#dc3545",
    "#fd7e14",
    "#e83e8c",
    "#6f42c1",
    "#20c997",
    "#795548",
    "#343a40",
)

private val QrCodeSize = 200.dp
private val ColorMarkSize = 24.dp
private val SwatchSize = 48.dp
