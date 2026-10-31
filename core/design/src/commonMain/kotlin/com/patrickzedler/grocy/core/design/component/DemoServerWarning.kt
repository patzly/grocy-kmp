package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_warning
import com.patrickzedler.grocy.core.resources.settings_server_demo
import com.patrickzedler.grocy.core.resources.settings_server_demo_warning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The warning that the app is logged in to Grocy's public demo: everything entered there is
 * public until the daily reset. A group of its own in the error colors, so it cannot be
 * overlooked or taken for a setting; with [onClick] it leads somewhere, e.g. the server
 * settings.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DemoServerWarning(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val contentColor = MaterialTheme.colorScheme.onErrorContainer
    val shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
    val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = contentColor,
        leadingContentColor = contentColor,
        supportingContentColor = contentColor,
    )
    val leading = @Composable {
        LeadingTrailingContentWrapper {
            Icon(painterResource(Res.drawable.ic_rounded_warning), contentDescription = null)
        }
    }
    val supporting = @Composable { Text(stringResource(Res.string.settings_server_demo_warning)) }
    val title = @Composable {
        Text(
            text = stringResource(Res.string.settings_server_demo),
            style = MaterialTheme.typography.bodyLargeEmphasized,
        )
    }

    if (onClick != null) {
        val haptic = LocalHapticFeedback.current
        SegmentedListItem(
            onClick = {
                haptic.confirm()
                onClick()
            },
            shapes = shapes,
            colors = colors,
            modifier = modifier.fillMaxWidth().clickCursor(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = leading,
            supportingContent = supporting,
            content = title,
        )
    } else {
        ListItem(
            modifier = modifier.fillMaxWidth(),
            leadingContent = leading,
            supportingContent = supporting,
            shapes = shapes,
            colors = colors,
            content = title,
        )
    }
}
