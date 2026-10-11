package com.patrickzedler.grocy.apps.android.foss

import android.app.Application
import com.patrickzedler.grocy.apps.shared.AndroidPlatformDependencies
import com.patrickzedler.grocy.apps.shared.AppGraph
import com.patrickzedler.grocy.apps.shared.AppGraphOwner

class FossApplication : Application(), AppGraphOwner {

    override val graph: AppGraph by lazy {
        AppGraph(AndroidPlatformDependencies(this))
    }
}
