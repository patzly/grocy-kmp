package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.data.productfacts.isLookupBarcode
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.forms.FormDialog
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.ScannerToggleButton
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.barcode_barcode
import com.patrickzedler.grocy.core.resources.product_facts_barcode_invalid
import com.patrickzedler.grocy.core.resources.product_facts_look_up
import com.patrickzedler.grocy.core.resources.product_facts_look_up_action
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import org.jetbrains.compose.resources.stringResource

/**
 * The barcode of a new product to look up, typed or scanned: a scan looks up at once. The camera
 * opens only when asked for, a form opened to type a name does not need it.
 */
@Composable
internal fun ProductFactsDialog(
    onLookUp: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val barcode = rememberTextFieldState()
    var isScannerOpen by rememberSaveable { mutableStateOf(false) }
    var showsError by rememberSaveable { mutableStateOf(false) }
    val scannerShape = MaterialTheme.shapes.small
    val scannerColor = MaterialTheme.colorScheme.surfaceContainerHighest

    FormDialog(
        title = stringResource(Res.string.product_facts_look_up),
        isSaving = false,
        error = null,
        saveText = stringResource(Res.string.product_facts_look_up_action),
        onSave = {
            val code = barcode.text.toString().trim()
            showsError = !isLookupBarcode(code)
            if (!showsError) onLookUp(code)
        },
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = barcode,
                        autoFocus = remember { !isScannerOpen },
                        isLastField = true,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Number,
                        label = stringResource(Res.string.barcode_barcode),
                        error = stringResource(Res.string.product_facts_barcode_invalid)
                            .takeIf { showsError },
                        index = index,
                        count = count,
                        trailingAction = {
                            ScannerToggleButton(
                                isScannerOpen = isScannerOpen,
                                onClick = { isScannerOpen = !isScannerOpen },
                            )
                        },
                        below = if (isScannerOpen) {
                            {
                                BarcodeScanner(
                                    onBarcodeScanned = { scanned ->
                                        barcode.setTextAndPlaceCursorAtEnd(scanned.value)
                                        isScannerOpen = false
                                        showsError = !isLookupBarcode(scanned.value)
                                        if (!showsError) onLookUp(scanned.value)
                                    },
                                    shape = scannerShape,
                                    containerColor = scannerColor,
                                )
                            }
                        } else {
                            null
                        },
                    )
                },
            )
        }
    }
}
