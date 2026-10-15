package com.patrickzedler.grocy.apps.android.play

import android.app.Application
import com.patrickzedler.grocy.apps.shared.AndroidPlatformDependencies
import com.patrickzedler.grocy.apps.shared.AppGraph
import com.patrickzedler.grocy.apps.shared.AppGraphOwner
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.scanner.mlkit.MlKitBarcodeDecoderFactory

class PlayApplication : Application(), AppGraphOwner {

    override val graph: AppGraph by lazy {
        AppGraph(
            AndroidPlatformDependencies(
                context = this,
                isPlayVariant = true,
                additionalBarcodeDecoders = mapOf(
                    ScannerEngine.ML_KIT to MlKitBarcodeDecoderFactory,
                ),
            ),
        )
    }
}
