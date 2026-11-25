#!/usr/bin/env bash
# Atkuria DB iš atsarginės kopijos (NFR-P4).
#   ./scripts/restore.sh backups/<failas>.dump            -> į bandomąją DB "<DB_NAME>_restore_test"
#   ./scripts/restore.sh backups/<failas>.dump --replace  -> PERRAŠO pagrindinę DB
set -euo pipefail

if [ $# -lt 1 ] || [ ! -f "$1" ]; then
  echo "Naudojimas: $0 backups/<failas>.dump [--replace]" >&2
  exit 1
fi

dump="$1"
mode="${2:-}"
container=korepetavimo-db

main_db=$(docker exec "$container" sh -c 'echo "$POSTGRES_DB"')
if [ "$mode" = "--replace" ]; then
  target="$main_db"
  read -r -p "Bus PERRAŠYTA DB '$target'. Ar aplikacija sustabdyta ir tęsti? (taip/ne) " ans
  [ "$ans" = "taip" ] || { echo "Atšaukta."; exit 1; }
else
  target="${main_db}_restore_test"
fi

echo "==> Kopija: $dump -> DB: $target"
docker cp "$dump" "$container:/tmp/restore.dump"

docker exec -e TARGET="$target" "$container" sh -c '
  set -e
  psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 \
       -c "DROP DATABASE IF EXISTS \"$TARGET\" WITH (FORCE);" \
       -c "CREATE DATABASE \"$TARGET\" OWNER \"$POSTGRES_USER\";"
  pg_restore -U "$POSTGRES_USER" -d "$TARGET" --no-owner /tmp/restore.dump
  rm -f /tmp/restore.dump
  echo "==> Įrašų skaičius atkurtoje DB:"
  psql -U "$POSTGRES_USER" -d "$TARGET" -At -c "
    SELECT '\''users: '\''         || count(*) FROM users
    UNION ALL SELECT '\''tutor_profiles: '\'' || count(*) FROM tutor_profiles
    UNION ALL SELECT '\''lessons: '\''       || count(*) FROM lessons
    UNION ALL SELECT '\''payments: '\''      || count(*) FROM payments
    UNION ALL SELECT '\''messages: '\''      || count(*) FROM messages;"
'
echo "==> Atkūrimas baigtas sėkmingai."
