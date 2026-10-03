package com.patrickzedler.grocy.apps.android.play

import android.app.Application
import com.patrickzedler.grocy.apps.shared.AndroidPlatformDependencies
import com.patrickzedler.grocy.apps.shared.AppGraph

class PlayApplication : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(AndroidPlatformDependencies(this))
    }
}