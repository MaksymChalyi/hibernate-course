# Модуль 7. Запити: JPQL, Criteria API, Native SQL

**Що вмітимеш після модуля:**
- писати JPQL-запити з фільтрами, з'єднаннями, агрегатами і групуванням;
- безпечно передавати параметри;
- робити сторінки (пагінацію) правильно;
- повертати з запиту одразу DTO (record), а не entity;
- обирати між JPQL, Criteria API і native SQL.

**Код:** `m07-queries/starter` → `m07-queries/solution`

**Що треба пам'ятати:** SQL (`select`, `join`, `group by`); JPQL з модуля 1; `join fetch` з модуля 6; `Instant` і часові пояси з модуля 2.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | `JOIN FETCH` vs batch fetching |
| Теорія | 25 хв | Частини 1–5 |
| Практика | 25 хв | Завдання 1 (місячний оборот), 2 (сторінки) |
| **Резерв** | 30 хв | Частина 6: Criteria, native SQL, bulk update |

---

## Розбір домашнього завдання (10 хв)

Для сторінки «історія операцій» правильна відповідь — **`JOIN FETCH` або DTO-проєкція**: дані потрібні гарантовано, тож один запит кращий за «1 + кілька пачок». Batch fetching — страховка від забутих N+1, а не основний інструмент.

---

# Частина 1. JPQL докладно

## 1.1. SQL і JPQL поруч

| SQL | JPQL |
|---|---|
| `select * from transactions t` | `select t from Transaction t` |
| `where t.account_id = ?` | `where t.account.id = :id` |
| `join accounts a on a.id = t.account_id` | `join t.account a` (умову `on` Hibernate бере з mapping) |
| `where a.number = 'UA01'` | `where t.account.number = 'UA01'` (шлях через зв'язок — неявний join) |
| `t.created_at` | `t.createdAt` |

Головне: у JPQL — **класи й поля**. Hibernate перекладає їх у таблиці й колонки за анотаціями.

## 1.2. Шлях через зв'язок

```sql
select t from Transaction t where t.account.number = :number
```

`t.account.number` — «у операції взяти рахунок, у рахунку — номер». Hibernate сам додасть `join accounts`. Це зручно для простих умов. Коли треба кілька умов по пов'язаній таблиці — краще явний `join t.account a` і далі `a.xxx`.

## 1.3. Агрегати і group by

```sql
select t.type, sum(t.amount), count(t)
from Transaction t
where t.account.number = :number
group by t.type
```

| Функція | Повертає в Java |
|---|---|
| `count(t)` | `Long` |
| `sum(t.amount)` для `BigDecimal` | `BigDecimal` |
| `avg(...)` | `Double` |
| `min`, `max` | Тип поля |

Без `group by` агрегат рахується по всіх рядках; з `group by t.type` — окремо для `CREDIT` і `DEBIT`.

---

# Частина 2. Параметри

## 2.1. Іменовані параметри

```java
em.createQuery("select a from Account a where a.number = :number", Account.class)
  .setParameter("number", accountNumber)
```

`:number` — те саме, що `?` у JDBC, але з ім'ям: не треба рахувати позиції. Значення передається базі **окремо** від тексту запиту.

## 2.2. Ніколи не склеюй

```java
// SQL injection — так само небезпечно, як у JDBC
em.createQuery("select a from Account a where a.number = '" + accountNumber + "'", Account.class)
```

Параметром можна передати будь-який тип, що відповідає полю: `String`, `Instant`, enum, навіть entity (`where t.account = :account`).

---

# Частина 3. Пагінація

## 3.1. Як зробити сторінку

```java
em.createQuery("""
        select t from Transaction t
        where t.account.number = :number
        order by t.createdAt desc, t.id desc
        """, Transaction.class)
  .setParameter("number", number)
  .setFirstResult(page * size)     // OFFSET: скільки пропустити
  .setMaxResults(size)             // LIMIT: скільки взяти
  .getResultList();
```

Сторінка 0 по 20 — рядки 1–20; сторінка 1 — 21–40.

## 3.2. Сортування має бути однозначним

Якщо дві операції мають однаковий `createdAt`, база **не гарантує** їхній порядок. На сторінці 1 операція А може бути останньою, а на сторінці 2 — першою знову. Тому завжди додаємо унікальне поле в кінець: `order by t.createdAt desc, t.id desc`.

## 3.3. Межа offset-пагінації

На сторінці 10 000 база однаково пропускає 200 000 рядків — повільно. Для довгих історій використовують **keyset-пагінацію**: «дай 20 операцій, старших за останню показану» — `where t.createdAt < :lastSeen`. У нашому курсі вистачить offset.

---

# Частина 4. DTO-проєкції в record

```java
public record TypeTotal(TransactionType type, BigDecimal total, Long count) { }
```

```sql
select new ua.minibank.TypeTotal(t.type, sum(t.amount), count(t))
from Transaction t
...
group by t.type
```

- `select new` — Hibernate для кожного рядка викличе конструктор record.
- Ім'я класу — **повне**, з пакетом.
- Типи компонентів record мають збігатися з типами виразів: `count` → `Long` (не `long`, не `int`).
- Результат — звичайні об'єкти, не entity: Persistence Context їх не відстежує.

Записи звітів, рядки таблиць для API — ідеальні кандидати для DTO.

---

# Частина 5. Часові пояси в запитах

## 5.1. Задача

«Оборот за січень» — що таке «січень»? У базі `created_at` зберігається як точний момент (`Instant`). Межа місяця — це момент, який залежить від **часового поясу**: 1 лютого 00:00 у Києві — це 31 січня 22:00 за UTC (взимку Київ = UTC+2).

```java
Instant from = month.atDay(1).atStartOfDay(DemoData.KYIV).toInstant();
Instant to   = month.plusMonths(1).atDay(1).atStartOfDay(DemoData.KYIV).toInstant();
```

```sql
where t.createdAt >= :from and t.createdAt < :to      -- права межа виключна
```

## 5.2. Що піде не так з UTC

У `DemoData` є операція «Таксі» 01.02.2026 о 00:30 за Києвом. За UTC це 31.01 о 22:30. Якщо рахувати межі місяця в UTC, таксі потрапить у **січень**: січневі витрати стануть 6 700 замість 6 000. Клієнт побачить у виписці за січень поїздку, яку здійснив у лютому.

Правило: **межі днів, місяців, кварталів — завжди в часовому поясі користувача.**

**Перевір себе:**
1. Чому права межа періоду виключна?
2. Чому для пагінації потрібен `t.id` у `order by`?
3. Що повертає `count(t)` у Java?

---

## Практика (25 хв)

### Крок 0 (2 хв)

Відкрий `DemoData.java`. Знайди операції рахунку UA01 у січні. Порахуй витрати за січень **вручну** — це очікувана відповідь тесту.

### Завдання 1. monthlyTurnover (15 хв)

Кроки — над методом. Повний розв'язок:

```java
public MonthlyTurnover monthlyTurnover(String accountNumber, YearMonth month) {
    Instant from = month.atDay(1).atStartOfDay(DemoData.KYIV).toInstant();
    Instant to = month.plusMonths(1).atDay(1).atStartOfDay(DemoData.KYIV).toInstant();

    try (EntityManager em = emf.createEntityManager()) {
        List<TypeTotal> totals = em.createQuery("""
                        select new ua.minibank.TypeTotal(t.type, sum(t.amount), count(t))
                        from Transaction t
                        where t.account.number = :number
                          and t.createdAt >= :from and t.createdAt < :to
                        group by t.type
                        """, TypeTotal.class)
                .setParameter("number", accountNumber)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        BigDecimal credit = BigDecimal.ZERO;
        BigDecimal debit = BigDecimal.ZERO;
        long count = 0;
        for (TypeTotal total : totals) {
            if (total.type() == TransactionType.CREDIT) {
                credit = total.total();
            } else {
                debit = total.total();
            }
            count += total.count();
        }
        return new MonthlyTurnover(accountNumber, month, credit, debit, count);
    }
}
```

**Перевірка:** тести «1 · …». Січень UA01: надходження 20 000, витрати 6 000, 4 операції.

**Експеримент (3 хв):** заміни `DemoData.KYIV` на `ZoneOffset.UTC` → тест показує 6 700. Поверни.

### Завдання 2. findTransactions (8 хв)

```java
try (EntityManager em = emf.createEntityManager()) {
    return em.createQuery("""
                    select new ua.minibank.TransactionView(t.id, t.type, t.amount, t.description, t.createdAt)
                    from Transaction t
                    where t.account.number = :number
                    order by t.createdAt desc, t.id desc
                    """, TransactionView.class)
            .setParameter("number", accountNumber)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();
}
```

**Перевірка:** тести «2 · …». У консолі — `offset ? rows fetch first ? rows only` (так PostgreSQL-діалект Hibernate записує `LIMIT/OFFSET`).

**Якщо не працює:**

| Помилка | Причина |
|---|---|
| `Could not resolve class 'TypeTotal'` | У `select new` потрібне повне ім'я `ua.minibank.TypeTotal` |
| `Cannot instantiate ... no matching constructor` | Типи в record не збігаються з виразами (`count` → `Long`) |
| `Could not resolve attribute 'created_at'` | У JPQL — поле `createdAt`, не колонка |

---

# Частина 6. Резерв (30 хв)

## 6.1. Criteria API (12 хв)

JPQL — рядок. Зібрати його з довільного набору фільтрів («рахунок — якщо вказано, тип — якщо вказано, сума від — якщо вказано…») конкатенацією незручно й небезпечно. Criteria API будує запит **об'єктами**:

```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<TransactionView> query = cb.createQuery(TransactionView.class);
Root<Transaction> t = query.from(Transaction.class);           // from Transaction t
Join<Transaction, Account> a = t.join("account");              // join t.account a

List<Predicate> where = new ArrayList<>();
if (filter.accountNumber() != null) where.add(cb.equal(a.get("number"), filter.accountNumber()));
if (filter.type() != null)          where.add(cb.equal(t.get("type"), filter.type()));
if (filter.minAmount() != null)     where.add(cb.greaterThanOrEqualTo(t.get("amount"), filter.minAmount()));

query.select(cb.construct(TransactionView.class,
            t.get("id"), t.get("type"), t.get("amount"), t.get("description"), t.get("createdAt")))
     .where(where.toArray(Predicate[]::new))
     .orderBy(cb.desc(t.get("createdAt")), cb.desc(t.get("id")));

return em.createQuery(query).getResultList();
```

| | JPQL | Criteria |
|---|---|---|
| Статичний запит | Читабельний | Багатослівний |
| Динамічні фільтри | Склеювання рядків | Природно |

Мінус: рядки `"amount"` не перевіряє компілятор. Рішення — JPA static metamodel (`Transaction_.amount`), але це окремий крок збірки, у курсі не підключаємо.

## 6.2. Native SQL (10 хв)

Коли JPQL не вистачає: window functions, CTE, `jsonb`, специфічні функції PostgreSQL.

```java
List<Object[]> rows = em.createNativeQuery("""
        select t.id, t.amount,
               sum(case when t.type = 'CREDIT' then t.amount else -t.amount end)
                   over (order by t.created_at, t.id) as balance_after
        from transactions t
        join accounts a on a.id = t.account_id
        where a.number = :number
        order by t.created_at, t.id
        """)
        .setParameter("number", accountNumber)
        .getResultList();
```

`sum(...) over (order by ...)` — накопичена сума: для кожного рядка сума всіх попередніх і його самого. Тобто залишок після кожної операції.

Ціна native SQL:
- імена **таблиць і колонок** — перейменування поля в entity не оновить запит;
- результат — `Object[]`, перетворення вручну;
- код прив'язаний до PostgreSQL.

**Правило вибору:** JPQL за замовчуванням → Criteria для динамічних фільтрів → native для можливостей бази, яких немає в JPQL.

## 6.3. Bulk update (8 хв)

```java
em.getTransaction().begin();
int updated = em.createQuery("""
        update Account a set a.balance = a.balance - :fee
        where a.currency = :currency and a.balance >= :fee
        """)
        .setParameter("fee", fee)
        .setParameter("currency", currency)
        .executeUpdate();
em.getTransaction().commit();
```

Один `UPDATE` на всі рахунки замість завантаження кожного. Але bulk-операція йде **повз Persistence Context**:

```java
Account loaded = em.find(...);           // баланс 10 000 у пам'яті
repository.chargeMonthlyFee(UAH, 50);    // у базі вже 9 950
loaded.getBalance();                     // 10 000 — застаріле значення
em.refresh(loaded);                      // перечитати з бази → 9 950
```

Також bulk update не збільшує `@Version` (модуль 9) і не викликає `@PreUpdate` (модуль 8). Правило: bulk-операції — на початку транзакції або в окремому `EntityManager`.

---

## Словник

| Термін | Простими словами |
|---|---|
| JPQL | Мова запитів до класів і полів |
| Іменований параметр | `:name` у запиті, значення через `setParameter` |
| Пагінація | Розбиття результату на сторінки |
| DTO-проєкція | `select new` — запит одразу повертає record |
| Criteria API | Побудова запиту Java-об'єктами |
| Native SQL | «Сирий» SQL конкретної бази |
| Window function | Агрегат «поверх вікна рядків» без групування |
| Bulk update | Один `UPDATE`/`DELETE` на багато рядків |

---

## Контрольні питання

1. Чому `order by createdAt` недостатньо для пагінації?
   *Однакові значення дають нестабільний порядок між сторінками.*
2. Чому права межа періоду виключна?
   *Щоб момент рівно на межі не потрапив у два періоди.*
3. Чим DTO-проєкція відрізняється від завантаження entity?
   *Лише потрібні колонки, без Persistence Context.*
4. Коли обирати native SQL?
   *Коли потрібні можливості бази, яких немає в JPQL.*
5. Що станеться із завантаженими entity після bulk update?
   *Залишаться зі старими значеннями до `refresh` або нового `EntityManager`.*

---

## Домашнє завдання

- [ ] **3.** `topCustomersBySpending` — JPQL. Очікувано для UAH за I квартал 2026: Ivan — 10 000, Olena — 9 200.
- [ ] **4.** `topCustomersBySpendingNative` — той самий звіт native SQL.
- [ ] Тести «3 (ДЗ)» і «4 (ДЗ)» зелені.
- [ ] Письмово: чому валюта — обов'язковий параметр звіту?

## Джерела

- [Hibernate Query Language Guide (6.6)](https://docs.jboss.org/hibernate/orm/6.6/querylanguage/html_single/Hibernate_Query_Language.html)
- [Hibernate User Guide: Criteria](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#criteria)
- [Hibernate User Guide: Native SQL](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#sql)
- [PostgreSQL: Window Functions](https://www.postgresql.org/docs/16/tutorial-window.html)
