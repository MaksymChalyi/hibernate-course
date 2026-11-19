# Модуль 12. Тестування persistence-шару

**Що вмітимеш після модуля:**
- писати інтеграційні тести на справжньому PostgreSQL, а не на H2;
- готувати тестову базу тими самими міграціями, що й застосунок;
- робити тести незалежними один від одного;
- писати тест за схемою «дані → дія → перевірка», включно з винятками і паралельним кодом;
- ловити повернення N+1 тестом на кількість SQL-запитів.

**Код:** `m12-testing/starter` → `m12-testing/solution`

**Що треба пам'ятати:** JUnit 5 (`@Test`, `assertEquals`); `Tx` з модуля 4; `PESSIMISTIC_WRITE` з модуля 9; N+1 з модуля 6; Flyway з модуля 11.

> Docker у цьому модулі не обов'язковий. Готовий клас `TestDatabase` сам вибирає базу: Docker-контейнер, якщо Docker є, інакше локальний PostgreSQL (схема `m12_test`). Коли учень вивчить Docker, достатньо запустити тести ще раз — без змін у коді.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | `POST /statements`, міграція V3 |
| Теорія | 25 хв | Частини 1–5 |
| Практика | 25 хв | Завдання 1–6 |
| **Резерв** | 30 хв | Частина 6: тест на N+1, builders, тестова піраміда |

---

## Розбір домашнього завдання (10 хв)

- Нова міграція — новий файл; V1 і V2 не змінені.
- Одна транзакція на файл, `flush`/`clear` пачками, `jdbc.batch_size` у `persistence.xml`.
- Що повертає ендпоінт при помилці в рядку 5 000? Якщо 500 і половина даних у базі — повернутися до модуля 4.

---

# Частина 1. Навіщо тестувати persistence-шар

Unit-тест `Account.debit` перевіряє правило «не можна списати більше, ніж є». Але він нічого не скаже про те, чи:

- правильно змаплені колонки (модуль 2);
- JPQL-запит повертає саме ті рядки (модуль 7);
- `rollback` справді нічого не зберігає (модуль 4);
- блокування не дає загубити гроші при паралельних переказах (модуль 9);
- не повернувся N+1 після чиєїсь «дрібної» правки (модуль 6).

Усе це перевіряється лише разом з базою. Такі тести називають **інтеграційними**.

---

# Частина 2. Чому не H2

H2 — Java-база, що живе в пам'яті: швидко і нічого не треба встановлювати. Але це **інша база**:

| Що перевіряємо | H2 | PostgreSQL |
|---|---|---|
| Native SQL: window functions, `jsonb` | Інший діалект або немає | Так |
| `timestamp with time zone`, точність `numeric` | Відрізняється | Так |
| Блокування рядків, deadlock | Інша модель блокувань | Так |
| Міграції Flyway, написані для PostgreSQL | Можуть не виконатися | Так |

Тест, зелений на H2, нічого не гарантує для production на PostgreSQL. Найцінніші тести persistence-шару — саме ті, що H2 не відтворить.

---

# Частина 3. Звідки береться тестова база

| Варіант | Як | Плюси | Мінуси |
|---|---|---|---|
| **Окрема схема в локальній базі** | `localhost:5432/minibank`, схема `m12_test` | Нічого додатково не потрібно | Залежить від того, що встановлено на машині |
| **Testcontainers** | Новий PostgreSQL у Docker-контейнері на кожен запуск | Повна ізоляція, однакова версія у всіх, працює в CI | Потрібен Docker |

Обидва варіанти вже зібрані в `TestDatabase` (готовий, писати не треба):

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

**Головне правило:** тести не чіпають робочі дані. Тести очищають таблиці — тому окрема схема `m12_test`, а не `m12`.

**Testcontainers коротко:** бібліотека з тесту запускає Docker-контейнер з PostgreSQL на випадковому порту, а після тестів службовий контейнер **Ryuk** його видаляє, навіть якщо JVM упала.

---

# Частина 4. Базовий клас тестів

## 4.1. Навіщо

Кожен тестовий клас потребує бази з таблицями і `EntityManagerFactory`. Щоб не повторювати це, виносимо в абстрактний клас, який наслідують усі тести:

```java
class TransferServiceTest extends PostgresTestBase { ... }
```

## 4.2. Що в ньому

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

Це той самий код, що в `AppContextListener` модуля 11, лише з іншою адресою.

**Чому `static`-блок:** static-поля класу ініціалізуються **один раз на JVM**. Усі тестові класи використовують ту саму фабрику: міграції виконуються один раз, а не для кожного класу. З контейнером це ще й економить кілька секунд старту на клас.

**Бонус:** таблиці створює Flyway, а Hibernate з `validate` перевіряє, що entity відповідають міграціям. Якщо хтось змінив entity і забув міграцію — впадуть усі тести одразу.

---

# Частина 5. Тести не залежать один від одного

JUnit **не гарантує порядок** запуску тестів. Якщо тест A залишив у базі рахунок `UA01`, тест B, що створює свій `UA01`, впаде на `unique` — але лише коли A запустився першим. Такі «плаваючі» тести гірші, ніж їх відсутність.

| Підхід | Плюси | Мінуси |
|---|---|---|
| `TRUNCATE` перед кожним тестом | Просто, швидко | Треба перелічити таблиці |
| Rollback транзакції після тесту | Найшвидше | Не працює, якщо код сам комітить (а наш комітить), і з кількома потоками |
| Нова база на кожен тест | Повна ізоляція | Дуже повільно |

Наш код керує транзакціями сам, тому `TRUNCATE`:

```java
@BeforeEach
void cleanDatabase() {
    Tx.runInTransaction(EMF, em ->
            em.createNativeQuery("truncate table transactions, accounts, customers").executeUpdate());
    statistics().clear();
}
```

- `@BeforeEach` — JUnit викликає метод **перед кожним** тестом.
- Три таблиці в одному `truncate`: між ними зовнішні ключі, PostgreSQL дозволяє очистити пов'язані таблиці лише разом.
- **Перед** тестом, а не після: якщо тест упав посередині, «після» може не виконатися.

## 5.1. Анатомія тесту

```java
@Test
void transferMovesMoneyAndWritesTwoTransactions() {
    // 1. ДАНІ — що є в базі до дії
    TestData.customer().account("UA01", "1000").account("UA02", "100").save(EMF);

    // 2. ДІЯ — один виклик того, що перевіряємо
    service.transfer(TestData.accountId(EMF, "UA01"), TestData.accountId(EMF, "UA02"), new BigDecimal("250"));

    // 3. ПЕРЕВІРКА — що мало статися
    assertMoney("750", TestData.balance(EMF, "UA01"));
    assertMoney("350", TestData.balance(EMF, "UA02"));
    assertEquals(2L, transactionCount());
}
```

`TestData` — готовий помічник: створює клієнта з рахунками однією строкою і дістає баланс з бази.

**Чому `assertMoney`, а не `assertEquals`:** у базі `numeric(19,4)`, тобто `1000.0000`. `new BigDecimal("1000").equals(new BigDecimal("1000.0000"))` — `false` (різний scale). `compareTo` порівнює лише значення.

## 5.2. Перевірка винятку

```java
assertThrows(InsufficientFundsException.class, () -> service.transfer(from, to, new BigDecimal("100.01")));
```

`assertThrows` виконує лямбду і перевіряє, що вона кинула виняток **саме цього типу**. Якщо не кинула — тест падає.

Але виняток — лише половина. Треба ще перевірити, що **нічого не змінилося**: баланси ті самі, операцій немає. Це перевірка, що rollback з модуля 4 справді працює.

**Перевір себе:**
1. Чому тести працюють у схемі `m12_test`, а не `m12`?
2. Чому очищаємо таблиці перед тестом, а не після?
3. Чому не можна порівнювати `BigDecimal` через `equals`?

---

## Практика (25 хв)

Список завдань у коді: **View → Tool Windows → TODO**. Короткий план — `starter/ЗАВДАННЯ.md`. Тести запускаємо з IDEA (зелена стрілка біля класу).

### Завдання 1–3. `PostgresTestBase` (8 хв)

Код — у частинах 4.2 і 5. Заміни рядок `EMF = null` на Flyway + фабрику, заповни `cleanDatabase`.

Запусти `TransferServiceTest`. Зразковий тест «1–3 · Приклад …» має стати зеленим, інші — червоні з `TODO`. У консолі видно, яку базу взято:

```
[TestDatabase] Docker недоступний або вимкнений — локальний PostgreSQL, схема m12_test
```

Відкрий схему `m12_test` у pgAdmin: таблиці створив Flyway.

### Завдання 4. Бракує коштів (5 хв)

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

### Завдання 5. Різні валюти (4 хв)

Учень пише сам за зразком завдання 4:

```java
@Test
void differentCurrenciesAreRejected() {
    TestData.customer().account("UA01", "1000").account("UA02", CurrencyCode.USD, "100").save(EMF);

    assertThrows(CurrencyMismatchException.class, () ->
            service.transfer(TestData.accountId(EMF, "UA01"), TestData.accountId(EMF, "UA02"), BigDecimal.TEN));

    assertMoney("1000", TestData.balance(EMF, "UA01"));
    assertMoney("100", TestData.balance(EMF, "UA02"));
}
```

### Завдання 6. Паралельні перекази (8 хв)

Тест, який на H2 нічого б не довів: справжні блокування рядків PostgreSQL.

```java
@Test
void concurrentTransfersKeepBalanceCorrect() throws Exception {
    TestData.customer().account("UA01", "1000").account("UA02", "0").save(EMF);
    Long from = TestData.accountId(EMF, "UA01");
    Long to = TestData.accountId(EMF, "UA02");

    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            futures.add(pool.submit(() -> service.transfer(from, to, BigDecimal.TEN)));
        }
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
    } finally {
        pool.shutdownNow();
    }

    assertMoney("500", TestData.balance(EMF, "UA01"));
    assertMoney("500", TestData.balance(EMF, "UA02"));
}
```

- `ExecutorService` з 8 потоками виконує 50 задач паралельно.
- `future.get(...)` чекає завершення задачі. Якщо в потоці був виняток, він вилетить тут — інакше тест не дізнався б про помилку.

**Експеримент (обов'язково):** у `TransferService.lock(...)` прибери `LockModeType.PESSIMISTIC_WRITE` → тест падає (на рахунку не 500, бо частина списань загубилась — lost update з модуля 9). Поверни блокування. Тест, який ніколи не падав, нічого не доводить.

**Якщо не працює:**

| Що бачиш | Причина |
|---|---|
| `NullPointerException` у `statistics()` або `new TransferService(EMF)` | Не зроблено завдання 2: `EMF` ще `null` |
| `ExceptionInInitializerError` | Виняток у static-блоці — дивись `Caused by` нижче |
| `Connection refused` | Локальний PostgreSQL не запущений |
| `duplicate key value violates unique constraint` | Не зроблено завдання 3: залишились дані з попереднього тесту |
| Тести зелені поодинці, червоні разом | Те саме — немає очищення |
| Тест 6 падає з `TimeoutException` | Deadlock або дуже повільна машина; перевір, що блокуються рахунки в однаковому порядку |

---

# Частина 6. Резерв (30 хв)

## 6.1. Завдання 7. Регресійний тест на N+1 (12 хв)

**Регресія** — коли те, що працювало, зламалося після зміни. N+1 особливо підступний: функціонально все правильно, лише повільно. Звичайний тест його не помітить.

```java
@Test
void historyUsesOneQuery() {
    TestData.CustomerBuilder builder = TestData.customer();
    for (int i = 0; i < 10; i++) {
        builder.accountWithTransactions("UA1" + i, 5);
    }
    Long customerId = builder.save(EMF);
    statistics().clear();                       // лічимо лише дію, не підготовку

    List<TransactionView> history = service.customerHistory(customerId);

    assertEquals(50, history.size());
    assertEquals(1, statistics().getPrepareStatementCount(),
            "Історія операцій має завантажуватися одним запитом. Перевір JOIN FETCH");
}
```

- Даних має бути достатньо: з одним рахунком різниця між 1 і 2 запитами непомітна.
- **Перевірка тесту:** прибери `fetch` у `ReportService` — тест має впасти з `expected: <1> but was: <11>`. Поверни `fetch`.

## 6.2. Builders тестових даних (10 хв)

```java
Long customerId = TestData.customer()
        .named("Ivan", "Petrenko")
        .account("UA01", "1000")
        .account("UA02", CurrencyCode.USD, "50")
        .save(EMF);
```

Тест описує лише те, що для нього важливо; решта полів отримує розумні значення (унікальний email генерується автоматично). Без builder кожен тест містить 15 рядків створення сутностей, і за ними не видно суті.

Альтернатива — SQL-скрипти з даними. Плюс: дані видно одразу. Мінус: при зміні схеми ламаються всі скрипти, а не одне місце в builder.

Подивись, як зроблено `TestData.CustomerBuilder`: кожен метод повертає `this`, тому виклики можна ланцюжити.

## 6.3. Тестова піраміда для persistence (8 хв)

| Рівень | Що перевіряє | Швидкість | Приклад |
|---|---|---|---|
| Unit без бази | Бізнес-правила entity | Мілісекунди | `Account.debit` кидає виняток |
| Інтеграційний з PostgreSQL | Mapping, запити, транзакції, блокування | Десятки мс на тест | `TransferServiceTest` |
| API-тест | HTTP + усе вище | Найповільніші | `WebApiTest` у модулях 11, 13 |

Unit-тестів багато, інтеграційних — на критичні сценарії (гроші, конкурентність, складні запити), API — на основні шляхи.

---

## Словник

| Термін | Простими словами |
|---|---|
| Інтеграційний тест | Тест коду разом з базою |
| Testcontainers | Бібліотека, що запускає Docker-контейнер з тесту |
| `@BeforeEach` | Метод, який JUnit викликає перед кожним тестом |
| `assertThrows` | Перевірка, що код кинув виняток потрібного типу |
| Регресійний тест | Тест, що ловить повернення старої помилки |
| Test data builder | Помічник для створення тестових даних однією строкою |

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
6. Як переконатися, що тест справді ловить помилку?
   *Зламати код (прибрати `fetch` або блокування) і побачити, що тест падає.*

---

## Домашнє завдання

- [ ] Тест `historyUsesOneQuery`. Перевірити, що він падає без `fetch`, і повернути `fetch`.
- [ ] Прибрати `<skipTests>true</skipTests>` у `pom.xml` starter-модуля і запустити тести через Maven: вкладка **Maven** в IDEA → `m12-starter` → **Lifecycle** → **test**.

## Типові проблеми

| Симптом | Причина |
|---|---|
| `Connection refused` | Локальний PostgreSQL не запущений |
| `permission denied for database minibank` | Користувач `minibank` не власник бази: `alter database minibank owner to minibank;` |
| Тести проходять поодинці, падають разом | Немає очищення бази між тестами |
| (з Docker) `client version 1.32 is too old` | Docker Engine 29+ і старий Testcontainers: онови `testcontainers.version` або запусти з `-Dminibank.tests.local=true` |

## Джерела

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Testcontainers for Java: PostgreSQL Module](https://java.testcontainers.org/modules/databases/postgres/)
- [Testcontainers: Singleton containers](https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers)
- [Flyway](https://documentation.red-gate.com/flyway)
