package com.patrickzedler.grocy.apps.wear

import android.app.Application

/** Holds the one [WearGraph] of the process. */
class WearApplication : Application() {
    val graph: WearGraph by lazy { WearGraph(this) }
}
