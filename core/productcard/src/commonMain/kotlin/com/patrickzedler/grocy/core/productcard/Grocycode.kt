package com.patrickzedler.grocy.core.productcard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.grocycode_description
import com.patrickzedler.grocy.core.resources.grocycode_download
import com.patrickzedler.grocy.core.resources.grocycode_print
import com.patrickzedler.grocy.core.resources.grocycode_print_failed
import com.patrickzedler.grocy.core.resources.grocycode_printed
import com.patrickzedler.grocy.core.resources.grocycode_save_failed
import com.patrickzedler.grocy.core.resources.ic_rounded_download
import com.patrickzedler.grocy.core.resources.ic_rounded_print
import com.patrickzedler.grocy.core.scanner.writer.BarcodeMatrix
import com.patrickzedler.grocy.core.scanner.writer.Grocycode
import com.patrickzedler.grocy.core.scanner.writer.PngWriter
import com.patrickzedler.grocy.core.ui.SavedFile
import com.patrickzedler.grocy.core.ui.rememberFileSaver
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's grocycode of an object: the code [code] to scan, "Download" as PNG and, where the
 * server prints labels ([onPrintClick] not null), "Print on label printer".
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GrocycodeItem(
    code: String,
    objectName: String,
    twoDimensional: Boolean,
    onDownloadClick: () -> Unit,
    onPrintClick: (() -> Unit)?,
    index: Int,
    count: Int,
) {
    val haptic = LocalHapticFeedback.current
    val matrix = remember(code, twoDimensional) { Grocycode.matrix(code, twoDimensional) }

    SegmentedContainer(index = index, count = count) {
        SegmentedTexts(
            supporting = stringResource(Res.string.grocycode_description, objectName),
        )
        Spacer(Modifier.height(16.dp))
        GrocycodeImage(
            matrix = matrix,
            description = code,
            modifier = if (twoDimensional) {
                Modifier.size(GRID_CODE_SIZE)
            } else {
                Modifier.fillMaxWidth().height(LINEAR_CODE_HEIGHT)
            },
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = {
                    haptic.confirm()
                    onDownloadClick()
                },
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_download),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(Res.string.grocycode_download))
            }
            if (onPrintClick != null) {
                OutlinedButton(
                    onClick = {
                        haptic.confirm()
                        onPrintClick()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_print),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(Res.string.grocycode_print))
                }
            }
        }
    }
}

/**
 * The code's modules, black on white whatever the theme, with a quiet zone, so scanners read
 * it from the screen too.
 */
@Composable
internal fun GrocycodeImage(
    matrix: BarcodeMatrix,
    description: String,
    modifier: Modifier = Modifier,
) {
    val isLinear = matrix.height == 1
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(Color.White)
            .padding(if (isLinear) 12.dp else 8.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = if (isLinear) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxSize().aspectRatio(1f)
            },
        ) {
            val moduleWidth = size.width / matrix.width
            val moduleHeight = if (isLinear) size.height else size.height / matrix.height
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (matrix[x, y]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * moduleWidth, y * moduleHeight),
                            // A hair wider, so neighbouring modules leave no gaps
                            size = Size(moduleWidth + 0.5f, moduleHeight + 0.5f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Remembers what saves a grocycode as "Grocycode.png" like Grocy's download; a failure shows in
 * [snackbarHostState].
 */
@Composable
fun rememberGrocycodeDownload(
    snackbarHostState: SnackbarHostState,
): (code: String, twoDimensional: Boolean) -> Unit {
    val scope = rememberCoroutineScope()
    val save = rememberFileSaver(mimeType = PNG) { success ->
        if (!success) {
            scope.launch {
                snackbarHostState.showSnackbar(getString(Res.string.grocycode_save_failed))
            }
        }
    }
    return remember(save) {
        { code, twoDimensional ->
            val matrix = Grocycode.matrix(code, twoDimensional)
            val png = if (twoDimensional) {
                PngWriter.write(matrix, moduleSize = GRID_MODULE_PIXELS, quietZone = 1)
            } else {
                PngWriter.write(
                    matrix,
                    moduleSize = LINEAR_MODULE_PIXELS,
                    quietZone = LINEAR_QUIET_ZONE,
                    linearHeight = LINEAR_HEIGHT_MODULES,
                )
            }
            save(SavedFile(FILE_NAME, png))
        }
    }
}

/** Shows the results of [labels] in [snackbarHostState]. */
@Composable
fun LabelPrintEffect(labels: LabelPrintViewModel, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(labels) {
        labels.results.collect { result ->
            snackbarHostState.showSnackbar(
                when (result) {
                    LabelPrintResult.Printed -> getString(Res.string.grocycode_printed)
                    is LabelPrintResult.Failed ->
                        result.error.actionMessage(Res.string.grocycode_print_failed)
                },
            )
        }
    }
}

private val GRID_CODE_SIZE = 128.dp
private val LINEAR_CODE_HEIGHT = 72.dp
private const val GRID_MODULE_PIXELS = 10
private const val LINEAR_MODULE_PIXELS = 3
private const val LINEAR_QUIET_ZONE = 10
private const val LINEAR_HEIGHT_MODULES = 30
private const val FILE_NAME = "Grocycode.png"
private const val PNG = "image/png"
