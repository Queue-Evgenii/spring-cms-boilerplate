# audit_logs

Миграция: `V2__auth_sessions_and_logs.sql`. Журнал изменений данных: что, кем, когда, было и стало. Только добавление.

| Колонка | Тип | Null | По умолчанию | Описание |
|---|---|---|---|---|
| `id` | bigint | нет | identity | Первичный ключ |
| `entity_type` | varchar(50) | нет | | Например `USER`, `PAGE`, `SETTING` |
| `entity_id` | varchar(64) | нет | | Id изменённой записи (строкой, у сущностей разные типы id) |
| `action` | varchar(20) | нет | | `CREATE`, `UPDATE`, `DELETE`, `RESTORE` |
| `actor_type` | varchar(10) | нет | | `USER` или `SYSTEM` |
| `actor_id` | uuid | да | | Кто изменил. `NULL` для `SYSTEM` |
| `old_values` | jsonb | да | | Прежние значения изменённых полей |
| `new_values` | jsonb | да | | Новые значения |
| `ip` | varchar(45) | да | | |
| `user_agent` | text | да | | |
| `created_at` | timestamptz | нет | `now()` | |

## Индексы
- `(entity_type, entity_id, created_at DESC)`, `(actor_id, created_at DESC)`, `(created_at DESC)`.

## Правила
- Не изменяется и не удаляется.
- Секреты (`password_hash`, `totp_secret_encrypted`) в значения **не пишем**: только факт изменения (`"changed": true`).
- `actor_type = SYSTEM` для действий без пользователя (сидер, планировщик, автоблокировка).

