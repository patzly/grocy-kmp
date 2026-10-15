package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.ScannerEngine

/** Stored engine name, falling back to the default for missing or unknown values. */
internal fun scannerEngineOf(storedName: String?): ScannerEngine =
    ScannerEngine.entries.firstOrNull { it.name == storedName } ?: ScannerEngine.ZXING
