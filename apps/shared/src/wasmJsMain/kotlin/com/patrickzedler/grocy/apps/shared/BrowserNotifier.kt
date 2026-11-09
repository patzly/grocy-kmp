@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.notifications.NotificationAlarm
import com.patrickzedler.grocy.core.data.notifications.Notifier
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.data.notifications.ShoppingListPoller
import com.patrickzedler.grocy.core.model.notifications.NotificationSummary
import com.patrickzedler.grocy.core.model.notifications.ShoppingListChangeNames
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListRoute
import kotlin.coroutines.cancellation.CancellationException
import kotlin.js.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Notifications of the service worker, which also work on Android and reach the app when tapped;
 * builds without worker (local ones) use the page's. The tag replaces the one of its kind without
 * alerting anew, so two tabs delivering it at the same moment alert only once.
 */
internal class BrowserNotifier : Notifier {

    override suspend fun show(summary: NotificationSummary, silently: Boolean): Boolean {
        val text = notificationTextOf(summary)
        val body = listOfNotNull(text.details, text.note).joinToString("\n")
        return showBrowserNotification(text.title, body, summary.kind.name)
            .await<JsBoolean>()
            .toBoolean()
    }

    override suspend fun cancel(kind: NotificationKind) {
        closeBrowserNotifications(kind.name).await<JsAny?>()
    }

    override suspend fun isShowing(kind: NotificationKind): Boolean =
        isBrowserNotificationShown(kind.name).await<JsBoolean>().toBoolean()

    override suspend fun showShoppingListChanges(names: ShoppingListChangeNames): Boolean {
        val body = shoppingListNotificationLines(names).joinToString("\n")
        return showBrowserNotification(shoppingListNotificationTitle(), body, SHOPPING_LIST_TAG)
            .await<JsBoolean>()
            .toBoolean()
    }

    override suspend fun cancelShoppingListChanges() {
        closeBrowserNotifications(SHOPPING_LIST_TAG).await<JsAny?>()
    }

    override suspend fun isShowingShoppingListChanges(): Boolean =
        isBrowserNotificationShown(SHOPPING_LIST_TAG).await<JsBoolean>().toBoolean()
}

// Also what a tap on it tells the app, in the place of a kind's name
private const val SHOPPING_LIST_TAG = "SHOPPING_LIST"

/** While the page is open, also in the background: every minute one small request. */
internal class BrowserShoppingListPoller(
    private val scope: CoroutineScope,
    private val onDue: suspend () -> Unit,
) : ShoppingListPoller {

    private var job: Job? = null

    override fun setActive(active: Boolean) {
        job?.cancel()
        job = null
        if (!active) return
        job = scope.launch {
            while (true) {
                delay(CHECK_INTERVAL_MILLIS)
                try {
                    onDue()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: GrocyApiException) {
                    // The next round tries again
                }
            }
        }
    }

    private companion object {
        const val CHECK_INTERVAL_MILLIS = 60_000L
    }
}

/**
 * A page cannot be woken by the browser at a time: Web Push needs a server of its own and browsers
 * dropped notification triggers. So the open page, also in the background, looks every half minute
 * what is due; timers of hidden pages run at least once a minute. Reading from the storage costs
 * nothing, and what was missed while it was closed comes when it opens again.
 */
internal class BrowserNotificationAlarm(
    private val scope: CoroutineScope,
    private val onDue: suspend () -> Unit,
) : NotificationAlarm {

    private var job: Job? = null

    override fun set(localMillis: Long) {
        if (job != null) return
        job = scope.launch {
            while (true) {
                delay(CHECK_INTERVAL_MILLIS)
                onDue()
            }
        }
    }

    override fun cancel() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val CHECK_INTERVAL_MILLIS = 30_000L
    }
}

/**
 * Tells [graph] about tapped notifications: of an open page by a message of the service worker, or
 * the address it opened a closed app with, which then returns to the plain one.
 */
fun observeNotificationClicks(graph: AppGraph) {
    fun open(name: String?) {
        if (name == SHOPPING_LIST_TAG) {
            graph.openRoute(ShoppingListRoute())
            return
        }
        NotificationKind.entries.firstOrNull { it.name == name }?.let(graph::openNotification)
    }
    addNotificationClickListener { open(it.toString()) }
    open(takeNotificationOfAddress()?.toString())
}

@JsFun(
    """(title, body, kind) => {
        if (typeof Notification === 'undefined' || Notification.permission !== 'granted') {
            return Promise.resolve(false);
        }
        const options = { body, tag: kind, icon: 'icons/icon-192.png', data: { kind } };
        const page = () => {
            // Throws on Android, whose browsers only let workers notify
            try {
                const notification = new Notification(title, options);
                notification.onclick = () => {
                    window.focus();
                    notification.close();
                    window.dispatchEvent(new CustomEvent('grocy-notification', { detail: kind }));
                };
                (window.grocyNotifications ??= {})[kind] = notification;
                notification.onclose = () => {
                    if (window.grocyNotifications[kind] === notification) {
                        delete window.grocyNotifications[kind];
                    }
                };
                return true;
            } catch {
                return false;
            }
        };
        if (!('serviceWorker' in navigator)) return Promise.resolve(page());
        return navigator.serviceWorker.getRegistration().then((registration) => {
            if (registration?.active) {
                return registration.showNotification(title, options).then(() => true);
            }
            return page();
        }).catch(() => false);
    }""",
)
private external fun showBrowserNotification(
    title: String,
    body: String,
    kind: String,
): Promise<JsBoolean>

@JsFun(
    """(kind) => {
        window.grocyNotifications?.[kind]?.close();
        if (!('serviceWorker' in navigator)) return Promise.resolve(null);
        return navigator.serviceWorker.getRegistration()
            .then((registration) => registration?.getNotifications({ tag: kind }) ?? [])
            .then((notifications) => { notifications.forEach((it) => it.close()); return null; })
            .catch(() => null);
    }""",
)
private external fun closeBrowserNotifications(kind: String): Promise<JsAny?>

@JsFun(
    """(kind) => {
        if (window.grocyNotifications?.[kind]) return Promise.resolve(true);
        if (!('serviceWorker' in navigator)) return Promise.resolve(false);
        return navigator.serviceWorker.getRegistration()
            .then((registration) => registration?.getNotifications({ tag: kind }) ?? [])
            .then((notifications) => notifications.length > 0)
            .catch(() => false);
    }""",
)
private external fun isBrowserNotificationShown(kind: String): Promise<JsBoolean>

@JsFun(
    """(callback) => {
        window.addEventListener('grocy-notification', (event) => callback(event.detail));
        navigator.serviceWorker?.addEventListener('message', (event) => {
            if (event.data?.notificationKind) callback(event.data.notificationKind);
        });
    }""",
)
private external fun addNotificationClickListener(callback: (JsString) -> Unit)

@JsFun(
    """() => {
        const url = new URL(location.href);
        const kind = url.searchParams.get('notification');
        if (kind === null) return null;
        url.searchParams.delete('notification');
        history.replaceState(history.state, '', url);
        return kind;
    }""",
)
private external fun takeNotificationOfAddress(): JsString?
