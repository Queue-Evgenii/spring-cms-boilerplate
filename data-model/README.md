# Модели данных

Единый источник истины по схеме БД. Один файл на таблицу. Обновляй файл вместе с миграцией: изменил миграцию, поправь описание.

## Соглашения

- Таблицы во множественном числе, имена в `snake_case`.
- Время: `timestamptz` в БД, `Instant` в Kotlin.
- `id`: `uuid`.
- Перечисления: `varchar` + `CHECK` в БД, `enum class` в Kotlin (`EnumType.STRING`).
- Чувствительные данные: хеши и шифртекст, никаких открытых секретов.

## Таблицы

| Таблица | Файл | Миграция | Назначение |
|---|---|---|---|
| `users` | [users.md](users.md) | `V1__create_users.sql` | Администраторы CMS |
| `password_reset_tokens` | [password_reset_tokens.md](password_reset_tokens.md) | запланировано | Токены сброса пароля |
| `sessions` | [sessions.md](sessions.md) | `V2__auth_sessions_and_logs.sql` | Сессии и устройства |
| `auth_events` | [auth_events.md](auth_events.md) | `V2__auth_sessions_and_logs.sql` | Журнал входов |
| `audit_logs` | [audit_logs.md](audit_logs.md) | `V2__auth_sessions_and_logs.sql` | Журнал изменений |

Исключение из правила про `uuid`: журналы (`auth_events`, `audit_logs`) используют `bigint identity`, их много и порядок важен.

## Шаблон файла таблицы

1. Название и назначение.
2. Таблица колонок: колонка, тип, null, по умолчанию, описание.
3. Индексы и ограничения.
4. Enum-ы, связанные с таблицей.
5. Заметки и отложенные решения.

