@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.core.data.update

import com.patrickzedler.grocy.core.model.UpdateCheck
import kotlin.coroutines.cancellation.CancellationException
import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Updates of the web app through its service worker (`sw.js`), which keeps one complete version
 * cached, so the app also starts offline and never mixes files of two versions.
 *
 * A check compares [buildId] with the deployed `precache.json`. A newer build gets its own
 * worker, which downloads that version in the background; installing activates it and reloads.
 * Browsers without service workers (plain http) just reload to get the new version.
 */
class ServiceWorkerAppUpdater(private val buildId: String) : AppUpdater {

    override val checksAutomatically: Boolean = buildId != DEV_BUILD_ID

    override val foregroundEvents: Flow<Unit> = callbackFlow {
        val listener = addVisibleListener { trySend(Unit) }
        awaitClose { removeVisibleListener(listener) }
    }

    override suspend fun check(): UpdateCheck {
        val version = try {
            fetchPrecacheManifest().await<JsString?>()?.toString()?.let(::parseWebAppVersion)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        } ?: return UpdateCheck.Failed

        // Development builds always run what was just built
        val usesWorker = isServiceWorkerSupported() && buildId != DEV_BUILD_ID
        if (version.buildId == buildId) {
            // Caches the running version for offline starts, a no-op once it is installed
            if (usesWorker) installWorker(buildId)
            return UpdateCheck.UpToDate
        }
        if (usesWorker && !installWorker(version.buildId)) return UpdateCheck.Failed
        return UpdateCheck.Available(version.versionName)
    }

    override suspend fun install() {
        if (isServiceWorkerSupported() && buildId != DEV_BUILD_ID) {
            activateWaitingWorker().await<JsAny?>()
        } else {
            reloadPage()
        }
    }

    private suspend fun installWorker(build: String): Boolean =
        try {
            registerWorker(build).await<JsBoolean>().toBoolean()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            false
        }
}

@JsFun("() => 'serviceWorker' in navigator && window.isSecureContext")
private external fun isServiceWorkerSupported(): Boolean

// no-store: the HTTP cache of the host (GitHub Pages: 10 minutes) must not hide a new version
@JsFun("() => fetch('precache.json', { cache: 'no-store' }).then((r) => r.ok ? r.text() : null)")
private external fun fetchPrecacheManifest(): Promise<JsString?>

/**
 * Registers the worker of [build] and resolves to true once it has installed, i.e. cached the
 * complete version, or false if that failed. The build in the URL makes every version a new
 * worker; it refuses to install if the server already has another build.
 */
@JsFun(
    """(build) => navigator.serviceWorker
        .register('sw.js?build=' + encodeURIComponent(build))
        .then((registration) => new Promise((resolve) => {
            const worker = registration.installing || registration.waiting || registration.active;
            if (!worker) {
                resolve(false);
                return;
            }
            const report = () => {
                if (worker.state === 'redundant') resolve(false);
                else if (worker.state !== 'installing') resolve(true);
            };
            worker.addEventListener('statechange', report);
            report();
        }))""",
)
private external fun registerWorker(build: String): Promise<JsBoolean>

/** Lets the waiting worker take over and reloads the page once it controls it. */
@JsFun(
    """() => navigator.serviceWorker.getRegistration().then((registration) => {
        const waiting = registration && registration.waiting;
        if (!waiting) {
            location.reload();
            return;
        }
        navigator.serviceWorker.addEventListener('controllerchange', () => location.reload(), {
            once: true,
        });
        waiting.postMessage('skipWaiting');
    })""",
)
private external fun activateWaitingWorker(): Promise<JsAny?>

@JsFun("() => location.reload()")
private external fun reloadPage()

@JsFun(
    """(callback) => {
        const listener = () => {
            if (document.visibilityState === 'visible') callback();
        };
        document.addEventListener('visibilitychange', listener);
        return listener;
    }""",
)
private external fun addVisibleListener(callback: () -> Unit): JsAny

@JsFun("(listener) => document.removeEventListener('visibilitychange', listener)")
private external fun removeVisibleListener(listener: JsAny)
