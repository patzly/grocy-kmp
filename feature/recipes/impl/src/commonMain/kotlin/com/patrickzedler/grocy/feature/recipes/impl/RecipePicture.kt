package com.patrickzedler.grocy.feature.recipes.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_image
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource

/**
 * A recipe's picture, cropped to the size [modifier] gives it, in [shape]. Shows the picture
 * icon while loading and when the picture is neither stored nor downloadable.
 */
@Composable
internal fun RecipePicture(
    fileName: String,
    load: suspend (fileName: String) -> ByteArray?,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val image by produceState<ImageBitmap?>(null, fileName) {
        value = load(fileName)?.let { bytes ->
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
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                // The recipe's name tells what it shows
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
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
