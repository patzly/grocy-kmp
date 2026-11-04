package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The PDF [bytes] in [modifier], which needs a bounded height: it scrolls on its own. Android
 * renders its pages, pinching zooms; browsers show them with their own viewer. [onFailed] tells
 * that this device cannot show it, e.g. a damaged file or a mobile browser without a viewer.
 *
 * @param isCovered something of the app lies above it right now: a browser's viewer lies above
 *   all of Compose, so menus, dialogs and snackbars would be behind it; it hides meanwhile.
 */
@Composable
expect fun PdfDocumentView(
    bytes: ByteArray,
    isCovered: Boolean,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
)
