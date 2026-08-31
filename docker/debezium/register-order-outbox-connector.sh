#!/usr/bin/env bash
set -euo pipefail

curl -sS -X PUT \
  -H "Content-Type: application/json" \
  --data @docker/debezium/connectors/order-outbox.json \
  http://localhost:8085/connectors/order-outbox-connector/config
