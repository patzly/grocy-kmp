package com.patrickzedler.grocy.core.forms

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close_scanner
import com.patrickzedler.grocy.core.resources.action_scan_barcode
import com.patrickzedler.grocy.core.resources.ic_rounded_barcode_scanner
import com.patrickzedler.grocy.core.resources.ic_rounded_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The trailing action of a field that can be scanned: opens and closes its scanner. */
@Composable
fun ScannerToggleButton(
    isScannerOpen: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val description = stringResource(
        if (isScannerOpen) Res.string.action_close_scanner else Res.string.action_scan_barcode,
    )
    HapticTooltipBox(text = description) {
        IconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = Modifier.clickCursor(enabled = enabled),
            enabled = enabled,
        ) {
            Icon(
                painter = painterResource(
                    if (isScannerOpen) {
                        Res.drawable.ic_rounded_close
                    } else {
                        Res.drawable.ic_rounded_barcode_scanner
                    },
                ),
                contentDescription = description,
            )
        }
    }
}
