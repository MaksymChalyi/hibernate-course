# Mini Bank API

REST-подібний API банку на Jakarta Servlet + Hibernate + PostgreSQL. Навчальний проєкт курсу Hibernate.

## Запуск

```bash
# локальний PostgreSQL налаштовано через setup-local-db.sql (див. README курсу)
# Run ua.minibank.Main в IDEA
curl http://localhost:8080/accounts/1
```

| Метод | Шлях | Опис |
|---|---|---|
| GET | `/accounts/{id}` | Рахунок і власник |
| GET | `/accounts/{id}/transactions` | Історія: `page`, `size` (≤100), `type`, `from`, `to`, `minAmount`, `maxAmount` |
| POST | `/transfers` | Переказ: `{"fromAccountId":1,"toAccountId":3,"amount":250}` |
| POST | `/statements` | Імпорт CSV-виписки |

Помилки: `404 ACCOUNT_NOT_FOUND`, `422 INSUFFICIENT_FUNDS / CURRENCY_MISMATCH`, `400 BAD_REQUEST`, `409 CONCURRENT_UPDATE`.

Тести: `mvn verify`. Якщо Docker є — PostgreSQL піднімає Testcontainers; якщо ні — тести йдуть на локальний PostgreSQL у схему `m13_test`.

## Рішення і причини

**Шари.** Servlet розбирає HTTP → сервіс керує транзакцією і повертає DTO → entity живуть лише всередині сервісу. Servlet не бачить `EntityManager`, тому `LazyInitializationException` за межами сервісу неможлива.

**Одна транзакція на бізнес-операцію.** Переказ — два баланси і два записи `Transaction` в одній транзакції. Імпорт виписки — один файл в одній транзакції: або весь, або нічого.

**Pessimistic lock для переказів.** `SELECT ... FOR UPDATE` на обох рахунках, завжди в порядку зростання id (без deadlock). Транзакція коротка, повтори не потрібні. `@Version` на `Account` лишається другим рубежем захисту від змін в обхід сервісу.
Компроміс: під великим навантаженням на один «гарячий» рахунок запити стоять у черзі.

**DTO замість entity в API.** Контракт API не залежить від схеми бази, службові поля (`version`) не витікають, Jackson не чіпає lazy-зв'язки.

**Flyway + `hbm2ddl=validate`.** Схема змінюється лише міграціями, які проходять code review. Hibernate при старті перевіряє, що entity відповідають схемі: розбіжність ламає старт, а не запит у production.

**SEQUENCE з кроком 50.** Одне звернення до послідовності на 50 нових рядків і JDBC batching при імпорті. `IDENTITY` вимкнув би batching.

**Історія операцій.** DTO-проєкція через Criteria API: умови лише для переданих фільтрів. Три запити на сторінку незалежно від кількості операцій. Індекс `(account_id, created_at desc)` під фільтр і сортування.

## Межі поточного рішення

- Вбудований пул з'єднань Hibernate — лише для навчання. Для production: HikariCP.
- Offset-пагінація повільнішає на далеких сторінках. Для великих історій — keyset-пагінація (`where created_at < :last`).
- Перекази лише в одній валюті, без комісій і без ідемпотентності: повторний POST створить другий переказ. Для production потрібен ключ ідемпотентності.
- Демо-дані в міграції `V2` — лише для навчання.
- Автентифікації немає.
