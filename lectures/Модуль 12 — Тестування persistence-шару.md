# Модуль 12. Тестування persistence-шару

**Мета:** тести на справжньому PostgreSQL, а не на H2.

**Код:** `m12-testing/starter` → `m12-testing/solution`

**Критерій завершення:** `mvn test` проходить на машині учня: з локальним PostgreSQL або з Docker.

> Docker у цьому модулі не обов'язковий. Готовий клас `TestDatabase` сам вибирає базу: Docker-контейнер, якщо Docker є, інакше локальний PostgreSQL. Тема Testcontainers залишається в лекції, щоб після вивчення Docker учень лише запустив тести ще раз — без змін у коді.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | `POST /statements`, міграція V3 |
| Теорія | 25 хв | Чому не H2, де взяти тестовий PostgreSQL, базовий клас, ізоляція тестів |
| Практика | 25 хв | `PostgresTestBase`, тести `TransferService` |
| **Резерв** | 30 хв | Тест на кількість SQL, builders тестових даних, тестова піраміда |

---

## Розбір домашнього завдання (10 хв)

- Нова міграція — новий файл; V1 і V2 не змінені.
- Одна транзакція на файл, `flush`/`clear` пачками, `jdbc.batch_size` у `persistence.xml`.
- Що повертає ендпоінт при помилці в рядку 5 000? Якщо 500 і половина даних у базі — повернути до модуля 4.

---

## Теорія (25 хв)

### 1. Чому не H2 (6 хв)

H2 in-memory — швидко і нічого не треба встановлювати, але це **інша база**:

| Що перевіряємо | H2 | PostgreSQL |
|---|---|---|
| Native SQL з window functions, `jsonb`, `for no key update` | Інший діалект або немає | ✅ |
| `timestamp with time zone`, точність `numeric` | Відрізняється | ✅ |
| Блокування рядків, deadlock | Інша модель блокувань | ✅ |
| Міграції Flyway, написані для PostgreSQL | Можуть не виконатися | ✅ |

Тест, зелений на H2, нічого не гарантує для production на PostgreSQL. Найцінніші тести persistence-шару — саме ті, що H2 не відтворить: конкурентність, native-запити, міграції.

### 2. Де взяти справжній PostgreSQL для тестів (7 хв)

| Варіант | Як | Плюси | Мінуси |
|---|---|---|---|
| **Окрема схема в локальній базі** | `localhost:5432/minibank`, схема `m12_test` | Нічого додатково не потрібно | Залежить від того, що встановлено на машині; версія PostgreSQL може відрізнятися від production |
| **Testcontainers** | Новий PostgreSQL у Docker-контейнері на кожен запуск тестів | Повна ізоляція, однакова версія у всіх, працює в CI | Потрібен Docker |

У проєкті обидва варіанти зібрані в `TestDatabase`:

```java
public static TestDatabase start(String localSchema) {
    boolean forceLocal = Boolean.getBoolean("minibank.tests.local");
    if (!forceLocal && dockerAvailable()) {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        postgres.start();
        return new TestDatabase(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), "public", true);
    }
    return new TestDatabase("jdbc:postgresql://localhost:5432/minibank?currentSchema=" + localSchema,
            "minibank", "minibank", localSchema, false);
}
```

Головне правило однакове для обох варіантів: **тести не чіпають робочі дані**. Окрема схема `m12_test` не перетинається зі схемою `m12`, у якій працює `Main`.

**Testcontainers коротко (для майбутнього):** бібліотека з тесту запускає Docker-контейнер з PostgreSQL на випадковому порту, а після тестів службовий контейнер **Ryuk** його видаляє, навіть якщо JVM упала. Коли учень вивчить Docker, достатньо запустити тести ще раз — у консолі з'явиться `[TestDatabase] PostgreSQL у Docker: ...`.

### 3. Базовий клас тестів (6 хв)

```java
public abstract class PostgresTestBase {
    protected static final TestDatabase DB = TestDatabase.start("m12_test");
    protected static final EntityManagerFactory EMF;

    static {
        Flyway.configure()
              .dataSource(DB.url(), DB.user(), DB.password())
              .schemas(DB.schema())
              .load()
              .migrate();
        EMF = Persistence.createEntityManagerFactory("minibank", Map.of(
                "jakarta.persistence.jdbc.url", DB.url(),
                "jakarta.persistence.jdbc.user", DB.user(),
                "jakarta.persistence.jdbc.password", DB.password(),
                "hibernate.default_schema", DB.schema()));
    }
}
```

- `static`-блок виконується **один раз на JVM**: усі тестові класи використовують ту саму базу і фабрику. Із контейнером це економить 2–5 секунд старту на кожному класі; з локальною базою — повторне виконання міграцій.
- Схему створює Flyway **тими самими міграціями**, що й у production. `hbm2ddl=validate` перевіряє, що entity відповідають міграціям — це вже окремий корисний тест.

### 4. Ізоляція тестів (6 хв)

Тести не повинні залежати від порядку запуску. Варіанти:

| Підхід | Плюси | Мінуси |
|---|---|---|
| `TRUNCATE` перед кожним тестом | Просто, швидко, працює з будь-якими транзакціями | Треба перелічити таблиці |
| Rollback транзакції після тесту | Найшвидше | Не працює, якщо код сам комітить (а наш комітить) і з кількома потоками |
| Нова база на кожен тест | Повна ізоляція | Дуже повільно |

Наш код керує транзакціями сам, тому `TRUNCATE`:

```java
@BeforeEach
void cleanDatabase() {
    Tx.runInTransaction(EMF, em ->
        em.createNativeQuery("truncate table transactions, accounts, customers").executeUpdate());
}
```

Чому очищаємо **перед** тестом, а не після: якщо тест упав посередині, «після» може не виконатися, і наступний тест стартує з чужими даними.

---

## Практика (25 хв)

**TODO 1–3. `PostgresTestBase`** (10 хв) — Flyway, фабрика, очищення. `TestDatabase` уже готовий.

Після запуску в консолі видно, яку базу взято:

```
[TestDatabase] Docker недоступний або вимкнений — локальний PostgreSQL, схема m12_test
```

Відкрий схему `m12_test` у pgAdmin / DBeaver / IDEA: таблиці створив Flyway, є таблиця `flyway_schema_history`.

**TODO 4–6. `TransferServiceTest`** (15 хв). Перший тест — готовий приклад. Решту учень пише сам за шаблоном **дані → дія → перевірка**:

```java
@Test
void insufficientFundsChangesNothing() {
    TestData.customer().account("UA01", "100").account("UA02", "0").save(EMF);
    Long from = TestData.accountId(EMF, "UA01");
    Long to = TestData.accountId(EMF, "UA02");

    assertThrows(InsufficientFundsException.class, () -> service.transfer(from, to, new BigDecimal("100.01")));

    assertMoney("100", TestData.balance(EMF, "UA01"));
    assertMoney("0", TestData.balance(EMF, "UA02"));
    assertEquals(0L, transactionCount());
}
```

Тест на паралельні перекази (50 переказів, 8 потоків) — той, що на H2 нічого б не довів.

---

## Резерв (30 хв)

### Регресійний тест на N+1 (12 хв)

```java
@Test
void historyUsesOneQuery() {
    TestData.CustomerBuilder builder = TestData.customer();
    for (int i = 0; i < 10; i++) builder.accountWithTransactions("UA1" + i, 5);
    Long customerId = builder.save(EMF);
    statistics().clear();                       // лічимо лише дію, не підготовку

    List<TransactionView> history = service.customerHistory(customerId);

    assertEquals(50, history.size());
    assertEquals(1, statistics().getPrepareStatementCount());
}
```

Перевірка тесту: прибери `fetch` у `ReportService` — тест має впасти з 11 запитами. Тест, який ніколи не падав, нічого не доводить.

Даних має бути достатньо, щоб N+1 проявився: з одним рахунком різниця між 1 і 2 запитами непомітна.

### Builders тестових даних (10 хв)

```java
Long customerId = TestData.customer()
        .named("Ivan", "Petrenko")
        .account("UA01", "1000")
        .account("UA02", CurrencyCode.USD, "50")
        .save(EMF);
```

Тест описує лише те, що для нього важливо; решта полів — розумні значення за замовчуванням (унікальний email генерується автоматично). Без builder кожен тест містить 15 рядків створення сутностей, і за ними не видно суті.

Альтернатива — SQL-скрипти з даними. Плюс: дані видно одразу. Мінус: при зміні схеми ламаються всі скрипти, а не одне місце в builder.

### Тестова піраміда для persistence (8 хв)

| Рівень | Що перевіряє | Приклад |
|---|---|---|
| Unit без бази | Бізнес-правила entity | `Account.debit` кидає виняток |
| Інтеграційний зі справжнім PostgreSQL | Mapping, запити, транзакції, блокування | `TransferServiceTest` |
| API-тест | HTTP + усе вище | `WebApiTest` у модулях 11, 13 |

Unit-тести швидкі й численні; інтеграційні — повільніші, на критичні сценарії.

---

## Контрольні питання

1. Назви три речі, які тест на H2 не перевірить для PostgreSQL.
   *Native SQL діалекту, блокування і конкурентність, міграції.*
2. Чому тести працюють в окремій схемі `m12_test`, а не в `m12`?
   *Тести очищають таблиці; робочі дані не повинні від цього страждати.*
3. Що дає Testcontainers порівняно з локальною базою?
   *Повну ізоляцію і однакову версію PostgreSQL на будь-якій машині та в CI.*
4. Чому ми очищаємо таблиці, а не відкочуємо транзакцію після тесту?
   *Код під тестом сам комітить транзакції; rollback зовні їх не скасує.*
5. Чому `statistics().clear()` після підготовки даних?
   *Щоб рахувати лише запити тестованої дії.*
6. Як переконатися, що тест справді ловить N+1?
   *Зламати код (прибрати `fetch`) і побачити, що тест падає.*

---

## Домашнє завдання

- [ ] Тест `historyUsesOneQuery`. Перевірити, що він падає без `fetch`, і повернути `fetch`.
- [ ] Прибрати `<skipTests>true</skipTests>` у `pom.xml` starter-модуля і запустити `mvn test` з терміналу (або через вкладку Maven в IDEA: `m12-starter → Lifecycle → test`).

## Типові проблеми

| Симптом | Причина |
|---|---|
| `Connection refused` | Локальний PostgreSQL не запущений |
| `permission denied for database minibank` | Користувач `minibank` не власник бази: `alter database minibank owner to minibank;` |
| Тести проходять поодинці, падають разом | Немає очищення бази між тестами |
| (з Docker) `client version 1.32 is too old` | Docker Engine 29+ і старий Testcontainers: онови `testcontainers.version` або запусти з `-Dminibank.tests.local=true` |

## Джерела

- [Testcontainers for Java: PostgreSQL Module](https://java.testcontainers.org/modules/databases/postgres/)
- [Testcontainers: Singleton containers](https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers)
- [Flyway](https://documentation.red-gate.com/flyway)
