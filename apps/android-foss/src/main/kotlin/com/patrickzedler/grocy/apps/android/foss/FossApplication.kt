package com.patrickzedler.grocy.apps.android.foss

import android.app.Application
import com.patrickzedler.grocy.apps.shared.AndroidPlatformDependencies
import com.patrickzedler.grocy.apps.shared.AppGraph

class FossApplication : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(AndroidPlatformDependencies(this))
    }
}