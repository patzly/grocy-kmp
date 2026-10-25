package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_image
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource

/**
 * A product's picture as square with small corners, the size of a list item's image. Shows the
 * picture icon while loading and when the picture is neither stored nor downloadable.
 */
@Composable
internal fun ProductThumbnail(
    fileName: String,
    load: suspend (fileName: String) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    ProductThumbnail(key = fileName, load = { load(fileName) }, modifier = modifier)
}

/** [ProductThumbnail] of the image [load] reads, read again when [key] changes. */
@Composable
internal fun ProductThumbnail(
    key: Any,
    load: suspend () -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    val image by produceState<ImageBitmap?>(null, key) {
        value = load()?.let { bytes ->
            try {
                bytes.decodeToImageBitmap()
            } catch (_: Exception) {
                // Not an image Compose can show
                null
            }
        }
    }

    Box(
        modifier = modifier
            .size(ThumbnailSize)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                // The item describes that it has a picture
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(ThumbnailSize),
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_image),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Material's size for the image of a list item
private val ThumbnailSize = 56.dp
