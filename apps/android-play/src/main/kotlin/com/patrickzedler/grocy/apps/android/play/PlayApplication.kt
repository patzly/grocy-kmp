package com.patrickzedler.grocy.apps.android.play

import android.app.Application
import com.patrickzedler.grocy.apps.shared.AndroidPlatformDependencies
import com.patrickzedler.grocy.apps.shared.AppGraph
import com.patrickzedler.grocy.apps.shared.AppGraphOwner
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.scanner.mlkit.MlKitBarcodeDecoderFactory
import com.patrickzedler.grocy.core.update.play.PlayAppUpdater
import com.patrickzedler.grocy.core.update.play.ResumedActivityTracker

class PlayApplication : Application(), AppGraphOwner {

    // The Play update screen starts from the activity in front
    private val activityTracker = ResumedActivityTracker()

    override val graph: AppGraph by lazy {
        AppGraph(
            AndroidPlatformDependencies(
                context = this,
                isPlayVariant = true,
                additionalBarcodeDecoders = mapOf(
                    ScannerEngine.ML_KIT to MlKitBarcodeDecoderFactory,
                ),
                appUpdater = { gitHubUpdater ->
                    PlayAppUpdater(
                        context = this,
                        currentActivity = { activityTracker.activity },
                        fallback = gitHubUpdater,
                    )
                },
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityTracker)
    }
}
