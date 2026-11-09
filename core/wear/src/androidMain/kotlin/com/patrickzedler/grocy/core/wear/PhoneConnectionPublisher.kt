package com.patrickzedler.grocy.core.wear

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.patrickzedler.grocy.core.data.credentials.ServerConnectionCodec
import com.patrickzedler.grocy.core.data.credentials.TrustedCertificatesCodec
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Keeps the phone's login in the Wearable Data Layer, which syncs it to the watch app over the
 * encrypted link of the pairing, readable only by an app with this one's ID and signature.
 */
class PhoneConnectionPublisher(
    context: Context,
    private val connection: Flow<ServerConnection?>,
    private val trustedCertificates: Flow<List<TrustedCertificate>>,
    private val scope: CoroutineScope,
) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)

    fun start() {
        scope.launch {
            combine(connection, trustedCertificates) { current, trusted ->
                current to current?.let { trustedFor(it, trusted) }.orEmpty()
            }.distinctUntilChanged().collect { (current, trusted) ->
                try {
                    if (current != null) publish(current, trusted) else unpublish()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Phones without Google's Wear OS app have no Data Layer, nothing to do
                }
            }
        }
    }

    private suspend fun publish(connection: ServerConnection, trusted: List<TrustedCertificate>) {
        val request = PutDataMapRequest.create(WEAR_CONNECTION_PATH).apply {
            dataMap.putString(WEAR_CONNECTION_KEY, ServerConnectionCodec.encode(connection))
            dataMap.putString(WEAR_CERTIFICATES_KEY, TrustedCertificatesCodec.encode(trusted))
        }
        dataClient.putDataItem(request.asPutDataRequest().setUrgent()).await()
    }

    private suspend fun unpublish() {
        val anyNode = Uri.Builder().scheme("wear").path(WEAR_CONNECTION_PATH).build()
        dataClient.deleteDataItems(anyNode).await()
    }
}
