# Backend

Платформа коротких игровых сессий для Столото. Игроки заходят в комнаты, выбирают бочки с рандомными весами, лучший по сумме весов за два раунда забирает призовой фонд. Призы выплачиваются бонусными баллами.

Два Spring Boot сервиса: `core` — пользователи, баланс, авторизация, WebSocket-relay; `service-game-lucky-rabbit` — игровая логика, комнаты, раунды, бочки, Provably Fair.

## Сервисы

| Сервис | URL |
|---|---|
| core API | http://92.51.23.102:8080 |
| core Swagger UI | http://92.51.23.102:8080/swagger-ui.html |
| game API | http://92.51.23.102:8081 |
| game Swagger UI | http://92.51.23.102:8081/swagger-ui.html |
| Grafana | http://92.51.23.102:3000 |
| Prometheus | http://92.51.23.102:9090 |

Данные для Grafana
```
GRAFANA_ADMIN_USER=kubok_admin
GRAFANA_ADMIN_PASSWORD=/A7lwLV8H5VqkSy+4bDUng==
```

## Стек

Java 21, Spring Boot 3.2, PostgreSQL 16, Apache Kafka 3.9, Quartz, WebSocket/STOMP, Flyway, Maven (multi-module). Мониторинг: Prometheus + Loki + Grafana.

## Запуск

Создай `.env` в корне (см. [Переменные окружения](#переменные-окружения)).

```bash
# Запустить всё (оба сервиса + инфраструктура + мониторинг)
docker-compose up --build -d

# Только инфраструктура для локальной разработки
docker-compose up postgres kafka

```

Порты по умолчанию: `core` — 8080, `service-game-lucky-rabbit` — 8081, Grafana — 3000, Prometheus — 9090.

Swagger UI: `http://localhost:8080/swagger-ui.html` и `http://localhost:8081/swagger-ui.html`.

## Переменные окружения

| Переменная | Описание | Дефолт |
|---|---|---|
| `DB_HOST` | Хост PostgreSQL | `localhost` |
| `DB_PORT` | Порт PostgreSQL | `5432` |
| `DB_NAME` | Имя базы данных | — |
| `DB_USER` | Пользователь БД | — |
| `DB_PASS` | Пароль БД | — |
| `JWT_SECRET` | Секрет для подписи JWT | — |
| `INTERNAL_SECRET` | Секрет для межсервисных вызовов (`X-Internal-Secret`) | — |
| `ADMIN_USER` | Логин администратора (core) | — |
| `ADMIN_PASSWORD` | Пароль администратора (core) | — |
| `KAFKA_BOOTSTRAP` | Адрес Kafka | `localhost:9092` |
| `CORE_SERVICE_URL` | URL core из game-сервиса | `http://localhost:8080` |
| `WS_ALLOWED_ORIGINS` | CORS для WebSocket | `http://localhost:3000` |
| `CORE_PORT` | Порт core | `8080` |
| `GAME_PORT` | Порт game-сервиса | `8081` |
| `GRAFANA_ADMIN_USER` | Логин Grafana | — |
| `GRAFANA_ADMIN_PASSWORD` | Пароль Grafana | — |

## Структура

```
backend/
├── core/                      # Пользователи, баланс, авторизация, WebSocket-relay
├── service-game-lucky-rabbit/ # Игровые комнаты, раунды, бочки, призы
├── docker/                    # init-schemas.sql
├── monitoring/                # Конфиги Prometheus, Loki, Grafana
└── docker-compose.dev.yml
```

Оба сервиса работают с одной PostgreSQL — схемы `core` и `game` разделены.

Архитектура каждого сервиса: `presentation → application → domain ← infrastructure` (Hexagonal/DDD). Kafka-события идут через Transactional Outbox, а не напрямую из бизнес-логики.

## API

### core (`:8080`)

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Регистрация, возвращает JWT |
| `POST` | `/api/v1/auth/login` | Вход, возвращает JWT |
| `GET` | `/api/v1/users` | Список пользователей (JWT) |
| `POST` | `/api/v1/users` | Создать пользователя (JWT) |
| `GET` | `/api/v1/users/{guid}` | Получить пользователя (JWT) |
| `PUT` | `/api/v1/users/{guid}` | Обновить пользователя (JWT) |
| `DELETE` | `/api/v1/users/{guid}` | Удалить пользователя (JWT) |
| `GET` | `/api/v1/users/me/balance` | Баланс текущего пользователя (JWT) |

Внутренние эндпоинты `/internal/**` доступны только с заголовком `X-Internal-Secret`.

### service-game-lucky-rabbit (`:8081`)

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/v1/game/rooms` | Создать комнату (ADMIN) |
| `GET` | `/api/v1/game/rooms` | Список комнат с фильтрами |
| `GET` | `/api/v1/game/rooms/affordable` | Комнаты по балансу текущего пользователя |
| `GET` | `/api/v1/game/rooms/suggest` | Подобрать комнату (по цене и числу мест) |
| `POST` | `/api/v1/game/rooms/admin/evaluate` | Оценить конфигурацию без создания (ADMIN) |
| `DELETE` | `/api/v1/game/rooms/admin/{roomId}` | Отменить комнату, вернуть баллы участникам (ADMIN) |
| `GET` | `/api/v1/game/rooms/{roomId}` | Получить комнату |
| `POST` | `/api/v1/game/rooms/{roomId}/join` | Войти в комнату |
| `GET` | `/api/v1/game/rooms/{roomId}/participants` | Участники с вероятностью победы |
| `GET` | `/api/v1/game/rooms/{roomId}/next-game` | Рекомендации следующей игры (SAME/SAFER/RISKIER) |
| `GET` | `/api/v1/game/rooms/{roomId}/rounds/{n}/barrels` | Бочки раунда (перемешаны per-user) |
| `POST` | `/api/v1/game/rooms/{roomId}/rounds/{n}/selection` | Выбрать бочки |
| `POST` | `/api/v1/game/rooms/{roomId}/rounds/{n}/boost` | Купить буст |
| `GET` | `/api/v1/game/rooms/{roomId}/rounds/{n}/result` | Результат раунда |
| `POST` | `/api/v1/game/rooms/{roomId}/rounds/2/ready` | Подтвердить готовность к финальному раунду |
| `GET` | `/api/v1/game/rooms/{roomId}/rounds/{n}/verify` | Верификация Provably Fair (commit-reveal SHA-256) |
| `GET` | `/api/v1/game/rooms/{roomId}/history` | История игры |
| `GET` | `/api/v1/game/rooms/{roomId}/events` | Хронологический лог событий комнаты |

### WebSocket

Подключение: `ws://localhost:8080/ws`

| Топик | События |
|---|---|
| `/topic/rooms` | `ROOM_CREATED`, `ROOM_SCHEDULED`, `ROOM_FULL` |
| `/topic/room/{roomId}` | `ROOM_UPDATED` |
| `/topic/room/{roomId}/round` | `ROUND_STARTED`, `PLAYER_SELECTED`, `BOOST_DECISION_STARTED`, `BOOST_WINDOW_STARTED`, `ROUND_COMPLETED` |
| `/topic/room/{roomId}/game` | `FINALISTS_ANNOUNCED`, `GAME_FINISHED` |
| `/topic/user/{userId}` | `ROOM_CANCELLED`, `BALANCE_UPDATED` |

## Аутентификация

Внешние запросы: `Authorization: Bearer <jwt>`. Токен живёт 24 часа.

Межсервисные вызовы (core ↔ game): `X-Internal-Secret: <значение INTERNAL_SECRET>`. Без этого заголовка — 403.

## Мониторинг

Метрики Prometheus: `/actuator/prometheus` на каждом сервисе. Grafana поднимается из коробки — provisioning в `monitoring/grafana/provisioning/`.


## Данные пользователя, необходимые игровому модулю

Игровой сервис не хранит пользовательские профили — вся информация о пользователе поступает из JWT-токена при каждом запросе.

**Поля, извлекаемые из JWT:**

| Поле | Тип | Описание |
|---|---|---|
| `id` | `UUID` | Идентификатор пользователя (`userId`) |
| `username` | `String` | Отображаемое имя (используется в комнатах и истории) |
| `roles` | `List<String>` | Роли: `"user"`, `"admin"` |

**Требования к токену:**

- Алгоритм подписи: HMAC SHA-256
- Секрет подписи: значение переменной `JWT_SECRET` (одинаковое для core и game)
- Срок действия: 24 часа

Токен генерирует `core`-сервис при логине/регистрации. При замене `core` на собственный бэкенд нужно выдавать токены с теми же claims и тем же секретом.


## Передача баланса бонусных баллов

Игровой сервис **не хранит баланс** — он читает и изменяет его через `core`.

### Чтение баланса (синхронно, HTTP)

Перед входом в комнату и перед покупкой буста game-сервис вызывает:

```
GET /internal/balance/{userId}
X-Internal-Secret: <INTERNAL_SECRET>
```

- `available` — доступно для трат прямо сейчас
- `reserved` — заблокировано внутри активных комнат

### Изменение баланса (асинхронно, Kafka)

Все операции списания и начисления отправляются в Kafka-топик `balance.command`. `core`-сервис консьюмит и обрабатывает команды.

**Типы команд:**

| Команда | Когда | Что делает |
|---|---|---|
| `RESERVE` | Игрок вошёл в комнату | Перекладывает `entryFee` из `available` в `reserved` |
| `RELEASE` | Комната отменена администратором | Возвращает `reserved` → `available` |
| `DEDUCT` | Куплен буст | Списывает стоимость буста из `available` |
| `DEDUCT_RESERVED` | Игра завершена | Окончательно списывает зарезервированный взнос |
| `AWARD` | Игра завершена | Зачисляет приз победителю |

Все команды идемпотентны. Ключ идемпотентности: `{COMMAND_TYPE}:{userId}:{roomId}`.

## Фиксация операций списания и начисления

### Транзакции в `core` (схема `core`, таблица `pointTransactions`)

Каждая команда из Kafka порождает одну запись:

| Поле | Тип | Описание |
|---|---|---|
| `id` | UUID | Первичный ключ |
| `userId` | UUID | Пользователь |
| `type` | enum | `RESERVE`, `DEDUCT`, `AWARD`, `BOOST_PURCHASE` |
| `amount` | decimal(12,2) | Сумма операции |
| `gameRoomId` | UUID | Комната (nullable) |
| `description` | String | Текстовое описание |
| `idempotencyKey` | String | Уникальный ключ (UNIQUE-индекс) |
| `createdAt` | Instant | Время операции |

Описания, записываемые в `description`:

- `"Entry fee reserved"` — взнос зарезервирован
- `"Entry fee returned to available"` — взнос возвращён
- `"Entry fee deducted"` — взнос окончательно списан
- `"Boost purchased"` — куплен буст
- `"Prize awarded"` — начислен приз

### Лог событий в `service-game-lucky-rabbit` (таблица `gameEventLog`)

Параллельно игровой сервис пишет аудит-лог всех событий комнаты:

| Событие | Когда записывается |
|---|---|
| `ROOM_CREATED` | Создание комнаты |
| `PLAYER_JOINED` | Игрок вошёл |
| `ROOM_STARTED` | Игра началась |
| `ROUND_STARTED` | Старт раунда (с `seedHash`) |
| `WEIGHTS_REVEALED` | Раскрытие весов бочек |
| `PARTICIPANT_SCORED` | Подсчёт очков участника |
| `BOOST_PURCHASED` | Покупка буста |
| `ROUND_COMPLETED` | Раунд завершён |
| `FINALISTS_ANNOUNCED` | Объявлены финалисты |
| `GAME_FINISHED` | Игра завершена с распределением призов |
| `ROOM_CANCELLED` | Комната отменена |

Лог доступен через API: `GET /api/v1/game/rooms/{roomId}/events`.

---

## API запуска раунда и определения победителя

### Запуск раунда

Раунд 1 стартует автоматически — ручного триггера нет. Сценарии:

**Комната заполнена сразу:** последний вошедший игрок заполняет лимит → раунд 1 стартует немедленно.

**Комната заполнена не полностью:** через `app.game.wait-timer-seconds` (дефолт: 60 с) оставшиеся места заполняются ботами, затем стартует раунд 1.

**Запланированная комната:** администратор создаёт комнату с `scheduledStartAt`. В назначенное время `OpenScheduledRoomJob` переводит статус в `WAITING` и запускает таймер ожидания.

**Участник может повлиять на раунд через:**

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/v1/game/rooms/{roomId}/rounds/{n}/selection` | Выбор бочек (до истечения таймера раунда) |
| `POST` | `/api/v1/game/rooms/{roomId}/rounds/{n}/boost` | Покупка буста в окне буста |

### Хронология одного раунда

```
t=0s   ROUND_STARTED      — публикуется seedHash, начинается выбор бочек
t=30s  ResolveRoundJob    — веса раскрываются из rawSeed (SHA-256 commit-reveal)
t=35s  BoostWindowStartJob — окно покупки буста (веса и эффекты видны)
t=40s  FinalizeRoundJob   — подсчёт очков, определение победителя
```

### Алгоритм определения победителя

1. **Генерация весов:** из `rawSeed` получается 12 случайных значений в диапазоне `[-10, +10]`, по одному на бочку.

2. **Базовый счёт:** `сумма весов выбранных бочек`.

3. **Эффект буста** (если куплен):
   - Есть отрицательные веса в выборке → самый негативный вес меняет знак на положительный.
   - Все веса положительные → минимальный вес удваивается.

4. **Итоговый счёт:** `базовый счёт + эффект буста`.

5. **Ранжирование:**
   - Первично: `totalScore DESC`.
   - Тай-брейк: `selectionTimestamp ASC` (кто выбрал раньше при равных очках).

6. **Критерий победы** (поле `winCriteria` в истории):
   - `"highest_score"` — явный лидер по очкам.
   - `"tiebreak_timestamp"` — победа по времени при ничье.
   - `"direct"` — все остальные выбыли (не сделали выбор).

7. **Выбывание:** участник, не сделавший выбор бочек до истечения таймера, получает статус `ELIMINATED`; его зарезервированный взнос списывается.

### WebSocket-события раунда

| Событие | Данные |
|---|---|
| `ROUND_STARTED` | `roundNumber`, `barrelIds[]`, `seedHash`, `expiresAt` |
| `PLAYER_SELECTED` | `userId`, `selectionCount` |
| `BOOST_DECISION_STARTED` | веса бочек |
| `BOOST_WINDOW_STARTED` | веса + эффекты буста на каждого участника |
| `ROUND_COMPLETED` | `winnerId`, `winCriteria`, финальные очки участников |

---

## Хранение истории участия

### Таблица `gameHistory` (схема `game`)

По одной записи на завершённую игру (уникальный индекс по `gameRoomId`):

| Поле | Тип | Описание |
|---|---|---|
| `gameRoomId` | UUID | Комната |
| `winnerUserId` | UUID | Победитель (null, если бот) |
| `winnerIsBot` | boolean | Победил бот |
| `prizeAwarded` | decimal(12,2) | Начисленный приз |
| `systemRevenue` | decimal(12,2) | Доход платформы |
| `completedAt` | Instant | Время завершения |
| `winCriteria` | String | Критерий победы |
| `realPlayersCount` | int | Число живых игроков |
| `botCount` | int | Число ботов |
| `realPlayersRevenue` | decimal(12,2) | Взносы живых игроков |
| `boostRevenue` | decimal(12,2) | Выручка от бустов |
| `boostUsedCount` | int | Сколько бустов куплено |
| `winnerUsedBoost` | boolean | Победитель использовал буст |
| `entryFeeAmount` | decimal(12,2) | Стоимость входа |
| `boostAvailable` | boolean | Буст был включён |

### Таблица `participantRoundEntries` (схема `game`)

Детализация по участнику и раунду:

| Поле | Тип | Описание |
|---|---|---|
| `roundResultId` | UUID | Раунд |
| `participantId` | UUID | Участник |
| `boostPurchased` | boolean | Куплен буст |
| `totalScore` | decimal | Итоговый счёт |
| `rankInRound` | int | Место (1 = победитель) |
| `selectionTimestamp` | Instant | Время выбора бочек |

### API истории

```
GET /api/v1/game/rooms/{roomId}/history
Authorization: Bearer <jwt>
```

Ответ содержит: победителя, сумму приза, критерий победы, число игроков/ботов, статистику бустов и детализацию по каждому участнику (счёт, место, буст, флаг победителя).

```
GET /api/v1/game/rooms/{roomId}/events
Authorization: Bearer <jwt>
```

Хронологический лог всех событий комнаты с параметрами (для отладки и верификации).

---

## Управление конфигурацией комнат администратором

### Создание комнаты

```
POST /api/v1/game/rooms
Authorization: Bearer <admin-jwt>
```

**Параметры конфигурации:**

| Параметр | Тип | Ограничения | Описание |
|---|---|---|---|
| `maxPlayers` | int | 2–10 | Число мест в комнате |
| `entryFeeAmount` | decimal | ≥ 0.01 | Стоимость входа в баллах |
| `winnerPayoutPercentage` | decimal | 1–100 | Доля призового фонда победителю (%) |
| `boostCostAmount` | decimal | ≥ 0 | Стоимость буста |
| `boostEnabled` | boolean | — | Включить функцию буста |
| `maxBarrelSelection` | int | 1–10 | Максимум бочек для выбора |
| `scheduledStartAt` | Instant | В будущем | Время открытия (null = сразу) |
| `repeatInterval` | enum | — | Периодичность: `EVERY_30_MIN`, `EVERY_HOUR`, `EVERY_DAY`, `EVERY_WEEK`, `EVERY_MONTH` (null = разовая) |
| `confirmWarnings` | boolean | — | `true`, если конфиг содержит предупреждения уровня ERROR |

Все поля конфигурации **неизменяемы после создания**.

### Предварительная оценка конфигурации

```
POST /api/v1/game/rooms/admin/evaluate
Authorization: Bearer <admin-jwt>
```

Возвращает финансовый анализ без создания комнаты:

| Поле | Описание |
|---|---|
| `projectedPrizePool` | Призовой фонд при полном заполнении |
| `projectedSystemRevenue` | Доход платформы при полном заполнении |
| `systemRevenuePercent` | Доход как % от общего пула |
| `playerExpectedValue` | Математическое ожидание для игрока (приз − взнос) |
| `attractivenessScore` | `HIGH`, `MEDIUM`, `LOW` |
| `warnings[]` | Список предупреждений |

**Предупреждения валидатора:**

| Код | Уровень | Условие |
|---|---|---|
| `LOW_PLAYER_PAYOUT` | WARN | Выплата < 50% |
| `LOW_ORGANIZER_REVENUE` | WARN | Выплата > 95% |
| `BOOST_TOO_EXPENSIVE` | WARN | Буст дороже взноса |
| `BOOST_CONFIG_INCONSISTENT` | ERROR | Буст выключен, но цена > 0 |
| `LOW_SELECTION_CHOICE` | WARN | Выбор только 1 бочки |
| `SMALL_ROOM` | INFO | 2 игрока в комнате |

При наличии ERROR-предупреждений комнату можно создать только с `confirmWarnings: true`.

### Отмена комнаты

```
DELETE /api/v1/game/rooms/admin/{roomId}
Authorization: Bearer <admin-jwt>
```

Работает только для комнат в статусе `WAITING`. Автоматически возвращает зарезервированные взносы всем участникам через команду `RELEASE` в Kafka.

---

## Интеграция в существующий личный кабинет или игровой раздел

Сервис `core` — это эмулятор вашего бэкенда. Он реализует два контракта, которые игровой сервис ожидает от реальной системы.

### Контракт 1. Пользователи и JWT

Игровой сервис проверяет JWT из заголовка `Authorization: Bearer <token>`. Токен должен содержать:

- `sub` — UUID пользователя
- `username` — отображаемое имя
- `roles` — список ролей (`["user"]` или `["admin"]`)

Реализуйте в вашем бэкенде выдачу таких токенов с тем же секретом, что указан в `JWT_SECRET`.

### Контракт 2. Баланс (HTTP)

Игровой сервис обращается к `core` по внутренним эндпоинтам с заголовком `X-Internal-Secret` для чтения баланса.

Реализуйте в вашем бэкенде:

```
GET  /internal/balance/{userId}          — текущий баланс
```

Ответ:
```json
{ "available": "100.50", "reserved": "25.00" }
```

### Контракт 3. Баланс (Kafka)

Все операции изменения баланса приходят асинхронно через Kafka-топик `balance.command`. Ваш бэкенд должен консьюмить этот топик и обрабатывать команды:

| Команда | Действие |
|---|---|
| `RESERVE` | Заморозить `amount` из доступного баланса |
| `RELEASE` | Разморозить `amount`, вернуть в доступный |
| `DEDUCT` | Списать `amount` из доступного баланса |
| `DEDUCT_RESERVED` | Окончательно списать замороженную сумму |
| `AWARD` | Зачислить `amount` на баланс |

Все операции должны быть идемпотентны по ключу: `{commandType}:{userId}:{roomId}`.

### Что нужно изменить в конфиге

Переменная `CORE_SERVICE_URL` в `.env` — поменяйте с адреса `core` на адрес вашего бэкенда. После этого `core` можно убрать из docker-compose, игровой сервис будет работать с вашей системой напрямую.
