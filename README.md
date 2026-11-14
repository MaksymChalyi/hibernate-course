# Hibernate Course: Mini Bank

Практичні завдання до курсу Hibernate. Протягом курсу учень будує backend невеликого онлайн-банку: клієнти, мультивалютні рахунки, картки, перекази, історія операцій, звіти і REST API.

Один Maven-проєкт, 13 модулів, у кожному дві частини:

| Папка | Що всередині |
|---|---|
| `mXX-.../starter` | Заготовка. Код компілюється, а місця для роботи позначені `// TODO`. З неї починається заняття |
| `mXX-.../solution` | Повний розв'язок із тестами. Відкривати після спроби зробити самому |

## Що потрібно

- JDK 21
- IntelliJ IDEA (Maven вбудований)
- PostgreSQL 16, встановлений локально

Docker **не обов'язковий**. Якщо він є, можна підняти базу через `docker-compose.yml`, а тести модулів 12–13 самі запустять PostgreSQL у контейнері (Testcontainers). Без Docker усе працює з локальним PostgreSQL.

## Налаштування бази (один раз)

1. Встановити PostgreSQL 16:
   - **Windows / macOS:** інсталятор з [postgresql.org/download](https://www.postgresql.org/download/). Під час встановлення запам'ятай пароль користувача `postgres`. Порт — 5432.
   - **macOS альтернатива:** [Postgres.app](https://postgresapp.com/) або `brew install postgresql@16`.
   - **Linux (Ubuntu):** `sudo apt install postgresql`.
2. Створити користувача і базу курсу — виконати `setup-local-db.sql` від імені `postgres`:
   ```bash
   psql -U postgres -f setup-local-db.sql
   ```
   Або відкрити файл у pgAdmin (встановлюється разом із PostgreSQL на Windows) і натиснути Execute.

Результат: база `minibank` на `localhost:5432`, користувач і пароль `minibank`.

**З Docker (альтернатива):** `docker compose up -d` у корені — те саме без встановлення PostgreSQL. Не запускай одночасно з локальним PostgreSQL: обидва займають порт 5432.

## Запуск

1. Відкрити в IDEA: **File → Open → `pom.xml` у корені → Open as Project**.
2. Запускати класи `Main` і тести прямо з IDEA (зелена стрілка біля класу чи методу).

Перевірити всі розв'язки з терміналу:
```bash
mvn verify
```
Тести starter-модулів при цьому пропускаються: вони навмисно падають, доки завдання не зроблено. З IDEA вони запускаються звичайно.

## Як працювати з модулем

1. Прочитати лекцію в Notion.
2. Відкрити `starter`, знайти всі `TODO` (у IDEA: **View → Tool Windows → TODO**).
3. Запустити тести starter-модуля: вони показують, що ще не працює.
4. Робити завдання, доки тести не стануть зеленими.
5. Порівняти свій код із `solution`.

## Ізоляція модулів у базі

Кожен модуль працює у власній схемі PostgreSQL (`m01`, `m02`, …). Це задається в `persistence.xml` через `currentSchema` у JDBC URL і `hibernate.default_schema`. Модулі не заважають один одному, а starter і solution одного модуля працюють з тією самою схемою.

Модулі 1–10 створюють таблиці через `hibernate.hbm2ddl.auto=create` при кожному запуску: це зручно для навчання і неприйнятно для production. З модуля 11 схемою керує Flyway, а Hibernate лише перевіряє її (`validate`).

## Тести модулів 12–13: Docker чи локальна база

Клас `TestDatabase` вибирає базу автоматично:

| Docker | Де виконуються тести |
|---|---|
| Є і запущений | Новий PostgreSQL у контейнері (Testcontainers), після тестів видаляється |
| Немає | Локальний PostgreSQL, окрема схема `m12_test` / `m13_test` |

Примусово локальна база навіть за наявності Docker: VM option `-Dminibank.tests.local=true`.

## Модулі

| # | Папка | Тема |
|---|---|---|
| 1 | `m01-jdbc-to-hibernate` | Від JDBC до Hibernate |
| 2 | `m02-entity-mapping` | Базовий mapping entity |
| 3 | `m03-persistence-context` | Persistence Context і життєвий цикл |
| 4 | `m04-transactions` | Транзакції |
| 5 | `m05-relationships` | Зв'язки між entity |
| 6 | `m06-fetching` | Fetching і N+1 |
| 7 | `m07-queries` | JPQL, Criteria, Native SQL |
| 8 | `m08-inheritance` | Наслідування |
| 9 | `m09-locking` | Блокування |
| 10 | `m10-performance` | Продуктивність, batching |
| 11 | `m11-web-jakarta` | Hibernate у Servlet-застосунку |
| 12 | `m12-testing` | Тести на справжньому PostgreSQL |
| 13 | `m13-final-project` | Фінальний проєкт |

## Типові проблеми

- **`Connection refused`** — PostgreSQL не запущений. Windows: служба `postgresql-x64-16` у «Службах»; macOS: Postgres.app або `brew services start postgresql@16`.
- **`password authentication failed for user "minibank"`** — не виконано `setup-local-db.sql`.
- **`permission denied for database minibank`** — базу створено не з `owner minibank`. Виконай від `postgres`: `alter database minibank owner to minibank;`
- **Testcontainers: `client version 1.32 is too old`** — Docker Engine 29+ вимагає новішого Testcontainers. Онови `testcontainers.version` у кореневому `pom.xml` до останньої версії 1.21.x або 2.x. Або запусти тести з `-Dminibank.tests.local=true`.
- **`Unable to locate persister` / `Not an entity`** — клас не має `@Entity` або не вказаний у `persistence.xml`.
