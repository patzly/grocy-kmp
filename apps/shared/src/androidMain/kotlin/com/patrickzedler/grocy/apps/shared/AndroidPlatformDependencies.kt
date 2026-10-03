package com.patrickzedler.grocy.apps.shared

import android.content.Context

class AndroidPlatformDependencies(
    context: Context,
) : PlatformDependencies {

    // Application context only, an Activity context would leak on rotation
    val context: Context = context.applicationContext
}
