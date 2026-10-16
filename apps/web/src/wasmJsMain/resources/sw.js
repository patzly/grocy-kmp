// Service worker of the Grocy web app. One worker per deployment ("sw.js?build=<id>", registered
// by ServiceWorkerAppUpdater in core:data) caches that complete version, so the app starts
// offline and never mixes files of two versions. Requests to Grocy servers are never touched.

const build = new URL(self.location.href).searchParams.get('build');
const cacheName = `grocy-${build}`;

self.addEventListener('install', (event) => {
    event.waitUntil((async () => {
        const response = await fetch('precache.json', { cache: 'no-store' });
        const manifest = await response.json();
        // The server has another version by now, the worker of that one caches it
        if (manifest.buildId !== build) {
            throw new Error(`Server has build ${manifest.buildId}, not ${build}`);
        }
        const cache = await caches.open(cacheName);
        // no-cache revalidates, so files the browser already has are not downloaded again
        await cache.addAll(manifest.files.map((file) => new Request(file, { cache: 'no-cache' })));
    })());
});

self.addEventListener('activate', (event) => {
    event.waitUntil((async () => {
        const names = await caches.keys();
        await Promise.all(
            names
                .filter((name) => name.startsWith('grocy-') && name !== cacheName)
                .map((name) => caches.delete(name)),
        );
        await self.clients.claim();
    })());
});

// The app sends this when the user chose to update now
self.addEventListener('message', (event) => {
    if (event.data === 'skipWaiting') self.skipWaiting();
});

self.addEventListener('fetch', (event) => {
    const request = event.request;
    if (request.method !== 'GET') return;
    const url = new URL(request.url);
    if (url.origin !== self.location.origin) return;
    // The update check must always reach the server
    if (url.pathname.endsWith('/precache.json') || url.pathname.endsWith('/sw.js')) return;

    event.respondWith((async () => {
        const cache = await caches.open(cacheName);
        const cached = request.mode === 'navigate'
            ? await cache.match('index.html')
            : await cache.match(request, { ignoreSearch: true });
        return cached ?? fetch(request);
    })());
});
