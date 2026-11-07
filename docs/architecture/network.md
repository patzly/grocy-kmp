# Network details

API key placement and Home Assistant ingress. Moved from CLAUDE.md.

- API key placement (`sendApiKeyAsQueryParameter`, expect/actual): the `GROCY-API-KEY` header
  on Android, a query parameter of the same name in browsers. Grocy checks the key before its
  CORS middleware, so the preflight for a custom header fails with a 401 without CORS headers;
  with the query parameter GET requests need no preflight. Consequence in browsers: a wrong key
  looks like an unreachable server, because the 401 carries no CORS headers either.
- Home Assistant ingress (`ServerConnection.HomeAssistant`, Android only,
  `isHomeAssistantIngressSupported`): `WebSocketIngressSessionSource` authenticates on
  `/api/websocket` with the long-lived token, reads `ingress_entry` of the add-on
  (`supervisor/api` `/addons/<slug>/info`, default slug `a0d7b954_grocy`) and creates a session
  (`/ingress/session`); the REST proxy does not allow these endpoints. Requests then go to
  `<HA URL><ingress_entry>/api/...` with the cookie `ingress_session` plus the Grocy API key.
  `HomeAssistantIngress` reuses a session for 10 minutes after its last use (the supervisor ends
  it after 15) and `GrocyApi` retries a 401 once with a fresh session. Slug and ingress path are
  validated, so a server answer can never redirect the API key to another host.
- Browsers cannot use ingress at all (no CORS on ingress, session only as cookie, `Cookie` is a
  forbidden header); the web login explains the add-on's network port as alternative.

## Client and errors

- One `HttpClient` per process (`createGrocyHttpClient(proxy)`), OkHttp on Android, Js engine on
  Wasm, JSON with `ignoreUnknownKeys = true`. Every request of the app goes through it (Grocy,
  Home Assistant's WebSocket, product databases, GitHub), so the proxy below covers them all;
  only what Google Play Services download themselves (Play updates, ML Kit) and links opened in
  another app do not.

## Proxy and Tor (Android)

- `NetworkSettingsRepository.proxy()` gives the `NetworkProxy` of the process: the settings it
  started with, read once (`ProxyRouting`, OkHttp's `ProxySelector` and an interceptor). A
  change needs a restart on purpose: OkHttp keeps connections in its pool, also those in use
  during a change, and one opened before it would carry later requests past the new proxy.
  Requests at app start wait for the stored settings, so none leaves before they are known.
- Tor: Orbot's SOCKS port `127.0.0.1:9050`. OkHttp hands SOCKS proxies the unresolved name, so
  Tor resolves it (also `.onion`) and no DNS query leaves the device; its HTTP tunnel port would
  not do, it only knows CONNECT, which plain `http://` onion services do not use. Connect and
  request timeouts are longer under Tor (`TOR_*_TIMEOUT_MILLIS`). The app asks Orbot to start
  (`requestTorStart`, Orbot's `START` broadcast, `<queries>` for its package) at every start;
  Orbot does if the user allows background starts there.
- HTTP proxy: `Proxy.Type.HTTP` with the unresolved host; HTTPS goes through CONNECT, so the
  proxy sees only the server's name, plain HTTP servers it sees completely.
- Fail closed: OkHttp only tries the proxies the selector returns, never a direct connection,
  so Tor without a running Orbot fails as unreachable. An HTTP proxy that is on without a valid
  host blocks every request (`NetworkProxy.Blocked`, `ProxyBlockedException`) before anything
  connects. Without either, the system's proxy applies as before.
- `ProxyRoutingTest` runs real sockets on the loopback device: a fake HTTP proxy, a fake SOCKS5
  server that checks it gets the name, a closed Tor port and a blocked proxy that must not reach
  a direct server.
- `GrocyApi` methods take the `ServerConnection` per call. `ServerConnection.serverUrl` is the
  base URL without `/api` and without trailing slash; build it with `ServerUrl.normalize`, which
  only accepts http(s) URLs with a real host and no whitespace.
- Error handling: transport errors -> `Unreachable` (catches `Throwable`, Wasm fetch failures
  arrive as `Error`), 401 -> `Unauthorized`, other non-2xx -> `HttpError`, parse failures ->
  `NotGrocyServer`; Home Assistant: `HomeAssistantTokenRejected`, `AddonNotFound`,
  `NotHomeAssistant`.
- DTOs are `internal` in `core.network.dto` with a `toModel()` mapping into `core:model`.
- Android allows cleartext HTTP and user CAs on purpose (self-hosted servers), see
  `network_security_config.xml`.
