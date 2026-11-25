#!/usr/bin/env bash
# Sukuria atsarginę DB kopiją dabar. Naudojimas: ./scripts/backup-now.sh
set -euo pipefail

mkdir -p backups
docker exec korepetavimo-db-backup sh -c \
  'f="/backups/${PGDATABASE}_$(date +%Y%m%d_%H%M%S).dump"; pg_dump -Fc -f "$f" && echo "Sukurta: $f"'
