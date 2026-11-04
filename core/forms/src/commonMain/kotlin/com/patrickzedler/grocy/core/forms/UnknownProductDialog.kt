package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.LocalPointerKind
import com.patrickzedler.grocy.core.design.component.FocusRequestEffect
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.booking_add_as_barcode
import com.patrickzedler.grocy.core.resources.booking_add_as_new_product
import com.patrickzedler.grocy.core.resources.booking_add_as_new_product_with_barcode
import com.patrickzedler.grocy.core.resources.booking_external_barcode_lookup
import com.patrickzedler.grocy.core.resources.booking_scan_again
import com.patrickzedler.grocy.core.resources.booking_unknown_code_message
import com.patrickzedler.grocy.core.resources.booking_unknown_code_title
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_barcode
import com.patrickzedler.grocy.core.resources.ic_rounded_barcode_scanner
import com.patrickzedler.grocy.core.resources.ic_rounded_box
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Create or assign product" while [flow] asks, with its answers in its order; those
 * Grocy's picker leaves out are the null callbacks. Grocy's letters answer on a hardware keyboard.
 *
 * @param products the choices now, to recognize the product created next.
 * @param lookupPlugin the server's barcode plugin, named by Grocy's button.
 * @param onOpenProduct opens the product form for a new product.
 * @param onBarcodeAssigned the user chooses the product now: the field's turn.
 * @param onScanAgain the camera button; null without a scanner.
 * @param onLookUp [UnknownProductFlow.lookUpExternally]; null where Grocy does not offer it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UnknownProductDialog(
    flow: UnknownProductFlow,
    products: List<Product>,
    onOpenProduct: (ProductFormRequest) -> Unit,
    onBarcodeAssigned: () -> Unit,
    onDismissRequest: () -> Unit,
    lookupPlugin: String? = null,
    onScanAgain: (() -> Unit)? = null,
    onLookUp: (() -> Unit)? = null,
) {
    val input = flow.input ?: return
    val answers = buildList {
        if (flow.canCreate) {
            add(
                UnknownProductAnswer('P', Res.drawable.ic_rounded_add) {
                    onOpenProduct(flow.newProductWithName(products))
                },
            )
        }
        add(
            UnknownProductAnswer('B', Res.drawable.ic_rounded_barcode) {
                flow.addAsBarcode()
                onBarcodeAssigned()
            },
        )
        if (flow.canCreate) {
            add(
                UnknownProductAnswer('A', Res.drawable.ic_rounded_box) {
                    onOpenProduct(flow.newProductWithBarcode())
                },
            )
        }
        if (flow.canCreate && lookupPlugin != null && onLookUp != null) {
            add(UnknownProductAnswer('E', Res.drawable.ic_rounded_search, onLookUp))
        }
        if (onScanAgain != null) {
            add(
                UnknownProductAnswer('C', Res.drawable.ic_rounded_barcode_scanner) {
                    flow.scanAgain()
                    onScanAgain()
                },
            )
        }
    }
    val haptic = LocalHapticFeedback.current
    // Keys belong to the dialog; the browser's text input of a focused field behind it took them
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusManager.clearFocus() }
    val dialogFocus = remember { FocusRequester() }
    // Not AutoFocusEffect: its saved state would keep every later question from taking the focus
    var isFocusDone by remember { mutableStateOf(false) }
    FocusRequestEffect(!isFocusDone, dialogFocus) { isFocusDone = true }
    val showsLetters = LocalInputModeManager.current.inputMode == InputMode.Keyboard ||
        LocalPointerKind.current.usesMouse

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        ScrollableAlertDialogContent(
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val letter = event.utf16CodePoint.toChar().uppercaseChar()
                val answer = answers.firstOrNull { it.letter == letter }
                answer?.onClick?.invoke()
                answer != null
            },
            title = { Text(stringResource(Res.string.booking_unknown_code_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismissRequest()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .focusRequester(dialogFocus)
                    .focusable(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.booking_unknown_code_message, input),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    answers.forEachIndexed { index, answer ->
                        SegmentedListItem(
                            onClick = {
                                haptic.confirm()
                                answer.onClick()
                            },
                            modifier = Modifier.clickCursor(),
                            shapes = ListItemDefaults.segmentedShapes(
                                index = index,
                                count = answers.size,
                            ),
                            colors = SegmentedListDefaults.colors(),
                            leadingContent = { Icon(painterResource(answer.icon), null) },
                            trailingContent = if (showsLetters) {
                                { Text(answer.letter.toString()) }
                            } else {
                                null
                            },
                            supportingContent = if (answer.letter == 'E') {
                                lookupPlugin?.let { { Text(it) } }
                            } else {
                                null
                            },
                        ) {
                            Text(stringResource(answerText(answer.letter)))
                        }
                    }
                }
            }
        }
    }
}

private class UnknownProductAnswer(
    val letter: Char,
    val icon: DrawableResource,
    val onClick: () -> Unit,
)

private fun answerText(letter: Char) = when (letter) {
    'P' -> Res.string.booking_add_as_new_product
    'B' -> Res.string.booking_add_as_barcode
    'A' -> Res.string.booking_add_as_new_product_with_barcode
    'E' -> Res.string.booking_external_barcode_lookup
    else -> Res.string.booking_scan_again
}
