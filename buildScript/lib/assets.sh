#!/usr/bin/env bash

# Downloads the route geo assets and stores them gzip-compressed for bundling.
# The old libcore/cmd/ruleset_generate Go tool (mmdb → geoip.db, geosite source
# build) was removed together with the Go core; sing-geosite/sing-geoip publish
# the same artifacts as release assets, so fetch those instead.

set -euo pipefail

DIR="composeApp/src/commonMain/composeResources/files/sing-box"
GEOIP_REPO="SagerNet/sing-geoip"
GEOSITE_REPO="SagerNet/sing-geosite"

rm -rf "$DIR"
mkdir -p "$DIR"

fetch_latest_tag() {
    curl -fsSL "https://api.github.com/repos/$1/releases/latest" |
        sed -n 's/.*"tag_name": *"\([^"]*\)".*/\1/p' |
        head -1
}

echo "Resolving latest release tags..."
GEOIP_TAG="$(fetch_latest_tag "$GEOIP_REPO")"
GEOSITE_TAG="$(fetch_latest_tag "$GEOSITE_REPO")"

if [ -z "$GEOIP_TAG" ] || [ -z "$GEOSITE_TAG" ]; then
    echo "Failed to resolve release tags" >&2
    exit 1
fi

echo "GEOIP: $GEOIP_REPO $GEOIP_TAG"
echo "GEOSITE: $GEOSITE_REPO $GEOSITE_TAG"

curl -fsSL -o "$DIR/geoip.db" \
    "https://github.com/$GEOIP_REPO/releases/download/$GEOIP_TAG/geoip.db"
curl -fsSL -o "$DIR/geosite.db" \
    "https://github.com/$GEOSITE_REPO/releases/download/$GEOSITE_TAG/geosite.db"

gzip -9 -f "$DIR/geoip.db"
gzip -9 -f "$DIR/geosite.db"

sha256sum "$DIR"/*.db.gz

echo -n "$GEOIP_TAG" >"$DIR/geoip.version.txt"
echo -n "$GEOSITE_TAG" >"$DIR/geosite.version.txt"
