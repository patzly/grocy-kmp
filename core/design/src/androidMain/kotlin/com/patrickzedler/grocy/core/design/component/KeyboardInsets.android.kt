package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable

// Follows the system keyboard animation frame by frame, the activity is edge-to-edge
actual val WindowInsets.Companion.keyboard: WindowInsets
    @Composable get() = WindowInsets.ime
