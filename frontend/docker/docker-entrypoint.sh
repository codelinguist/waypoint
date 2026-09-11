#!/bin/sh
# Runs as one of the official nginx image's /docker-entrypoint.d/ scripts
# (executed before nginx starts, per that image's own entrypoint — this
# script must not itself start nginx). Injects the HOUSEHOLD_ID environment
# variable into the served static config.js at container startup, so the
# same built image can point at a different household without a rebuild.
# See src/config.ts and README.md "Local startup" for the full contract.
set -eu

cat > /usr/share/nginx/html/config.js <<EOF
window.__WAYPOINT_CONFIG__ = { householdId: "${HOUSEHOLD_ID:-}" };
EOF
