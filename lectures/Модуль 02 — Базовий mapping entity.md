# Модуль 2. Базовий mapping entity

**Мета:** правильно змапити таблицю на клас з урахуванням особливостей фінансових даних.

**Код:** `m02-entity-mapping/starter` → `m02-entity-mapping/solution`

**Критерій завершення:** учень пояснює, чому для `Account.balance` обрано `BigDecimal`, а для id — `SEQUENCE`.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | DAO учня на Hibernate, лог з параметрами |
| Теорія | 25 хв | Анотації колонок, вимоги до entity, генерація id, гроші, enum, `@Embeddable` |
| Практика | 25 хв | TODO 1–3: `Customer`, `Address`, `Account` |
| **Резерв** | 30 хв | UUID, дати й часові пояси, `Money`, `@Transient`, `AttributeConverter` |

---

## Розбір домашнього завдання (10 хв)

- Чи закривається `EntityManager` у кожному методі?
- Чи є rollback при помилці в методах запису?
- Чи використано JPQL з іменами класів, а не таблиць?
- Чи не потрапили в лог із параметрами чутливі дані? Обговори, чому в production цей логер вимкнений.

---

## Теорія (25 хв)

### 1. Анотації колонок (4 хв)

```java
@Entity
@Table(name = "customers")
public class Customer {
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, unique = true)
    private String email;
}
```

| Атрибут | Що робить | Коли спрацьовує |
|---|---|---|
| `name` | Ім'я колонки | Завжди |
| `nullable = false` | `NOT NULL` у DDL, Hibernate перевіряє перед INSERT | DDL і runtime |
| `length` | `varchar(100)` | Лише DDL |
| `unique = true` | Unique constraint | Лише DDL |
| `precision`, `scale` | `numeric(19,4)` | Лише DDL |

Важливо: `length` і `unique` впливають **лише на генерацію схеми**. Якщо схему створює Flyway, Hibernate не перевіряє довжину рядка — перевіряє база.

### 2. Вимоги до entity-класу (4 хв)

- конструктор без аргументів (може бути `protected`);
- клас не `final`, методи не `final` — Hibernate створює підкласи-proxy для lazy loading (модуль 6);
- є `@Id`.

**Чому records не підходять як entity:** record `final`, поля `final`, немає конструктора без аргументів. Hibernate не може створити proxy і не може змінювати поля при dirty checking. Records чудово підходять для **DTO** — побачимо в модулі 6.

**Field access:** якщо `@Id` стоїть на полі, Hibernate читає й пише поля напряму, а не через getter/setter. Тому метод `getFullName()` не стає колонкою.

### 3. Генерація id: `IDENTITY` vs `SEQUENCE` (7 хв)

| | `IDENTITY` | `SEQUENCE` |
|---|---|---|
| Хто генерує | Колонка `generated as identity` | Окремий об'єкт бази — послідовність |
| Коли id відомий | Лише після INSERT | До INSERT |
| JDBC batching вставок | **Вимикається** | Працює |
| Звернень до бази на 50 нових рядків | 50 INSERT, кожен окремо | 1 `nextval` + 50 INSERT пачкою |

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
@SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq", allocationSize = 50)
private Long id;
```

**`allocationSize = 50`**: Hibernate бере з послідовності одне значення і сам видає наступні 50 id з пам'яті. Тому id можуть мати «дірки» (1, 2, 51, 52…) — це нормально, id не повинні бути щільними.

Чому `IDENTITY` вимикає batching: після `persist` Hibernate мусить одразу знати id (щоб покласти об'єкт у Persistence Context). З `IDENTITY` id дає лише INSERT, тож Hibernate виконує INSERT негайно, по одному. Повернемось до цього в модулі 10.

### 4. Гроші: тільки `BigDecimal` (5 хв)

```java
System.out.println(0.1 + 0.2);            // 0.30000000000000004
new BigDecimal("0.1").add(new BigDecimal("0.2")); // 0.3
```

`double` — двійковий дріб: 0.1 у ньому не представляється точно. Для грошей це неприйнятно.

```java
@Column(nullable = false, precision = 19, scale = 4)
private BigDecimal balance;
```

- `precision = 19` — усього цифр, `scale = 4` — після коми. Максимум ~10¹⁵, з запасом для будь-якого рахунку.
- 4 знаки замість 2 — для проміжних розрахунків (відсотки, курси валют), щоб не накопичувати округлення.
- Порівнювати `BigDecimal` — через `compareTo`, не `equals`: `new BigDecimal("100.0").equals(new BigDecimal("100.00"))` повертає `false`.
- Створювати з рядка: `new BigDecimal("0.1")`, не `new BigDecimal(0.1)`.

### 5. Enum: `STRING`, не `ORDINAL` (2 хв)

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private CustomerStatus status;   // у базі: 'ACTIVE'
```

`ORDINAL` (за замовчуванням) зберігає номер константи: `ACTIVE = 0`, `BLOCKED = 1`. Додали `PENDING` на початок — усі збережені статуси зсунулися, і заблоковані клієнти стали активними. `STRING` займає більше місця, але читається в SQL і не ламається від перестановки.

### 6. `@Embeddable` — value object (3 хв)

```java
@Embeddable
public class Address {
    @Column(name = "city", length = 100) private String city;
    @Column(name = "street")             private String street;
    @Column(name = "postal_code", length = 10) private String postalCode;
}

@Entity
public class Customer {
    @Embedded
    private Address address;   // колонки city, street, postal_code у таблиці customers
}
```

Embeddable не має власного id і таблиці. Корисно для понять, які мають сенс лише разом: адреса, гроші (сума + валюта), період (від–до). Value object порівнюється за значенням, тому в `Address` є `equals`/`hashCode` по всіх полях.

---

## Практика (25 хв)

**TODO 1. `Customer`** (10 хв): усі анотації з коментаря в класі. Після запуску `Main` подивись у лозі DDL:

```sql
create sequence customer_seq start with 1 increment by 50
create table customers (
    birth_date date,
    registered_at timestamp(6) with time zone not null,
    ...
    status varchar(20) not null check (status in ('ACTIVE','BLOCKED','CLOSED')),
    ...
)
```

Обговори: звідки `increment by 50`, звідки `check` для статусу.

**TODO 2. `Address`** (5 хв): `@Embeddable`, колонки, конструктор.

**TODO 3. `Account`** (10 хв): `SEQUENCE`, номер IBAN 34 символи, `balance numeric(19,4)`.

**Перевірка:** `MappingTest` — усі тести обов'язкової частини зелені. Тести `currencyIsStoredAsIsoCode` і `creditLimitUsesOverriddenColumns` — для резерву.

Запитай учня: чому тест `balanceKeepsFourDigitsAfterPoint` порівнює через `compareTo`, а не `assertEquals(new BigDecimal(...), ...)`?

---

## Резерв (30 хв)

### UUID як id (5 хв)

| Плюси | Мінуси |
|---|---|
| Генерується без бази, унікальний між системами | 16 байт замість 8 |
| Не видає кількість записів (`/accounts/1042`) | Випадковий UUID v4 фрагментує B-tree індекс: вставки повільніші |
| Зручно для розподілених систем | Незручно читати й диктувати |

Практичний компроміс: внутрішній `Long id` з `SEQUENCE` + окрема колонка `public_id UUID` для зовнішнього API. Або UUID v7 (впорядкований за часом), якщо потрібен лише UUID.

### Дати й час (8 хв)

| Тип | Що означає | Колонка PostgreSQL | Приклад |
|---|---|---|---|
| `LocalDate` | Календарна дата без часу і зони | `date` | День народження |
| `Instant` | Точка на часовій шкалі (UTC) | `timestamp with time zone` | Момент операції |
| `OffsetDateTime` | Момент + зсув від UTC | `timestamp with time zone` | Момент із зсувом клієнта |
| `LocalDateTime` | Дата й час **без зони** | `timestamp` | Майже ніколи для банку |

Правило: **моменти подій — `Instant`**, календарні дати — `LocalDate`. `LocalDateTime` для операцій — помилка: «2026-03-29 03:30» у Києві — це інший момент, ніж у Лондоні, а в день переходу на літній час такого часу може взагалі не бути.

Межі «дня» чи «місяця» рахуються в конкретній зоні (`Europe/Kyiv`) — у модулі 7 побачимо, як UTC-межі псують місячний звіт.

### `Money` і `@AttributeOverrides` (7 хв)

Сума без валюти — не гроші. `Money(amount, currency)` як embeddable:

```java
@Embedded
@AttributeOverrides({
    @AttributeOverride(name = "amount",   column = @Column(name = "credit_limit_amount", precision = 19, scale = 4)),
    @AttributeOverride(name = "currency", column = @Column(name = "credit_limit_currency", length = 3))
})
private Money creditLimit;
```

`@AttributeOverrides` потрібен, коли той самий embeddable використовується двічі в одній таблиці (наприклад, `balance` і `creditLimit`): без перейменування колонки збігатимуться.

### `@Transient` і `AttributeConverter` (10 хв)

`@Transient` — поле існує в Java, але не в базі. У starter `Account.creditLimit` позначене `@Transient`, поки учень не зробить `Money` вбудованим.

`AttributeConverter` — власне перетворення значення:

```java
@Converter
public class CurrencyConverter implements AttributeConverter<Currency, String> {
    public String convertToDatabaseColumn(Currency c) { return c == null ? null : c.getCurrencyCode(); }
    public Currency convertToEntityAttribute(String code) { return code == null ? null : Currency.getInstance(code); }
}

@Convert(converter = CurrencyConverter.class)
private Currency currency;
```

Hibernate 6 мапить `java.util.Currency` і без конвертера. Конвертер потрібен, коли формат у базі нестандартний: `'Y'/'N'` замість boolean, сума в копійках, нормалізований номер телефону.

---

## Контрольні питання

1. Чому `IDENTITY` вимикає batching вставок?
   *Id відомий лише після INSERT, тож Hibernate виконує кожен INSERT одразу після `persist`.*
2. Що означає `allocationSize = 50`?
   *Одне звернення до послідовності дає Hibernate діапазон із 50 id.*
3. Чому не `double` для грошей?
   *Двійковий дріб не представляє точно десяткові частки: 0.1 + 0.2 ≠ 0.3.*
4. Що станеться з `ORDINAL`, якщо додати нову константу на початок enum?
   *Усі збережені значення зсунуться і означатимуть інші статуси.*
5. Чи стане метод `getFullName()` колонкою?
   *Ні: при field access Hibernate мапить поля, а не методи.*
6. Чим `Instant` відрізняється від `LocalDateTime`?
   *`Instant` — конкретний момент; `LocalDateTime` — «показання годинника» без зони, яке в різних зонах означає різні моменти.*

---

## Домашнє завдання

- [ ] Реалізувати `CurrencyConverter` і підключити до `Account.currency` (`@Convert`), розкоментувати в `persistence.xml`. Тест `currencyIsStoredAsIsoCode` має бути зеленим.
- [ ] Альтернатива: конвертер `PhoneNumberConverter`, що зберігає номер лише цифрами (`380671234567`), а віддає у форматі `+380 67 123 45 67`.
- [ ] Відповісти письмово: чому Hibernate не створює колонку для `getFullName()`?

## Типові помилки

| Симптом | Причина |
|---|---|
| `Could not determine recommended JdbcType for Money` | Поле типу `Money` без `@Embedded`/`@Embeddable` |
| У таблиці `status integer` | Забули `@Enumerated(EnumType.STRING)` |
| `numeric(38,2)` замість `numeric(19,4)` | Не вказали `precision`/`scale` |
| `PropertyValueException: not-null property references a null` | `nullable = false`, а значення `null` |

## Джерела

- [Hibernate User Guide: Domain Model](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#domain-model)
- [Hibernate User Guide: Identifier generators](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#identifiers-generators)
- [PostgreSQL: Date/Time Types](https://www.postgresql.org/docs/16/datatype-datetime.html)
- [PostgreSQL: Numeric Types](https://www.postgresql.org/docs/16/datatype-numeric.html)
