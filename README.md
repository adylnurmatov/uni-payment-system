# Crypto Payment Service

Учебный бэкенд-сервис, имитирующий платёжную систему на криптовалюте (тестовый ETH в сети **Ethereum Sepolia**).
Мерчант присылает запрос «спиши с моего клиента N» — сервис проверяет клиента, запрашивает его данные во внешнем
«сервисе ABC», проверяет баланс кошелька, отправляет транзакцию клиент → мерчант и отслеживает её подтверждение.
Есть возвраты, админ-панель на Thymeleaf и Swagger.

> **ВНИМАНИЕ: потеря `WALLET_ENCRYPTION_KEY` = потеря доступа ко всем кошелькам.**
> Приватные ключи кошельков хранятся в БД только в зашифрованном виде (AES-256-GCM), а мастер-ключ — в `.env`.
> На сервере должен быть **тот же ключ**, которым зашифрованы кошельки в его базе. Храните `.env` и бэкапы БД в надёжном месте,
> не коммитьте их и никогда не делайте `docker compose down -v` на базе с деньгами.

## Содержание
- [Стек и архитектура](#стек-и-архитектура)
- [Запуск локально (fake, без сети и монет)](#запуск-локально-fake-без-сети-и-монет)
- [Запуск в Sepolia](#запуск-в-sepolia)
- [Запуск в Docker](#запуск-в-docker)
- [Деплой на сервер (DigitalOcean / AWS EC2)](#деплой-на-сервер-digitalocean--aws-ec2)
- [Примеры curl](#примеры-curl)
- [Переменные окружения](#переменные-окружения)
- [Тесты](#тесты)
- [Частые проблемы](#частые-проблемы)

## Стек и архитектура

Java 21, Spring Boot 3.5, Maven (multi-module), PostgreSQL 16 + Flyway, Spring Data JPA, Thymeleaf, springdoc-openapi,
web3j 4.14, JUnit 5 + Mockito + Testcontainers, Docker.

Гексагональная архитектура, разбитая на Maven-модули (по образцу проекта s4):

```
domain/            модели, статусы, PaymentStateMachine, доменные исключения — без Spring
application/       use case (один модуль на сценарий) + порты (интерфейсы): BlockchainGateway, ClientDataProvider,
                   PrivateKeyCipher, репозитории; общие сервисы WalletService, TreasuryFundingService
infrastructure/    адаптеры: JPA + Flyway, web3j (sepolia), fake-блокчейн, AES-шифрование, HTTP-клиент ABC и mock ABC,
                   REST-контроллеры, Thymeleaf-панель, планировщик подтверждений, Spring Security
boot/              payment-app — точка входа и конфигурация
```

Профили: **`fake`** (по умолчанию) — блокчейн в памяти, сеть и монеты не нужны; **`sepolia`** — настоящая тестовая сеть.
Подробное описание решений и SOLID — в `docs/architecture.md`.

## Запуск локально (fake, без сети и монет)

Нужны **JDK 21**, Maven 3.9+ и Docker.

```bash
cp .env.example .env
# сгенерировать мастер-ключ и вписать в .env → WALLET_ENCRYPTION_KEY=...
openssl rand -base64 32

docker compose up -d postgres         # PostgreSQL на localhost:5433
mvn -q -DskipTests install
mvn -pl boot/payment-app spring-boot:run
```

- Админ-панель: http://localhost:8080/admin (логин `admin`, пароль `ADMIN_PASSWORD`; в профиле fake по умолчанию `admin`)
- Swagger: http://localhost:8080/swagger-ui.html

В fake-режиме казна при старте получает 100 тестовых ETH, транзакции «попадают в блок» через 20 секунд.
Балансы fake-блокчейна живут в памяти и обнуляются после перезапуска.

## Запуск в Sepolia

1. В `.env`:
   ```
   SPRING_PROFILES_ACTIVE=sepolia
   WALLET_ENCRYPTION_KEY=<openssl rand -base64 32>
   ADMIN_PASSWORD=<надёжный пароль>
   ETH_RPC_URL=https://eth-sepolia.g.alchemy.com/v2/<ключ>
   ```
   Публичные RPC (`https://1rpc.io/sepolia`, `https://ethereum-sepolia-rpc.publicnode.com`) быстро упираются в лимит
   запросов (HTTP 429). Бесплатный ключ [Alchemy](https://www.alchemy.com) или [Infura](https://developer.metamask.io)
   делается за пару минут: Create app → Ethereum → Sepolia → скопировать HTTPS-URL.
2. Запустить приложение — при первом старте создаётся **казна**, её адрес есть в логе и на странице `/admin`.
3. **Пополнить казну:** открыть [Google Cloud Sepolia Faucet](https://cloud.google.com/application/web3/faucet/ethereum/sepolia),
   войти в Google-аккаунт, выбрать Ethereum Sepolia, вставить адрес казны → *Receive 0.05 Sepolia ETH*.
   Через 15–30 секунд баланс появится на `/admin` и на etherscan.
4. Дальше всё из админки: создать мерчанта (ему уйдёт 0.005 ETH на комиссии), клиента, пополнить клиента из казны,
   провести тестовую оплату, дождаться CONFIRMED, сделать возврат.

## Запуск в Docker

Приложение и база вместе (настройки берутся из `.env`):

```bash
docker compose up -d --build
docker compose logs -f app       # логи
docker compose down              # остановить (данные сохраняются)
```

`Dockerfile` — multi-stage: Maven собирает jar в первом образе, во второй (JRE) попадает только jar; приложение
работает не от root. PostgreSQL открыт только на `127.0.0.1:5433`, из интернета база недоступна.

Бэкап и восстановление базы:

```bash
scripts/backup-db.sh                                  # → backups/payments-<дата>.sql.gz
scripts/restore-db.sh backups/payments-XXXX.sql.gz    # нужен тот же WALLET_ENCRYPTION_KEY
```

## Деплой на сервер (DigitalOcean / AWS EC2)

Подходит любой сервер **Ubuntu 22.04/24.04** с минимум 1 ГБ RAM (DigitalOcean droplet за $6 или AWS EC2 t2/t3.micro).

### 1. Сервер и доступ
- **DigitalOcean:** Create → Droplets → Ubuntu 24.04, Basic $6, добавить свой SSH-ключ. Пользователь — `root`.
- **AWS EC2:** пользователь — `ubuntu`, вход по `.pem`-ключу. В **Security Group** открыть входящие порты
  `22` (лучше только со своего IP) и `80` (HTTP).

```bash
chmod 600 uni-payment-key.pem
ssh -i uni-payment-key.pem ubuntu@<IP_СЕРВЕРА>
```

### 2. Код и Docker на сервере
```bash
git clone https://github.com/<you>/uni-payment-system.git
cd uni-payment-system
bash scripts/server-setup.sh     # Docker + swap 2 ГБ
exit                             # перелогиниться, чтобы docker работал без sudo
```

### 3. Секреты (с локального компьютера)
`.env` в git нет — его копируют отдельно. На сервере используется **тот же `WALLET_ENCRYPTION_KEY`**, что и для его базы.
```bash
scp -i uni-payment-key.pem .env ubuntu@<IP_СЕРВЕРА>:~/uni-payment-system/.env
```
На сервере в `.env` добавить порт: `APP_PORT=80`, затем `chmod 600 .env`.

### 4. Перенос базы с пополненной казной (если она уже есть локально)
Кошельки (и деньги на них) «живут» в базе. Чтобы не пополнять новую казну заново, перенесите базу:
```bash
# локально
scripts/backup-db.sh
scp -i uni-payment-key.pem backups/payments-XXXX.sql.gz ubuntu@<IP_СЕРВЕРА>:~/uni-payment-system/backups/
# на сервере
scripts/restore-db.sh backups/payments-XXXX.sql.gz
```
После переноса **остановите локальный экземпляр** (`docker compose down`): два приложения с одними кошельками
будут конфликтовать по nonce.

### 5. Запуск
```bash
docker compose up -d --build
docker compose logs -f app       # ждать "Started PaymentApplication" и "Treasury wallet: 0x…"
```
Открыть `http://<IP_СЕРВЕРА>/admin` и `http://<IP_СЕРВЕРА>/swagger-ui.html`.

Обновление после изменений в коде: `git pull && docker compose up -d --build`.

> Сервис отдаётся по HTTP. Для учебного демо этого достаточно, но пароль администратора и API-ключи идут открытым текстом.
> Для настоящего использования поставьте перед приложением reverse-proxy с TLS (например, Caddy + домен).

## Примеры curl

```bash
BASE=http://localhost:8080
ADMIN="-u admin:$ADMIN_PASSWORD"

# Казна
curl $ADMIN $BASE/api/admin/treasury

# Мерчант (API-ключ возвращается ОДИН раз)
curl $ADMIN -H 'Content-Type: application/json' -X POST $BASE/api/admin/merchants -d '{"name":"Coffee Shop"}'

# Клиент с кошельком и пополнение из казны
curl $ADMIN -H 'Content-Type: application/json' -X POST $BASE/api/admin/clients -d '{"externalId":"ext-42"}'
curl $ADMIN -H 'Content-Type: application/json' -X POST $BASE/api/admin/clients/<clientId>/fund -d '{"amount":"0.01"}'

# Оплата (Idempotency-Key обязателен; повтор с тем же ключом и телом вернёт тот же платёж)
curl -H "X-Api-Key: $API_KEY" -H 'Idempotency-Key: order-1001' -H 'Content-Type: application/json' \
     -X POST $BASE/api/v1/payments -d '{"clientId":"ext-42","amount":"0.001"}'

# Статус и список платежей
curl -H "X-Api-Key: $API_KEY" $BASE/api/v1/payments/<paymentId>
curl -H "X-Api-Key: $API_KEY" "$BASE/api/v1/payments?status=CONFIRMED&page=0&size=20"

# Возврат (без тела — весь остаток) и список возвратов
curl -H "X-Api-Key: $API_KEY" -H 'Idempotency-Key: refund-1' -H 'Content-Type: application/json' \
     -X POST $BASE/api/v1/payments/<paymentId>/refunds -d '{"amount":"0.0005"}'
curl -H "X-Api-Key: $API_KEY" $BASE/api/v1/payments/<paymentId>/refunds
```

Формат ошибки:
```json
{ "code": "INSUFFICIENT_FUNDS", "message": "Client balance ... Missing 0.0004 ETH", "paymentId": "...", "status": "FAILED" }
```

Mock «сервиса ABC» (`/mock-abc/clients/{externalId}`): `ext-blocked` — заблокирован, `unknown-*` — 404,
`unavailable-*` — 503, остальные — ACTIVE.

## Переменные окружения

| Переменная | Описание |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `fake` (по умолчанию) или `sepolia` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL (в Docker `DB_URL` подставляется автоматически) |
| `ETH_RPC_URL`, `ETH_CHAIN_ID` | RPC-узел Sepolia и chain id (`11155111`) |
| `WALLET_ENCRYPTION_KEY` | мастер-ключ AES-256 (base64, 32 байта): `openssl rand -base64 32` |
| `ADMIN_PASSWORD` | пароль администратора (`/admin`, `/api/admin`) |
| `ABC_BASE_URL` | адрес «сервиса ABC» (по умолчанию mock в этом же приложении) |
| `MERCHANT_INITIAL_FUNDING` | ETH новому мерчанту на комиссии (sepolia: `0.005`) |
| `CONFIRMATION_TIMEOUT` | через сколько неподтверждённая транзакция считается FAILED (`10m`) |
| `APP_PORT`, `POSTGRES_HOST_PORT` | порты на хосте для Docker (`8080`, `5433`) |

## Тесты

```bash
mvn test
```
Unit-тесты (state machine, шифрование, оплата, возвраты, подтверждения, web3j-адаптер офлайн) работают на fake-блокчейне
и in-memory репозиториях. Интеграционные тесты API и админ-панели поднимают PostgreSQL в Testcontainers
(нужен запущенный Docker, иначе пропускаются). В реальную сеть тесты не ходят.

## Документация для защиты

- [`docs/requirements.md`](docs/requirements.md) — функциональные и нефункциональные требования, ограничения, допущения, коды ошибок
- [`docs/architecture.md`](docs/architecture.md) — архитектура, ключевые решения, SOLID с конкретными классами
- [`docs/diagrams/`](docs/diagrams) — PlantUML: контекст, компоненты, классы, последовательности (оплата, возврат), состояния (платёж, возврат)

Перерисовать диаграммы после правок `.puml`:
```bash
cd docs/diagrams
docker run --rm -v "$PWD:/data" -w /data plantuml/plantuml:1.2025.4 -tpng -charset UTF-8 *.puml
docker run --rm -v "$PWD:/data" -w /data plantuml/plantuml:1.2025.4 -tsvg -charset UTF-8 *.puml
```

## Частые проблемы

| Симптом | Причина / решение |
|---|---|
| `WALLET_ENCRYPTION_KEY is not set` при старте | ключа нет в `.env` / окружении |
| `Failed to decrypt private key` | ключ не тот, которым шифровалась эта база |
| `BLOCKCHAIN_ERROR ... rate limit (HTTP 429)` | публичный RPC ограничивает запросы — используйте ключ Alchemy/Infura |
| `TREASURY_INSUFFICIENT_FUNDS` | пополните казну из крана |
| Maven пишет `release version 21 not supported` | Maven запущен на старом JDK — установите `JAVA_HOME` на JDK 21 |
| Порт 5432 занят | наш Postgres слушает 5433 (`POSTGRES_HOST_PORT`) |
