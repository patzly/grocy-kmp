package com.patrickzedler.grocy.feature.start.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun StartScreen(
  modifier: Modifier = Modifier,
) {
  Scaffold(modifier = modifier) { innerPadding ->
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