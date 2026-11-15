#!/usr/bin/env sh
# Builds the web app image from the distribution, runs it and checks it with
# deploy/check-site.sh. Run after :apps:web:wasmJsBrowserDistribution.
# ENGINE=podman for Podman; PORT if 18080 is taken.
set -eu

engine="${ENGINE:-docker}"
port="${PORT:-18080}"
web="$(cd "$(dirname "$0")/.." && pwd)"
image=grocy-kmp-web:check
container=grocy-kmp-web-check

"$engine" build -q -t "$image" -f "$web/docker/Dockerfile" "$web" >/dev/null
"$engine" rm -f "$container" >/dev/null 2>&1 || true
"$engine" run -d --name "$container" -p "127.0.0.1:$port:8080" "$image" >/dev/null
trap '"$engine" rm -f "$container" >/dev/null 2>&1 || true' EXIT

i=0
until curl -fs -o /dev/null "http://127.0.0.1:$port/"; do
    i=$((i + 1))
    if [ "$i" -ge 40 ]; then
        echo "the image's server did not answer" >&2
        exit 1
    fi
    sleep 0.25
done

sh "$web/deploy/check-site.sh" "http://127.0.0.1:$port" "$web/build/dist/wasmJs/productionExecutable"
