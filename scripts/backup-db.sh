#!/usr/bin/env bash
# Бэкап базы из контейнера payments-postgres в backups/payments-<дата>.sql.gz
# В бэкапе — зашифрованные приватные ключи кошельков. Без WALLET_ENCRYPTION_KEY они бесполезны,
# но храните бэкап так же бережно, как .env.
set -euo pipefail
cd "$(dirname "$0")/.."

DB_USER=${DB_USER:-payments}
mkdir -p backups
file="backups/payments-$(date +%Y%m%d-%H%M%S).sql.gz"

docker exec payments-postgres pg_dump -U "$DB_USER" -d payments --clean --if-exists --no-owner | gzip > "$file"
echo "Backup saved: $file ($(du -h "$file" | cut -f1))"
