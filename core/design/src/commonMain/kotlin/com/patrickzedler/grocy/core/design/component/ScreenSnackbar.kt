package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.ic_rounded_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The margin of a snackbar in its parent, Material's. */
internal val SnackbarMargin = 12.dp

// Material's largest snackbar: 600 dp of content, 16 dp before and 8 dp after it
private val SnackbarMaxWidth = 624.dp

/**
 * How wide a snackbar gets: one of two panes spans it from edge to edge, everywhere else it has
 * Material's limit, so it stays readable in a wide window.
 */
internal fun snackbarMaxWidth(pane: ScreenPane?): Dp =
    if (pane == ScreenPane.List || pane == ScreenPane.Detail) Dp.Unspecified else SnackbarMaxWidth

/**
 * Material's snackbar, whose own composable always has its width limit, with [maxWidth] and the
 * haptics of the app's buttons.
 */
@Composable
internal fun ScreenSnackbar(data: SnackbarData, maxWidth: Dp) {
    val haptic = LocalHapticFeedback.current
    val visuals = data.visuals

    Surface(
        modifier = Modifier
            .padding(SnackbarMargin)
            .widthIn(max = maxWidth)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = SnackbarDefaults.color,
        contentColor = SnackbarDefaults.contentColor,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(start = 16.dp, end = if (visuals.withDismissAction) 0.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = visuals.message,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            visuals.actionLabel?.let { label ->
                TextButton(
                    onClick = {
                        haptic.confirm()
                        data.performAction()
                    },
                    modifier = Modifier.clickCursor(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SnackbarDefaults.actionContentColor,
                    ),
                ) {
                    Text(label)
                }
            }
            if (visuals.withDismissAction) {
                val description = stringResource(Res.string.action_close)
                HapticTooltipBox(text = description) {
                    IconButton(
                        onClick = {
                            haptic.confirm()
                            data.dismiss()
                        },
                        modifier = Modifier.clickCursor(),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = SnackbarDefaults.dismissActionContentColor,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_close),
                            contentDescription = description,
                        )
                    }
                }
            }
        }
    }
}
