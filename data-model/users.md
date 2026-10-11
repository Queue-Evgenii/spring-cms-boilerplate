# users

Все пользователи системы: администраторы и клиенты публичного сайта (поле `user_type`). Один email это один аккаунт.
Миграции: `V1__create_users.sql`, колонка `user_type` добавляется в V2.

| Колонка | Тип | Null | По умолчанию | Описание |
|---|---|---|---|---|
| `id` | uuid | нет | `uuidv7()` | Первичный ключ. Требуется PostgreSQL 18+ |
| `first_name` | varchar(100) | да | | Имя |
| `last_name` | varchar(100) | да | | Фамилия |
| `middle_name` | varchar(100) | да | | Отчество |
| `phone` | varchar(20) | да | | Формат E.164 |
| `email` | varchar(255) | нет | | Логин. В коде приводить к нижнему регистру |
| `password_hash` | varchar(255) | нет | | Хеш пароля (Spring `PasswordEncoder`) |
| `must_change_password` | boolean | нет | `true` | Требовать смену пароля при входе |
| `password_changed_at` | timestamptz | да | | Когда пароль менялся в последний раз |
| `user_type` | varchar(20) | нет | `'CUSTOMER'` | `ADMIN` или `CUSTOMER` (V2). Сидер создаёт `ADMIN` |
| `status` | varchar(20) | нет | `'PENDING'` | См. `UserStatus` |
| `failed_login_attempts` | int | нет | `0` | Неудачные попытки (пароль и TOTP). Сброс при успешном входе и при сбросе пароля |
| `locked_until` | timestamptz | да | | До этого момента вход закрыт |
| `totp_secret_encrypted` | text | да | | Секрет TOTP, зашифрован (AES-GCM). Ключ в переменной окружения |
| `totp_enabled_at` | timestamptz | да | | `NULL`, пока TOTP не подтверждён кодом |
| `totp_last_used_step` | bigint | да | | Последний принятый 30-секундный шаг TOTP (защита от повтора кода) |
| `last_login_at` | timestamptz | да | | Последний успешный вход |
| `deleted_at` | timestamptz | да | | Мягкое удаление. `NULL` значит запись активна |
| `created_at` | timestamptz | нет | `now()` | |
| `updated_at` | timestamptz | нет | `now()` | Обновляется приложением (`@LastModifiedDate`), не БД |

## Индексы и ограничения

- PK по `id`.
- `users_email_unique`: уникальный индекс по `email` с условием `WHERE deleted_at IS NULL`. Email удалённого пользователя можно использовать повторно.
- `users_email_lowercase_check`: `email = lower(email)`. Код нормализует email (`trim().lowercase()`).
- `users_status_check`: `status IN ('PENDING', 'ACTIVE', 'BLOCKED')`.
- `users_type_check` (V2): `user_type IN ('ADMIN', 'CUSTOMER')`.
- `users_failed_attempts_check`: `failed_login_attempts >= 0`.

## UserStatus

| Значение | Смысл |
|---|---|
| `PENDING` | Создан, ещё не завершил первый вход (смена пароля, настройка TOTP) |
| `ACTIVE` | Может входить |
| `BLOCKED` | Заблокирован администратором, вход запрещён. Обратимо, запись видна в списке |

Набор значений может измениться. При добавлении значения обновляй `CHECK` новой миграцией.

## Мягкое удаление

- Удаление ставит `deleted_at = now()`. Физически строка остаётся.
- Удалённый пользователь не должен входить. Его сессии отзываются.
- Удаление отличается от `BLOCKED`: заблокированный виден и может быть разблокирован, удалённый скрыт из обычных запросов.
- В JPA-сущности: `@SQLRestriction("deleted_at IS NULL")`.

## Заметки и отложенные решения

- Сброс пароля: см. [password_reset_tokens.md](password_reset_tokens.md). Токены в `users` не хранятся.
- Роли и права: отдельные таблицы, добавим после входа.
- Recovery-коды TOTP: отдельная таблица, добавим после основного потока.
- Сессии: см. [sessions.md](sessions.md). Журналы: [auth_events.md](auth_events.md), [audit_logs.md](audit_logs.md).
- TOTP обязателен для `ADMIN`, для `CUSTOMER` опционален.
- `version` для оптимистичной блокировки: решить позже.

