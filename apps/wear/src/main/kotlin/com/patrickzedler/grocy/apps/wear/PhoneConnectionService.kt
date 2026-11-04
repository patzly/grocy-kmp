package com.patrickzedler.grocy.apps.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.patrickzedler.grocy.core.wear.WEAR_CONNECTION_KEY
import com.patrickzedler.grocy.core.wear.WEAR_CONNECTION_PATH
import kotlinx.coroutines.launch

/** Takes over what the phone published, also while the app is closed. */
class PhoneConnectionService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val graph = (application as WearApplication).graph
        // The buffer is only valid during this call
        val changes = dataEvents
            .filter { it.dataItem.uri.path == WEAR_CONNECTION_PATH }
            .map { event ->
                if (event.type == DataEvent.TYPE_CHANGED) {
                    DataMapItem.fromDataItem(event.dataItem).dataMap.getString(WEAR_CONNECTION_KEY)
                } else {
                    null
                }
            }
        graph.appScope.launch {
            changes.forEach { graph.sync.onPhoneConnection(it) }
        }
    }
}
