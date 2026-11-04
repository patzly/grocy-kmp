package com.patrickzedler.grocy.apps.android.play

import android.app.Application
import com.patrickzedler.grocy.apps.shared.AndroidPlatformDependencies
import com.patrickzedler.grocy.apps.shared.AppGraph
import com.patrickzedler.grocy.apps.shared.AppGraphOwner
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.scanner.mlkit.MlKitBarcodeDecoderFactory
import com.patrickzedler.grocy.core.update.play.PlayAppUpdater
import com.patrickzedler.grocy.core.update.play.ResumedActivityTracker
import com.patrickzedler.grocy.core.wear.PhoneConnectionPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PlayApplication : Application(), AppGraphOwner {

    // The Play update screen starts from the activity in front
    private val activityTracker = ResumedActivityTracker()

    private val publisherScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        // Zero click: whoever is logged in here is logged in on the paired watch too
        PhoneConnectionPublisher(this, graph.connection, publisherScope).start()
    }
}
