#!/usr/bin/env bash
set -euo pipefail

HOST_IP="203.150.243.87"
HOST_USER="${1:-}"
# Snap Docker-safe path on remote host.
HOST_DIR="${2:-/root/pos}"
DB_DIR="${DB_DIR:-/root/db}"
POS_PORT="${3:-8080}"

if [[ -z "${HOST_USER}" ]]; then
  echo "Usage: $0 <host-user> [host-dir] [pos-port]"
  exit 1
fi

echo "Building pos image..."
docker build -f Dockerfile.pos -t pos:latest .

echo "Exporting pos image archive..."
docker save pos:latest -o pos.tar

echo "Uploading pos.tar to ${HOST_USER}@${HOST_IP}:${HOST_DIR} ..."
ssh "${HOST_USER}@${HOST_IP}" "mkdir -p '${HOST_DIR}'"
scp pos.tar "${HOST_USER}@${HOST_IP}:${HOST_DIR}/"

echo "Loading image and starting container on remote host..."
ssh "${HOST_USER}@${HOST_IP}" "\
mkdir -p '${DB_DIR}'; \
docker load -i '${HOST_DIR}/pos.tar' && \
docker rm -f pos >/dev/null 2>&1 || true; \
docker run -d --name pos --restart always \
  -p '${POS_PORT}:8080' \
  -v '${DB_DIR}:/app/data' \
  pos:latest; \
docker ps --filter name=pos \
"

echo "Done."
