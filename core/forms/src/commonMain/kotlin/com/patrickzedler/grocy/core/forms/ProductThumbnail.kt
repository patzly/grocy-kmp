package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_image
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource

/**
 * A product's picture in one of Material's expressive shapes, the size of a list item's image;
 * the same picture always gets the same shape. Shows the picture icon while loading and when the
 * picture is neither stored nor downloadable.
 */
@Composable
fun ProductThumbnail(
    fileName: String,
    load: suspend (fileName: String) -> ByteArray?,
    modifier: Modifier = Modifier,
) {
    ProductThumbnail(key = fileName, load = { load(fileName) }, modifier = modifier)
}

/** [ProductThumbnail] of the image [load] reads, read again when [key] changes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProductThumbnail(
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
            .clip(ThumbnailShapes[thumbnailShapeIndex(key)].toShape())
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

/**
 * Material's expressive shapes on a square base, so every thumbnail is as high as the others.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val ThumbnailShapes: List<RoundedPolygon> = with(MaterialShapes) {
    listOf(
        Square, Slanted, Arch, Pill, Arrow, Fan, Pentagon, Gem, VerySunny, Sunny,
        Cookie4Sided, Cookie6Sided, Cookie7Sided, Cookie9Sided, Cookie12Sided,
        Clover4Leaf, Clover8Leaf, Burst, SoftBurst, SoftBoom, Flower, PuffyDiamond, Ghostish, Bun,
    )
}

/**
 * The shape of the thumbnail [key] names: a file name's hash computed here, so it is the same on
 * every platform and after every start.
 */
internal fun thumbnailShapeIndex(key: Any): Int {
    val hash = if (key is String) {
        key.fold(0) { hash, char -> 31 * hash + char.code }
    } else {
        key.hashCode()
    }
    return hash.mod(ThumbnailShapes.size)
}
