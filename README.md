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

## Стек

Java 21, Spring Boot 3.2, PostgreSQL 16, Apache Kafka 3.9, Quartz, WebSocket/STOMP, Flyway, Maven (multi-module). Мониторинг: Prometheus + Loki + Grafana.

## Запуск

Создай `.env` в корне (см. [Переменные окружения](#переменные-окружения)).

```bash
# Запустить всё (оба сервиса + инфраструктура + мониторинг)
docker-compose -f docker-compose.dev.yml up --build -d

# Только инфраструктура для локальной разработки
docker-compose -f docker-compose.dev.yml up postgres kafka

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
