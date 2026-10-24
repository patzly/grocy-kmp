package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.action_learn_more
import org.jetbrains.compose.resources.stringResource

@Composable
fun TextDialog(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    link: String? = null,
) {
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current

    ScrollableAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        TextDialogContent(
            title = title,
            text = text,
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
            onLearnMoreClick = link?.takeIf { it.isNotBlank() }?.let { url ->
                {
                    haptic.confirm()
                    uriHandler.tryOpenUri(url)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TextDialogContent(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit = {},
    onLearnMoreClick: (() -> Unit)? = null,
) {
    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(title) },
        confirmButton = {
            TextButton(
                onClick = onCloseClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_close))
            }
        },
        extraButton = onLearnMoreClick?.let { onClick ->
            {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_learn_more))
                }
            }
        },
        scrollableContentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp),
    ) {
        FormattedText(
            text = text,
            isDialog = true,
        )
    }
}

@Preview
@Composable
private fun TextDialogPreview() {
    GrocyTheme {
        TextDialogContent(
            title = "Title",
            text = "Text",
        )
    }
}

@Preview
@Composable
private fun TextDialogLinkPreview() {
    GrocyTheme {
        TextDialogContent(
            title = "Title",
            text = "Text",
            onLearnMoreClick = {},
        )
    }
}
