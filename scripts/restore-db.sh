#!/usr/bin/env bash
# Восстановление базы из бэкапа: scripts/restore-db.sh backups/payments-XXXX.sql.gz
# Приложение на время восстановления останавливается. WALLET_ENCRYPTION_KEY в .env должен быть ТЕМ ЖЕ,
# что и там, где делался бэкап, иначе кошельки не расшифруются.
set -euo pipefail
cd "$(dirname "$0")/.."

file=${1:?Usage: scripts/restore-db.sh backups/payments-XXXX.sql.gz}
DB_USER=${DB_USER:-payments}

docker compose up -d postgres
until docker exec payments-postgres pg_isready -U "$DB_USER" -d payments >/dev/null 2>&1; do sleep 1; done

docker compose stop app 2>/dev/null || true
gunzip -c "$file" | docker exec -i payments-postgres psql -q -v ON_ERROR_STOP=1 -U "$DB_USER" -d payments
echo "Restored from $file"
docker compose up -d app
