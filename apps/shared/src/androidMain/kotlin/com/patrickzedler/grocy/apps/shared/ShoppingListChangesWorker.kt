package com.patrickzedler.grocy.apps.shared

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.patrickzedler.grocy.core.data.notifications.ShoppingListPoller
import com.patrickzedler.grocy.core.network.GrocyApiException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Looks for changes of the shopping lists as often as Android allows, every 15 minutes, batched
 * by the system in Doze; only with a network and only while such notifications are on.
 */
internal class WorkManagerShoppingListPoller(private val context: Context) : ShoppingListPoller {

    override fun setActive(active: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (active) {
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ShoppingListChangesWorker>(
                    INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                ).setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                ).build(),
            )
        } else {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }

    private companion object {
        const val WORK_NAME = "shopping_list_changes"
        const val INTERVAL_MINUTES = 15L
    }
}

/** A check costs one small request as long as nothing changed on the server. */
class ShoppingListChangesWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val graph = (applicationContext as? AppGraphOwner)?.graph ?: return Result.success()
        try {
            graph.checkShoppingListChanges()
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // The next run tries again
        }
        return Result.success()
    }
}
