#!/usr/bin/env bash
# Подготовка чистого сервера Ubuntu (DigitalOcean droplet / AWS EC2) к запуску проекта.
# Запуск на сервере: bash scripts/server-setup.sh
# Повторный запуск безопасен.
set -euo pipefail

# 1. Docker Engine + docker compose plugin (официальный скрипт установки)
if ! command -v docker >/dev/null 2>&1; then
    curl -fsSL https://get.docker.com | sudo sh
fi
sudo usermod -aG docker "$USER"
sudo systemctl enable --now docker

# 2. Swap 2 ГБ: на сервере с 1 ГБ RAM сборке Maven внутри Docker не хватает памяти
if ! sudo swapon --show | grep -q /swapfile; then
    sudo fallocate -l 2G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

docker --version || true
echo "Готово. Перелогиньтесь (exit и снова ssh), чтобы docker работал без sudo."
