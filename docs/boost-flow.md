# Логика буста — документация для фронтенда

## Общая схема фаз

```
ROUND_1 / ROUND_2
    ↓  (30 сек)
BOOST_DECISION_1 / BOOST_DECISION_2
    ↓  (5 сек)
BOOST_WINDOW_1 / BOOST_WINDOW_2
    ↓  (5 сек)
ROUND_COMPLETED
```

---

## Фаза 1 — ROUND (выбор бочек и покупка буста)

**Статус комнаты:** `ROUND_1` или `ROUND_2`

### Действия пользователя

#### 1. Получить список бочек
```
GET /api/v1/game/rooms/{roomId}/rounds/{n}/barrels
Authorization: Bearer <token>
```
Возвращает бочки в порядке, перемешанном индивидуально для каждого пользователя (Provably Fair).  
**Веса на этом этапе = `null`** — они ещё не раскрыты.

#### 2. Выбрать бочки
```
POST /api/v1/game/rooms/{roomId}/rounds/{n}/selection
Authorization: Bearer <token>
Content-Type: application/json

{
  "barrelIds": ["uuid1", "uuid2", "uuid3"]
}
```
Можно вызвать повторно — выбор перезаписывается.

#### 3. Купить буст (опционально)
```
POST /api/v1/game/rooms/{roomId}/rounds/{n}/boost
Authorization: Bearer <token>
```
- Стоимость буста списывается с бонусного баланса
- Буст покупается **вслепую** — веса бочек ещё неизвестны
- Купить буст можно только **один раз за игру** (раунд 1 или раунд 2, не оба)
- Деньги добавляются в призовой фонд комнаты

---

## Фаза 2 — BOOST_DECISION (решение принято, ожидание)

**Статус комнаты:** `BOOST_DECISION_1` или `BOOST_DECISION_2`

**WS-событие:** `BOOST_DECISION_STARTED`
```json
{
  "type": "BOOST_DECISION_STARTED",
  "roundNumber": 1,
  "expiresAt": 1713700000000
}
```

> Веса ещё **не раскрыты**. Покупка буста на этом этапе **недоступна**.  
> Фаза длится **5 секунд** — показываем countdown.

---

## Фаза 3 — BOOST_WINDOW (раскрытие весов и эффект буста)

**Статус комнаты:** `BOOST_WINDOW_1` или `BOOST_WINDOW_2`

**WS-событие:** `BOOST_WINDOW_STARTED`
```json
{
  "type": "BOOST_WINDOW_STARTED",
  "roundNumber": 1,
  "seedHash": "a3f1...",
  "rawSeed": "00ff...",
  "barrelWeights": {
    "<barrelId-1>": 4.5,
    "<barrelId-2>": -3.2,
    "<barrelId-3>": 1.8
  },
  "boostEffects": {
    "<participantId>": {
      "barrelId": "<barrelId-2>",
      "originalWeight": -3.2,
      "boostedWeight": 3.2
    }
  },
  "expiresAt": 1713700005000
}
```

### Что делать на фронте

1. **Показать веса** всех бочек из `barrelWeights`
2. **Если текущий пользователь есть в `boostEffects`** — показать анимацию буста:
   - Бочка с `barrelId` была `originalWeight` → стала `boostedWeight`
3. Фаза длится **5 секунд** — показываем анимацию и countdown

### Логика применения буста (для понимания)

Буст применяется **автоматически** на сервере по следующим правилам:

| Ситуация | Эффект |
|---|---|
| Среди выбранных бочек есть отрицательные веса | Меняет знак у бочки с **наибольшим по модулю** отрицательным весом (`-5.0` → `+5.0`) |
| Все веса положительные | Удваивает бочку с **минимальным** положительным весом (`2.0` → `4.0`) |

---

## Фаза 4 — ROUND_COMPLETED

**WS-событие:** `ROUND_COMPLETED`
```json
{
  "type": "ROUND_COMPLETED",
  "roundNumber": 1,
  "winnerId": "<participantId>",
  "winCriteria": "SCORE",
  "disqualifiedIds": []
}
```

Финальный счёт уже учитывает применённый буст.

---

## Ограничения буста

| Правило | Описание |
|---|---|
| Один буст на игру | Нельзя купить буст в раунде 2, если купил в раунде 1 |
| Только во время раунда | Кнопка покупки доступна только в статусе `ROUND_1` / `ROUND_2` |
| Буст включён в конфиге | Если `boostEnabled = false` — кнопку скрыть |
| Достаточно баланса | Показывать стоимость буста и блокировать кнопку при нехватке |

---

## Сводная таблица WS-событий

| Событие | Фаза | Что содержит |
|---|---|---|
| `ROUND_STARTED` | `ROUND_1/2` | `barrelIds`, `seedHash`, `expiresAt` |
| `PLAYER_SELECTED` | `ROUND_1/2` | уведомление о выборе другого игрока |
| `BOOST_DECISION_STARTED` | `BOOST_DECISION_1/2` | `expiresAt` |
| `BOOST_WINDOW_STARTED` | `BOOST_WINDOW_1/2` | `barrelWeights`, `boostEffects`, `seedHash`, `rawSeed`, `expiresAt` |
| `ROUND_COMPLETED` | — | `winnerId`, `winCriteria`, `disqualifiedIds` |
| `FINALISTS_ANNOUNCED` | — | `finalistIds`, `winCriteria` |
| `GAME_FINISHED` | — | итоги игры |

---

## Пример полного флоу с бустом

```
1. [ROUND_1]            Пользователь выбирает 3 бочки
2. [ROUND_1]            Пользователь нажимает "Купить буст" → POST /boost
3. [BOOST_DECISION_1]   WS: BOOST_DECISION_STARTED → показать "Подготовка результатов..." (5 сек)
4. [BOOST_WINDOW_1]     WS: BOOST_WINDOW_STARTED → показать веса + анимация буста (5 сек)
                          boostEffects["userId"] → бочка -3.2 превратилась в +3.2
5. [ROUND_COMPLETED]    WS: ROUND_COMPLETED → показать итоговые очки и победителя раунда
```
