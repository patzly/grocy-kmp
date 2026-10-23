package com.patrickzedler.grocy.core.design

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Chromebooks and other Android PCs come with a trackpad
@Composable
internal actual fun prefersMouse(): Boolean =
    LocalContext.current.packageManager.hasSystemFeature(PackageManager.FEATURE_PC)
