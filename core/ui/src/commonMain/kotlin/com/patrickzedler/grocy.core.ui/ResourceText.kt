package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.patrickzedler.grocy.core.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Text of a file in composeResources/files, null while it is being read. */
@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun rememberResourceText(path: String): String? =
    produceState<String?>(initialValue = null, path) {
        value = Res.readBytes(path).decodeToString().trimEnd()
    }.value
