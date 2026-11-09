# ТЗ: Crypto Payment Service (учебный проект)

Документ для Claude Code. Прочитай целиком, потом выполняй **по этапам** (раздел 12). После каждого этапа останавливайся, коротко отчитывайся и жди подтверждения.

---

## 1. Суть проекта

Бэкенд-сервис, имитирующий платёжную систему на криптовалюте. К нему обращаются другие сервисы (мерчанты) с запросом «списать с моего клиента N».

Основной сценарий:
1. Мерчант присылает запрос на оплату: `clientId`, сумма.
2. Сервис проверяет, есть ли такой клиент и есть ли у него кошелёк.
3. Сервис запрашивает данные клиента у внешнего «сервиса ABC».
4. Сервис проверяет баланс кошелька клиента (сумма + комиссия сети).
5. Сервис отправляет транзакцию: с кошелька клиента на кошелёк мерчанта.
6. Сервис возвращает мерчанту ответ (статус, хэш транзакции), затем отслеживает подтверждение.

Плюс **возврат** (refund) по подтверждённому платежу.

Это учебный проект: на мидтерме его будут защищать (вопросы по архитектуре и SOLID). Потом проект будут развивать до финала (добавлять новые интерфейсы/подходы), поэтому код должен быть расширяемым, а не «одноразовым».

## 2. Стек

- Java 21
- Spring Boot 3.5.x
- Maven
- PostgreSQL 16, миграции через **Flyway**
- Spring Data JPA
- Thymeleaf (админ-панель для демонстрации)
- springdoc-openapi (Swagger UI): на защите запросы будут кидать через него
- **web3j** (`org.web3j:core`, Maven Central): генерация кошельков, баланс, отправка транзакций, статус
- Тесты: JUnit 5, Mockito; интеграционные — Testcontainers (Postgres)
- Docker + docker-compose (app + postgres) для деплоя на DigitalOcean

Никаких внешних провайдеров кошельков и платных сервисов.

## 3. Блокчейн

Сеть: **Ethereum Sepolia** (публичная тестовая сеть, монеты ничего не стоят). Валюта платежей: **тестовый ETH**.

| Параметр | Значение |
|---|---|
| Chain ID | `11155111` |
| RPC URL | из конфига (`ETH_RPC_URL`): публичный узел, например `https://ethereum-sepolia-rpc.publicnode.com`, или бесплатный ключ Infura/Alchemy |
| Обозреватель | `https://sepolia.etherscan.io/tx/{hash}`, `https://sepolia.etherscan.io/address/{address}` |
| Время блока | ~12 секунд |

Учитывай, что публичный RPC может быть медленным или ограничивать частоту запросов: таймауты, понятные ошибки, планировщик не чаще раза в 10–15 секунд.

### 3.1. Модель кошельков (кастодиальная)

- Кошельки **генерирует сам сервис** (web3j `Keys.createEcKeyPair()`), адрес — `"0x" + Keys.getAddress(keyPair)`.
- Приватный ключ хранится в БД **только в зашифрованном виде**: AES-256-GCM, мастер-ключ из переменной окружения `WALLET_ENCRYPTION_KEY` (base64, 32 байта). Шифрование — в отдельном классе (`PrivateKeyCipher`). Приватные ключи **никогда** не логируются, не отдаются через API и не показываются в UI.
- Три типа кошельков:
  - **Treasury (казна)** — один на систему. Создаётся автоматически при первом старте, если его нет. Пользователь вручную пополняет его тестовым ETH из крана (Google Cloud Sepolia faucet). Из казны админка раздаёт ETH клиентам и мерчантам. Это решает проблему лимитов крана: кран нужен только казне.
  - **Client** — кошелёк клиента, с него списываются платежи.
  - **Merchant** — кошелёк мерчанта, на него приходят платежи, с него уходят возвраты (поэтому при создании мерчанта ему переводится немного ETH из казны на комиссии).

### 3.2. Подсказки по web3j

```java
Web3j web3 = Web3j.build(new HttpService(rpcUrl));

// баланс
BigInteger wei = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();

// оценка комиссии простого перевода: 21000 газа * gasPrice (+ запас, например 20%)
BigInteger gasPrice = web3.ethGasPrice().send().getGasPrice();

// отправка БЕЗ ожидания блока (чтобы был честный статус PENDING)
RawTransactionManager tm = new RawTransactionManager(web3, Credentials.create(privateKeyHex), chainId);
EthSendTransaction res = tm.sendTransaction(gasPrice, BigInteger.valueOf(21_000), toAddress, "", valueWei);
// res.hasError() -> ошибка; иначе res.getTransactionHash()

// статус
Optional<TransactionReceipt> r = web3.ethGetTransactionReceipt(hash).send().getTransactionReceipt();
// r.get().isStatusOK()
```

Не используй блокирующий `Transfer.sendFunds(...).send()` в обработке запроса: он ждёт блок. Подтверждение отслеживает планировщик (раздел 7).

Важно — **nonce**: две транзакции с одного кошелька одновременно получат одинаковый nonce, и одна упадёт. Отправки с одного адреса должны идти последовательно (блокировка по адресу кошелька, например `SELECT ... FOR UPDATE` по строке кошелька или lock-map по адресу). Объясни это решение в `architecture.md`.

Конвертация: `Convert.toWei(amount, Convert.Unit.ETHER)` / `Convert.fromWei(...)`.

## 4. Архитектура

Слоистая архитектура с явными портами (упрощённая hexagonal):

```
controller (REST API, Thymeleaf UI)
   ↓
service (бизнес-логика: PaymentService, RefundService, ClientService, MerchantService, TreasuryService)
   ↓                ↓                        ↓
repository (JPA)   BlockchainGateway (порт)  ClientDataProvider (порт)
                     ├─ Web3jBlockchainGateway   └─ HttpClientDataProvider (ходит в «сервис ABC» по HTTP)
                     └─ FakeBlockchainGateway
```

Обязательные абстракции (это пойдёт на защиту как SOLID):
- `BlockchainGateway`: `generateKeyPair`, `getBalance(address)`, `estimateTransferFee()`, `sendTransfer(fromPrivateKey, toAddress, amount)` → txHash, `getTransactionStatus(txHash)` → `PENDING | SUCCESS | FAILED`. Бизнес-логика **не знает** про web3j.
- `Web3jBlockchainGateway`: реальная реализация (профиль `sepolia`).
- `FakeBlockchainGateway`: in-memory реализация с балансами в памяти (профиль `fake`, по умолчанию для локальной разработки и тестов). Позволяет запускать проект **без сети и без монет**.
- `PrivateKeyCipher`: шифрование/расшифровка приватных ключей.
- `WalletService`: создание кошельков, хранение зашифрованных ключей, сериализация отправок по адресу (nonce).
- `ClientDataProvider`: порт для «сервиса ABC».

Пакеты: `api` (REST + DTO), `web` (Thymeleaf-контроллеры), `domain` (сущности, enum статусов, доменные исключения), `service`, `port` (интерфейсы), `adapter.web3j`, `adapter.fake`, `adapter.abc`, `crypto`, `repository`, `config`, `scheduler`.

Конфигурация только через `application.yml` + переменные окружения. **Никаких секретов в коде и в git.**

## 5. Модель данных (PostgreSQL, Flyway)

- `wallets`: id (UUID), type (`TREASURY` / `CLIENT` / `MERCHANT`), address (unique), encrypted_private_key, created_at
- `merchants`: id (UUID), name, api_key_hash, wallet_id, created_at
- `clients`: id (UUID), external_id (unique, по нему мерчанты обращаются), wallet_id (nullable — у клиента может не быть кошелька), created_at
- `payments`: id (UUID), merchant_id, client_id, amount NUMERIC(36,18), currency (`ETH`), status, tx_hash, failure_code, failure_message, idempotency_key, request_hash, created_at, updated_at. Уникальный индекс (merchant_id, idempotency_key).
- `refunds`: id (UUID), payment_id, amount, status, tx_hash, failure_code, idempotency_key, created_at, updated_at
- `payment_status_history`: id, payment_id, from_status, to_status, changed_at, note. Аудит всех переходов (ложится на state-диаграмму).
- `treasury_transfers`: id, to_wallet_id, amount, tx_hash, status, created_at (раздачи из казны)

Суммы — только `BigDecimal`, никаких double.

## 6. API

Все ответы JSON. Аутентификация мерчанта: заголовок `X-Api-Key` (в БД хранится хэш). Единый формат ошибки:
```json
{ "code": "INSUFFICIENT_FUNDS", "message": "...", "paymentId": "..." }
```

### 6.1. Мерчантский API (`/api/v1`)

| Метод | Путь | Описание |
|---|---|---|
| POST | `/payments` | Создать оплату. Body: `{ "clientId": "ext-42", "amount": "0.001" }`. Заголовок `Idempotency-Key` обязателен. Синхронно проходит шаги 2–5, возвращает `201` с `{ paymentId, status: "PENDING", txHash, explorerUrl }` или ошибку со `status: FAILED`. Повтор с тем же ключом и тем же телом возвращает тот же результат и повторно не списывает; тот же ключ с другим телом → `IDEMPOTENCY_CONFLICT`. |
| GET | `/payments/{id}` | Статус платежа (мерчант видит только свои). |
| GET | `/payments` | Список платежей мерчанта (пагинация, фильтр по статусу). |
| POST | `/payments/{id}/refunds` | Возврат. Body: `{ "amount": "0.0005" }` (необязательно, по умолчанию весь остаток). `Idempotency-Key` обязателен. |
| GET | `/payments/{id}/refunds` | Список возвратов по платежу. |

### 6.2. Административный API (`/api/admin`, для демо; Basic Auth, пароль из `ADMIN_PASSWORD`)

- `GET /treasury` — адрес и баланс казны
- `POST /clients` — создать клиента (`{ externalId, createWallet: true }`)
- `POST /clients/{id}/wallet` — создать кошелёк существующему клиенту
- `POST /clients/{id}/fund` — перевести клиенту ETH из казны (`{ amount }`)
- `GET /clients/{id}/balance`
- `POST /merchants` — создать мерчанта + кошелёк + перевод ETH из казны на комиссии; вернуть сгенерированный API-ключ **один раз**

### 6.3. Mock «сервис ABC»

Отдельный контроллер в том же приложении под `/mock-abc/clients/{externalId}` → `{ externalId, fullName, email, country, status: "ACTIVE" | "BLOCKED" }`. Данные — из небольшой таблицы или seed-файла. `HttpClientDataProvider` ходит к нему **по HTTP** через `RestClient` (base URL в конфиге `ABC_BASE_URL`), чтобы это был честный внешний вызов и его можно было потом вынести в отдельный сервис. Клиент `BLOCKED` → отказ (`CLIENT_BLOCKED`). Недоступность ABC → `CLIENT_DATA_UNAVAILABLE`.

## 7. Статусы (state machine)

**Payment:**
```
CREATED → CLIENT_VERIFIED → CLIENT_DATA_RECEIVED → BALANCE_CHECKED → PENDING → CONFIRMED
   любой шаг до PENDING ──→ FAILED
   PENDING ──(tx reverted / timeout)──→ FAILED
   CONFIRMED → PARTIALLY_REFUNDED → REFUNDED
   CONFIRMED → REFUNDED
```

**Refund:** `CREATED → PENDING → COMPLETED | FAILED`

Правила:
- Переходы — только через один класс (`PaymentStateMachine`), недопустимый переход → `INVALID_STATE`. Каждый переход пишется в `payment_status_history`.
- Проверка баланса: `balance >= amount + estimatedFee`, иначе `INSUFFICIENT_FUNDS` (в сообщении указать, сколько не хватает).
- Возврат разрешён только для `CONFIRMED` / `PARTIALLY_REFUNDED`.
- Сумма всех не-FAILED возвратов ≤ сумма платежа, иначе `REFUND_EXCEEDS_PAYMENT`.
- Возврат = новая транзакция с кошелька мерчанта на кошелёк клиента (в блокчейне отмены нет). Комиссию за возврат платит мерчант; если у мерчанта не хватает на сумму + комиссию → `INSUFFICIENT_FUNDS`.

Подтверждение: `@Scheduled`-задача раз в ~15 секунд проверяет `PENDING` платежи, возвраты и раздачи из казны через `BlockchainGateway.getTransactionStatus`: SUCCESS → CONFIRMED/COMPLETED, FAILED → FAILED; нет receipt дольше 10 минут (настраивается) → FAILED с `CONFIRMATION_TIMEOUT`.

## 8. Коды ошибок

`UNAUTHORIZED`, `VALIDATION_ERROR`, `CLIENT_NOT_FOUND`, `WALLET_NOT_FOUND`, `CLIENT_BLOCKED`, `CLIENT_DATA_UNAVAILABLE`, `INSUFFICIENT_FUNDS`, `TREASURY_INSUFFICIENT_FUNDS`, `INVALID_AMOUNT`, `BLOCKCHAIN_ERROR`, `INVALID_STATE`, `REFUND_EXCEEDS_PAYMENT`, `IDEMPOTENCY_CONFLICT`, `CONFIRMATION_TIMEOUT`.

Обработка через `@RestControllerAdvice`. Ошибки web3j/RPC оборачиваются в доменные исключения, наружу классы web3j не протекают.

## 9. Thymeleaf-панель (`/admin`, для демонстрации на защите)

Простая, аккуратная, без фронтенд-фреймворков (можно Bootstrap через CDN). Защищена тем же `ADMIN_PASSWORD` (form login).
- **Казна:** адрес (с кнопкой «скопировать» и ссылкой на etherscan), баланс, короткая инструкция «как пополнить из крана Google Cloud».
- **Клиенты:** таблица (external id, адрес, баланс ETH), кнопки «Создать клиента», «Создать кошелёк», «Пополнить из казны».
- **Мерчанты:** таблица (адрес, баланс), создание, показ API-ключа один раз.
- **Платежи:** таблица со статусами (цветные бейджи), ссылка на tx в etherscan, фильтр по статусу. Страница платежа: детали, история статусов, возвраты, кнопка «Вернуть».
- **Тестовая оплата:** форма «мерчант / клиент / сумма» → вызывает тот же `PaymentService`, что и API.

## 10. Нефункциональные требования

- Профили: `fake` (по умолчанию, без сети), `sepolia` (реальная тестовая сеть).
- `.env.example` со всеми переменными; `.env` в `.gitignore`.
- Если `WALLET_ENCRYPTION_KEY` не задан или неверной длины — приложение не стартует с понятной ошибкой.
- Логи: каждый шаг сценария оплаты логируется с `paymentId`. Приватные ключи и API-ключи в логи не попадают.
- Тесты: unit для `PaymentService`, `RefundService`, `PaymentStateMachine`, `PrivateKeyCipher` (на `FakeBlockchainGateway`); интеграционные для API на Testcontainers. Тесты не ходят в реальную сеть.
- `Dockerfile` (multi-stage) + `docker-compose.yml` (app + postgres).
- README: запуск локально (fake), запуск в Sepolia (как пополнить казну), деплой на DigitalOcean (droplet Ubuntu + docker compose), примеры curl. Отдельно предупредить: **потеря `WALLET_ENCRYPTION_KEY` = потеря доступа ко всем кошелькам**; на сервере должен быть тот же ключ, что и в БД.

Переменные окружения:
```
SPRING_PROFILES_ACTIVE=fake|sepolia
DB_URL, DB_USER, DB_PASSWORD
ETH_RPC_URL=https://ethereum-sepolia-rpc.publicnode.com
ETH_CHAIN_ID=11155111
WALLET_ENCRYPTION_KEY=   # openssl rand -base64 32
ADMIN_PASSWORD
ABC_BASE_URL=http://localhost:8080/mock-abc
```

## 11. Что нужно от пользователя (подскажи, когда дойдёшь)

- На этапе 3: сгенерировать `WALLET_ENCRYPTION_KEY` (`openssl rand -base64 32`) и положить в `.env`; выбрать `ETH_RPC_URL`.
- После первого запуска в профиле `sepolia`: взять адрес казны в `/admin`, пополнить его через кран Google Cloud (Sepolia), дождаться баланса.

## 12. Этапы работы

**Этап 0. План.** Прочитай ТЗ, задай уточняющие вопросы, если что-то противоречиво. Предложи структуру проекта. Жди подтверждения.

**Этап 1. Каркас на fake.** Maven-проект, Flyway-миграции, сущности, `BlockchainGateway` + `FakeBlockchainGateway`, `PrivateKeyCipher`, `WalletService`, казна, mock ABC, `PaymentService` со всем сценарием, state machine, REST API, обработка ошибок, Swagger, unit-тесты. Всё работает без сети: `docker compose up postgres` + `mvn spring-boot:run`.

**Этап 2. Возвраты и подтверждения.** `RefundService`, планировщик подтверждений, история статусов, тесты.

**Этап 3. Sepolia.** `Web3jBlockchainGateway`, сериализация отправок по адресу (nonce). Попроси пользователя заполнить `.env` и пополнить казну. Ручной smoke-тест вместе с пользователем: создать мерчанта и клиента, пополнить клиента из казны, оплатить, дождаться CONFIRMED, сделать возврат. Покажи ссылки на etherscan.

**Этап 4. Thymeleaf-панель.**

**Этап 5. Docker, деплой, README.**

**Этап 6. Материалы для защиты** (в `docs/`):
- `requirements.md` — требования: функциональные (по сценариям), нефункциональные, ограничения, допущения.
- Диаграммы в PlantUML (`.puml`) + отрендеренные PNG/SVG: **context**, **sequence** (оплата и возврат), **class** (основные классы и порты), **state** (Payment и Refund).
- `architecture.md` — описание архитектуры, решения (кастодиальная модель, шифрование ключей, казна, nonce-блокировки, идемпотентность) и **где какой принцип SOLID** с конкретными классами (SRP; OCP — новый блокчейн через новую реализацию `BlockchainGateway` без изменения сервисов; LSP — Fake/Web3j взаимозаменяемы; ISP — узкие порты; DIP — сервисы зависят от интерфейсов).

## 13. Правила

- Не коммить секреты. Не выводи приватные ключи и API-ключи в логи и ответы.
- Не выдумывай методы web3j: если сомневаешься — проверь по исходникам/документации web3j той версии, что в pom.xml.
- Код и комментарии — понятные студенту: проект будут защищать устно.
- Коммиты небольшие, по этапам, с понятными сообщениями.
