# Notifications

The daily notifications per feature of the server. Grocy's web frontend has none, so there is no
page to match; texts and counts follow its messages above its tables (the start screen's
sentences), each opens the feature's page.

- Kinds (`NotificationKind`, `core:model`): stock, chores, tasks, batteries, meal plan, each with
  a channel of its own on Android. A kind's settings are only shown and it only notifies while the
  server has the feature (`isAvailable`). Shopping lists have nothing that gets due.
- Settings (`NotificationSettingsRoute`, category Notifications): on or off, the time, what it
  tells about (`NotificationTopic`: due soon, overdue, expired, below min. stock; overdue, due
  today, due soon; due soon of chores, tasks and batteries off by default), "Show names", "Only what is
  assigned to me" and "Show now". Kept on the device (`SettingsStore`, `stored()` strings, DataStore
  `notification_settings`, `grocy.notificationSettings` on the web).
- `NotificationRepository` (`core:data`) decides. `check()` runs when the alarm fires, at boot,
  when the app starts and when settings, login or server features change; it notifies about what
  `dueNotificationKinds` finds and sets the alarm to `nextNotificationCheck`. All logic works on
  what is stored (`NotificationDeliveries`), so a killed process changes nothing: one delivery per
  kind and day; what was missed (device off, app force stopped) comes on the next check of the same
  day. Turning a kind on or changing its time never fires at once for a time that passed
  (`deliveriesAfterChange`). After a logout everything counts as delivered today.
- "Show now" removes the kind's notification first, because platforms replace one with the same
  tag or id without a sound or popup, which would look as if nothing happened. The item shows a
  loading indicator while it downloads. It says so when there is nothing to tell
  (`ShowNowResult.NOTHING_TO_TELL`) or the platform did not take it (`FAILED`, `Notifier.show`
  returns false).
- Current state: every notification downloads first (`RepositoryNotificationDataSource`, at most
  20 seconds, per kind on its own). If the server cannot be reached it does not wait: it shows what
  is stored, computed for today and marked as last known state, and tries again every 15 minutes,
  8 times, updating it without alerting anew (a dismissed one stays away; one that had nothing to
  tell alerts once something turns up). Nothing to tell shows nothing and removes yesterday's.

## Android

`AndroidNotificationAlarm`: one alarm for all kinds, `setExactAndAllowWhileIdle`, which also fires
in Doze; without the special access "Alarms and reminders" (not granted by default from Android 14
on) `setAndAllowWhileIdle`, which Android may delay. The local time becomes an instant by its
calendar fields (`utcMillisOfLocal`), so a time beyond a clock change stays right.
`NotificationAlarmReceiver` runs the check within `goAsync`, also after `BOOT_COMPLETED`,
`MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` and when exact alarms get allowed; before
Android 13 it applies the app language itself. No WorkManager: it runs at its own discretion, the
alarm plus stored state is what brings a notification on time.

Permissions are asked for when the user turns a kind on or shows one now
(`rememberNotificationAccess`). The page warns only once something is on: notifications blocked,
exact alarms not allowed, battery optimization on (a link to the system's list: asking directly
needs `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, which Google Play restricts). A tap opens
`MainActivity` with the kind as extra (`AppGraph.openNotification`), the page opens on top of what
the user was doing.

## Web

A page cannot be woken at a time: Web Push needs a push server, which neither Grocy nor the static
deployment has, notification triggers were dropped by browsers, and periodic background sync runs
when Chromium likes, for installed apps only, and could not run this code from a service worker.
So `BrowserNotificationAlarm` checks every 30 seconds while the page is open, also in the background
(timers of hidden pages run at least once a minute), and what was missed comes when it opens again;
the settings page says so. `BrowserNotifier` shows them through the service worker
(`showNotification`, also on Android) or, in builds without worker, the page's `Notification`. Their
tag replaces the kind's notification without alerting anew, so two tabs delivering at once alert
once; both read the deliveries from `localStorage` before notifying. A tap reaches `sw.js`
(`notificationclick`): it messages an open page or opens `./?notification=<KIND>`.

## Testing

`NotificationSettingsTest`, `NotificationScheduleTest`, `NotificationSummaryTest` (`core:model`),
`NotificationRepositoryTest` with fakes, `RepositoryNotificationDataSourceTest` on
`FakeGrocyServer`, `NotificationSettingsViewModelTest`, `NotificationAlarmTest` (clock changes, the
manifest). Whether the system really fires an alarm in Doze needs a device.
