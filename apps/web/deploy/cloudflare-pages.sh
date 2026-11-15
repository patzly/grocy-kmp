#!/usr/bin/env sh
# Deploys the web app distribution $1 to the Cloudflare Pages project $2 and checks the
# deployment with check-site.sh. Needs CLOUDFLARE_API_TOKEN and CLOUDFLARE_ACCOUNT_ID.
set -eu

dist="$1"
project="$2"
deploy="$(cd "$(dirname "$0")" && pwd)"
site="$(mktemp -d)"
log="$(mktemp)"
trap 'rm -rf "$site" "$log"' EXIT

cp -R "$dist/." "$site/"
# Source maps help no one using the app
find "$site" -name '*.map' -delete
cp "$deploy/404.html" "$site/404.html"
sh "$deploy/cloudflare-headers.sh" "$site" > "$site/_headers"

# A failed upload must fail the job, which a pipe into tee would hide
export WRANGLER_SEND_METRICS=false
status=0
npx --yes wrangler@4.156.0 pages deploy "$site" \
    --project-name="$project" --branch=main --commit-dirty=true > "$log" 2>&1 || status=$?
cat "$log"
[ "$status" -eq 0 ] || exit "$status"

# The project's subdomain is its name only while that was free, so any *.pages.dev counts
url="$(grep -Eo 'https://[a-z0-9-]+\.[a-z0-9-]+\.pages\.dev' "$log" | head -n 1)"
[ -n "$url" ] || { echo "no deployment URL in wrangler's output" >&2; exit 1; }

# A new deployment can take a moment to answer everywhere
i=0
until curl -fs -o /dev/null "$url/"; do
    i=$((i + 1))
    [ "$i" -lt 30 ] || { echo "$url does not answer" >&2; exit 1; }
    sleep 2
done
sh "$deploy/check-site.sh" "$url" "$site"
