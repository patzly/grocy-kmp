package com.patrickzedler.grocy.core.design

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable

@Composable
fun GrocyTheme(
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        content = content,
    )
}
