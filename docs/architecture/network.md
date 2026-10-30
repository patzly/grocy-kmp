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
