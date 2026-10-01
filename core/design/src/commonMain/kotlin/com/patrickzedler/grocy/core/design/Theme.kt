package com.patrickzedler.grocy.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun GrocyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}