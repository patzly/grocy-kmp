package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.ic_rounded_add_photo_alternate
import com.patrickzedler.grocy.core.resources.ic_rounded_delete
import com.patrickzedler.grocy.core.resources.product_picture
import com.patrickzedler.grocy.core.resources.product_picture_choose
import com.patrickzedler.grocy.core.resources.product_picture_delete
import com.patrickzedler.grocy.core.resources.product_picture_load_failed
import com.patrickzedler.grocy.core.resources.product_picture_none
import com.patrickzedler.grocy.core.resources.product_picture_will_delete
import com.patrickzedler.grocy.core.resources.product_picture_will_upload
import com.patrickzedler.grocy.core.ui.rememberImagePicker
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Shows the picture of a product or recipe and lets the user replace or delete it. Like in
 * Grocy the change is applied when the object is saved.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PictureDialog(
    savedFileName: String?,
    change: PictureChange,
    loadSavedPicture: suspend () -> ByteArray?,
    onChange: (PictureChange) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val pickImage = rememberImagePicker { image ->
        onChange(PictureChange.Replace(image.bytes, image.fileName))
    }
    var saved by remember { mutableStateOf<PictureLoad>(PictureLoad.Loading) }
    LaunchedEffect(savedFileName) {
        saved = try {
            loadSavedPicture()?.let { PictureLoad.Loaded(it.toImageBitmap()) } ?: PictureLoad.None
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            PictureLoad.Failed
        }
    }
    val shown: PictureLoad = when (change) {
        PictureChange.Keep -> saved
        PictureChange.Delete -> PictureLoad.None
        is PictureChange.Replace -> remember(change) {
            change.bytes.toImageBitmapOrNull()?.let { PictureLoad.Loaded(it) } ?: PictureLoad.Failed
        }
    }
    val hasPicture = shown is PictureLoad.Loaded ||
        (change == PictureChange.Keep && savedFileName != null)

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        ScrollableAlertDialogContent(
            title = { Text(stringResource(Res.string.product_picture)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismissRequest()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PicturePreview(shown)
                when (change) {
                    PictureChange.Delete -> StatusText(Res.string.product_picture_will_delete)
                    is PictureChange.Replace -> StatusText(Res.string.product_picture_will_upload)
                    PictureChange.Keep -> Unit
                }
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    val count = if (hasPicture) 2 else 1
                    ActionItem(
                        text = stringResource(Res.string.product_picture_choose),
                        icon = {
                            Icon(
                                painterResource(Res.drawable.ic_rounded_add_photo_alternate),
                                contentDescription = null,
                            )
                        },
                        index = 0,
                        count = count,
                        onClick = {
                            haptic.confirm()
                            pickImage()
                        },
                    )
                    if (hasPicture) {
                        ActionItem(
                            text = stringResource(Res.string.product_picture_delete),
                            icon = { Icon(painterResource(Res.drawable.ic_rounded_delete), null) },
                            index = 1,
                            count = count,
                            onClick = {
                                haptic.confirm()
                                // A picture chosen but not saved yet is only discarded
                                val change = if (savedFileName != null) {
                                    PictureChange.Delete
                                } else {
                                    PictureChange.Keep
                                }
                                onChange(change)
                            },
                        )
                    }
                }
            }
        }
    }
}

private sealed interface PictureLoad {
    data object Loading : PictureLoad

    data object None : PictureLoad

    data object Failed : PictureLoad

    class Loaded(val image: ImageBitmap) : PictureLoad
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PicturePreview(load: PictureLoad) {
    SegmentedContainer(index = 0, count = 1) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (load) {
                PictureLoad.Loading -> LoadingIndicator()
                is PictureLoad.Loaded -> Image(
                    bitmap = load.image,
                    contentDescription = stringResource(Res.string.product_picture),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(MaterialTheme.shapes.medium),
                )
                PictureLoad.None, PictureLoad.Failed -> Text(
                    text = stringResource(
                        if (load == PictureLoad.None) {
                            Res.string.product_picture_none
                        } else {
                            Res.string.product_picture_load_failed
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun StatusText(text: StringResource) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionItem(
    text: String,
    icon: @Composable () -> Unit,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        modifier = Modifier.clickCursor(),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        leadingContent = { LeadingTrailingContentWrapper { icon() } },
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalResourceApi::class)
private fun ByteArray.toImageBitmap(): ImageBitmap = decodeToImageBitmap()

private fun ByteArray.toImageBitmapOrNull(): ImageBitmap? = try {
    toImageBitmap()
} catch (_: Exception) {
    // Not an image the platform can decode; Grocy may still accept it
    null
}
