package com.patrickzedler.grocy.core.update.play

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Remembers the activity in front without keeping it alive, for APIs that need one outside of
 * the UI, like the Play update screen. Register it in the Application.
 */
class ResumedActivityTracker : Application.ActivityLifecycleCallbacks {

    private var resumed: WeakReference<Activity>? = null

    val activity: Activity?
        get() = resumed?.get()

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
