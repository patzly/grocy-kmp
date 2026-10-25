package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.patrickzedler.grocy.core.design.withTabularNumbers

@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    isDialog: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (text.isBlank()) return

    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current
    val textColorVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val cardShape = MaterialTheme.shapes.medium

    val blocks = remember(text, isDialog) { parseFormattedText(text, isDialog) }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.fastForEach { block ->
            when (block) {
                is TextBlock.Headline -> {
                    val baseStyle = when (block.level) {
                        1 -> MaterialTheme.typography.headlineLarge
                        2 -> MaterialTheme.typography.headlineMedium
                        3 -> MaterialTheme.typography.headlineSmall
                        4 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    val style = if (block.useTNum) {
                        baseStyle.withTabularNumbers()
                    } else {
                        baseStyle
                    }

                    Text(
                        text = block.annotatedText,
                        style = style,
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { heading() }
                            .padding(bottom = if (block.keepDistance) 16.dp else 0.dp),
                    )
                }

                is TextBlock.Paragraph -> {
                    val style = if (block.isMedium) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.bodyLarge
                    }
                    val color = if (block.isMedium) textColorVariant else textColor

                    Text(
                        text = block.annotatedText,
                        style = style,
                        color = color,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (block.keepDistance) 16.dp else 0.dp),
                    )
                }

                is TextBlock.BulletList -> {
                    val bulletStyle = if (isDialog) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.bodyLarge
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    ) {
                        block.bullets.fastForEachIndexed { index, bullet ->
                            val isLast = index == block.bullets.lastIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {}
                                    .padding(bottom = if (isLast) 0.dp else 8.dp),
                            ) {
                                Text(
                                    text = "•",
                                    style = bulletStyle,
                                    color = textColor,
                                    modifier = Modifier.padding(start = 4.dp, end = 12.dp),
                                )
                                Text(
                                    text = bullet.annotatedText,
                                    style = bulletStyle,
                                    color = textColor,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                is TextBlock.Link -> {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .minimumInteractiveComponentSize()
                            .clip(MaterialTheme.shapes.small)
                            .clickCursor()
                            .clickable(role = Role.Button) {
                                haptic.confirm()
                                uriHandler.tryOpenUri(block.url)
                            }
                            .padding(vertical = 8.dp),
                    )
                }

                is TextBlock.MessageCard -> {
                    val containerColor = if (block.isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                    val contentColor = if (block.isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = containerColor,
                            contentColor = contentColor,
                        ),
                        shape = cardShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    ) {
                        Text(
                            text = block.annotatedText,
                            style = when {
                                block.isSmall -> MaterialTheme.typography.bodySmall
                                block.isMedium -> MaterialTheme.typography.bodyMedium
                                else -> MaterialTheme.typography.bodyLarge
                            },
                            color = contentColor,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                is TextBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 24.dp)
                            .width(56.dp)
                            .align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}
