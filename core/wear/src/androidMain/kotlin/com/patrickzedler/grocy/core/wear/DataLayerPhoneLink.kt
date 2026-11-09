package com.patrickzedler.grocy.core.wear

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

/** The watch's side of the Wearable Data Layer; without a phone everything is simply empty. */
class DataLayerPhoneLink(context: Context) : PhoneLink {
    private val appContext = context.applicationContext

    override suspend fun publishedConnection(): String? = published(WEAR_CONNECTION_KEY)

    override suspend fun publishedCertificates(): String? = published(WEAR_CERTIFICATES_KEY)

    private suspend fun published(key: String): String? = try {
        val anyNode = Uri.Builder().scheme("wear").path(WEAR_CONNECTION_PATH).build()
        val items = Wearable.getDataClient(appContext).getDataItems(anyNode).await()
        try {
            items.firstOrNull()?.let { DataMapItem.fromDataItem(it).dataMap.getString(key) }
        } finally {
            items.release()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    override suspend fun requestConnection() {
        try {
            val messages = Wearable.getMessageClient(appContext)
            Wearable.getNodeClient(appContext).connectedNodes.await().forEach { node ->
                messages.sendMessage(node.id, WEAR_REQUEST_CONNECTION_PATH, null).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // No phone in reach, the screen offers the phone's keyboard
        }
    }
}
