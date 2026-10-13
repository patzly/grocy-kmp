package com.patrickzedler.grocy.apps.android.play

import android.app.Application
import com.patrickzedler.grocy.apps.shared.AndroidPlatformDependencies
import com.patrickzedler.grocy.apps.shared.AppGraph
import com.patrickzedler.grocy.apps.shared.AppGraphOwner

class PlayApplication : Application(), AppGraphOwner {

    override val graph: AppGraph by lazy {
        AppGraph(AndroidPlatformDependencies(this, isPlayVariant = true))
    }
}
