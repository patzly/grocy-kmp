// Replaces the service worker of the web app at its old address (GitHub Pages). Browsers look
// for a new version of an installed worker on every start, find this one and run it: it deletes
// the cached app and itself and reloads, so the page that points to the new address shows up.
self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
    event.waitUntil((async () => {
        const names = await caches.keys();
        await Promise.all(names.map((name) => caches.delete(name)));
        await self.registration.unregister();
        // The browser may still keep the former page for a while (GitHub Pages: 10 minutes)
        await Promise.all(['./', 'index.html'].map((page) => fetch(page, { cache: 'reload' })));
        const windows = await self.clients.matchAll({ type: 'window' });
        windows.forEach((window) => window.navigate(window.url));
    })());
});
