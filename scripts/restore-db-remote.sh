#!/usr/bin/env bash
# Восстановление бэкапа в удалённый PostgreSQL (Railway, Render, Neon...) по connection string:
#   scripts/restore-db-remote.sh backups/payments-XXXX.sql.gz 'postgresql://user:pass@host:port/db'
# psql запускается в Docker-контейнере, локально ставить его не нужно.
# Бэкап сделан с --clean --if-exists: существующие таблицы удаляются и создаются заново.
# Приложение, подключённое к этой базе, на время восстановления лучше остановить, а после — перезапустить.
# WALLET_ENCRYPTION_KEY у приложения должен быть ТЕМ ЖЕ, что и там, где делался бэкап.
set -euo pipefail
cd "$(dirname "$0")/.."

file=${1:?Usage: scripts/restore-db-remote.sh backups/payments-XXXX.sql.gz 'postgresql://...'}
url=${2:?Usage: scripts/restore-db-remote.sh backups/payments-XXXX.sql.gz 'postgresql://...'}

gunzip -c "$file" | docker run --rm -i postgres:17-alpine psql -q -v ON_ERROR_STOP=1 "$url"
echo "Restored from $file"
