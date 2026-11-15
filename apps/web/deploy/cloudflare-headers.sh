#!/usr/bin/env sh
# Writes the _headers file of Cloudflare Pages for the distribution in $1: the headers of
# docker/nginx.conf. Pages already asks the server before every use of a file (max-age=0), only
# the files named after their content are kept for a year. Pages joins a header set by two
# rules, so the content-hashed files are listed one by one instead of a pattern for all .wasm.
set -eu

dist="$1"

cat <<'HEADERS'
/*
  X-Content-Type-Options: nosniff
  Referrer-Policy: no-referrer
  Content-Security-Policy: frame-ancestors 'none'
HEADERS

ls "$dist" | grep -E '^[0-9a-f]{20}\.(wasm|js)$' | while read -r file; do
    printf '\n/%s\n  Cache-Control: public, max-age=31536000, immutable\n' "$file"
done
