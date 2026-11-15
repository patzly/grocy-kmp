#!/usr/bin/env sh
# Checks a deployed web app at $1 for what browsers need of it: the page, Wasm with its type,
# caching, compression, the manifest's type and no source maps. $2 is the distribution it serves.
# Used for the Docker image (docker/check.sh) and for every Cloudflare deployment.
set -eu

url="${1%/}"
dist="$2"

fail() {
    echo "check of $url failed: $1" >&2
    exit 1
}

headers() {
    curl -fsS -D - -o /dev/null -H 'Accept-Encoding: gzip' "$url/$1" | tr -d '\r' \
        || fail "$1 is not served"
}
has() {
    printf '%s\n' "$2" | grep -Eqi "^$3" || fail "$1 lacks '$3'"
}

# Asked before every use, so a release shows up: nginx says no-cache, Cloudflare max-age=0
revalidated='cache-control: (no-cache|.*max-age=0)'

page="$(headers '')"
has "the page" "$page" "$revalidated"
has "the page" "$page" "x-content-type-options: nosniff"
has "the page" "$page" "content-security-policy: frame-ancestors 'none'"

hashed="$(ls "$dist" | grep -E '^[0-9a-f]{20}\.wasm$' | head -n 1)"
[ -n "$hashed" ] || fail "no content-hashed .wasm in $dist"
wasm="$(headers "$hashed")"
has "$hashed" "$wasm" "content-type: application/wasm"
has "$hashed" "$wasm" "cache-control: public, max-age=31536000, immutable"
has "$hashed" "$wasm" "content-encoding: gzip"

has "zxing_reader.wasm" "$(headers zxing_reader.wasm)" "$revalidated"
has "the manifest" "$(headers manifest.webmanifest)" "content-type: application/manifest\+json"
has "the service worker" "$(headers sw.js)" "$revalidated"
has "precache.json" "$(headers precache.json)" "$revalidated"

code="$(curl -s -o /dev/null -w '%{http_code}' "$url/grocy.js.map")"
[ "$code" = 404 ] || fail "source maps are served ($code)"

echo "web app at $url ok"
