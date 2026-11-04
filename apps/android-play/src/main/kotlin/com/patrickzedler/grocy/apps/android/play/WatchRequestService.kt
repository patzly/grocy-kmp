package com.patrickzedler.grocy.apps.android.play

import com.google.android.gms.wearable.WearableListenerService

/**
 * Exists for its intent filter: a watch that asks for the login starts this process, and
 * [PlayApplication] publishes the login whenever it starts.
 */
class WatchRequestService : WearableListenerService()
