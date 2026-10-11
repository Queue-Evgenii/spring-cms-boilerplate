# auth_events

Миграция: `V2__auth_sessions_and_logs.sql`. Журнал всех попыток входа и событий сессий. Только добавление.

| Колонка | Тип | Null | По умолчанию | Описание |
|---|---|---|---|---|
| `id` | bigint | нет | identity | Первичный ключ (объём большой, uuid не нужен) |
| `user_id` | uuid | да | | FK на `users(id)`, `ON DELETE SET NULL`. `NULL`, если email не найден |
| `email_attempted` | varchar(255) | да | | Что ввели |
| `event_type` | varchar(30) | нет | | См. ниже |
| `reason` | varchar(50) | да | | Причина отказа |
| `session_id` | uuid | да | | Связанная сессия |
| `ip` | varchar(45) | да | | |
| `user_agent` | text | да | | |
| `country` | varchar(2) | да | | |
| `city` | varchar(100) | да | | |
| `details` | jsonb | да | | Дополнительно: номер попытки, остаток до блокировки и т. д. |
| `created_at` | timestamptz | нет | `now()` | |

## event_type
`LOGIN_SUCCESS`, `LOGIN_FAILED`, `TOTP_FAILED`, `TOTP_SUCCESS`, `LOGOUT`, `SESSION_KICKED`, `ACCOUNT_LOCKED`, `PASSWORD_CHANGED`, `PASSWORD_RESET_REQUESTED`.

## Индексы
- `(user_id, created_at DESC)`, `(ip, created_at DESC)`, `(created_at DESC)`, `(event_type, created_at DESC)`.

## Правила
- Не изменяется и не удаляется (очистка только по сроку хранения из настроек).
- Пароли и коды в журнал не попадают.

