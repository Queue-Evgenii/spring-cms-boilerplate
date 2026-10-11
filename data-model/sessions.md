# sessions

Миграция: `V2__auth_sessions_and_logs.sql`. Сессии входа: одна запись на устройство.

| Колонка | Тип | Null | По умолчанию | Описание |
|---|---|---|---|---|
| `id` | uuid | нет | `uuidv7()` | Первичный ключ |
| `user_id` | uuid | нет | | FK на `users(id)`, `ON DELETE CASCADE` |
| `token_hash` | varchar(64) | нет | | Хеш токена из куки. Сам токен в БД не хранится |
| `login_realm` | varchar(20) | нет | | `ADMIN` или `PUBLIC`: через какой вход создана |
| `ip` | varchar(45) | да | | IP при входе |
| `user_agent` | text | да | | Заголовок браузера |
| `device` | varchar(30) | да | | desktop / mobile / tablet |
| `os` | varchar(50) | да | | |
| `browser` | varchar(50) | да | | |
| `country` | varchar(2) | да | | ISO-код (GeoIP) |
| `city` | varchar(100) | да | | (GeoIP) |
| `last_active_at` | timestamptz | нет | `now()` | Обновляется не чаще раза в минуту |
| `expires_at` | timestamptz | нет | | Срок из настроек |
| `revoked_at` | timestamptz | да | | Когда завершена |
| `revoked_reason` | varchar(30) | да | | `LOGOUT`, `KICKED_BY_LIMIT`, `PASSWORD_CHANGED`, `BY_ADMIN`, `BLOCKED` |
| `created_at` | timestamptz | нет | `now()` | |

## Индексы и ограничения
- PK по `id`; уникальный индекс по `token_hash`.
- Индекс по `(user_id, revoked_at, expires_at)` для списка активных сессий.
- `CHECK (login_realm IN ('ADMIN','PUBLIC'))`.

## Правила
- Активна, если `revoked_at IS NULL AND expires_at > now()`.
- Online: `last_active_at` не старше ~5 минут.
- Лимит активных сессий на пользователя из настроек. При превышении по умолчанию завершается самая старая (`KICKED_BY_LIMIT`), режим `DENY` отказывает во входе.
- Админские эндпоинты принимают только `login_realm = ADMIN`. Публичные принимают обе.

