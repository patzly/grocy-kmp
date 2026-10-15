package com.patrickzedler.grocy.feature.start.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.TooltipPosition
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.app_name
import com.patrickzedler.grocy.core.resources.ic_rounded_info
import com.patrickzedler.grocy.core.resources.title_about
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StartScreen(
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = { AboutButton(onClick = onAboutClick) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Grocy KMP Start Screen",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun AboutButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val description = stringResource(Res.string.title_about)

    HapticTooltipBox(
        text = description,
        position = TooltipPosition.Below,
    ) {
        IconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_info),
                contentDescription = description,
            )
        }
    }
}

@Preview
@Composable
private fun StartScreenPreview() {
    GrocyTheme {
        StartScreen(onAboutClick = {})
    }
}
