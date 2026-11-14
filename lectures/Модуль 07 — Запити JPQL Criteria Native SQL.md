# Модуль 7. Запити: JPQL, Criteria API, Native SQL

**Мета:** обирати правильний інструмент для запиту.

**Код:** `m07-queries/starter` → `m07-queries/solution`

**Критерій завершення:** учень пише JPQL-запит із JOIN, параметрами і DTO-проєкцією.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | Два способи виправлення N+1 |
| Теорія | 25 хв | JPQL, параметри, агрегати, пагінація, DTO-проєкції |
| Практика | 25 хв | Місячний оборот, сторінки операцій |
| **Резерв** | 30 хв | Criteria API, native SQL з window function, bulk update |

---

## Розбір домашнього завдання (10 хв)

Очікувана відповідь щодо історії операцій: **`JOIN FETCH` або DTO-проєкція**. Batch fetch зменшує N+1 до кількох запитів, але для екрана, де дані потрібні гарантовано, краще один запит. Batch fetch — страховка, а не основне рішення.

---

## Теорія (25 хв)

### 1. Дані для модуля (2 хв)

`DemoData` створює фіксований набір: 3 клієнти, 4 рахунки, 14 операцій у січні–лютому 2026 за київським часом. Тести перевіряють точні суми, тому дані не змінювати.

### 2. JPQL: класи, поля, шляхи (6 хв)

```sql
select t from Transaction t
where t.account.number = :number          -- шлях через зв'язок: неявний JOIN
  and t.createdAt >= :from
order by t.createdAt desc
```

```sql
select c.firstName, sum(t.amount)
from Transaction t
join t.account a                           -- явний JOIN по зв'язку, без ON
join a.customer c
group by c.firstName
```

- `join t.account a` — JOIN по полю зв'язку; умову `ON` Hibernate бере з mapping.
- Агрегати: `count`, `sum`, `avg`, `min`, `max`, `group by`, `having`.
- `count(t)` повертає `Long`, `sum` від `BigDecimal` — `BigDecimal`.

### 3. Параметри і SQL injection (4 хв)

```java
// ❌ Ніколи
em.createQuery("select a from Account a where a.number = '" + number + "'")

// ✅
em.createQuery("select a from Account a where a.number = :number", Account.class)
  .setParameter("number", number)
```

Як і `PreparedStatement` у JDBC: значення передаються окремо від тексту запиту, база не може сприйняти їх як SQL. Конкатенація в JPQL так само вразлива, як у SQL.

### 4. Пагінація (4 хв)

```java
.setFirstResult(page * size)   // OFFSET
.setMaxResults(size)           // LIMIT
```

**Сортування має бути однозначним.** `order by t.createdAt desc` — якщо дві операції мають однаковий час, база може повернути їх у різному порядку на різних сторінках: одна операція з'явиться двічі, інша зникне. Тому `order by t.createdAt desc, t.id desc`.

Обмеження offset-пагінації: на сторінці 10 000 база однаково пропускає 200 000 рядків. Для дуже довгих історій — keyset-пагінація (`where t.createdAt < :lastSeen`).

### 5. DTO-проєкції в records (6 хв)

```java
public record TypeTotal(TransactionType type, BigDecimal total, Long count) { }

em.createQuery("""
        select new ua.minibank.TypeTotal(t.type, sum(t.amount), count(t))
        from Transaction t
        where t.account.number = :number
          and t.createdAt >= :from and t.createdAt < :to
        group by t.type
        """, TypeTotal.class)
```

- Потрібне **повне ім'я класу** і конструктор з типами, що відповідають виразам (`count` → `Long`).
- Результат не потрапляє в Persistence Context і не відстежується.

### 6. Межі періоду і часова зона (3 хв)

```java
Instant from = month.atDay(1).atStartOfDay(KYIV).toInstant();
Instant to   = month.plusMonths(1).atDay(1).atStartOfDay(KYIV).toInstant();
// where t.createdAt >= :from and t.createdAt < :to
```

- Права межа **виключна** (`<`): операція рівно о 00:00 1 лютого не потрапить в обидва місяці.
- Межі місяця — **за Києвом**. Операція «Таксі» 01.02 00:30 за Києвом — це 31.01 22:30 за UTC. З UTC-межами січневі витрати були б 6 700 замість 6 000.

---

## Практика (25 хв)

**TODO 1. `monthlyTurnover`** (15 хв)

```java
public MonthlyTurnover monthlyTurnover(String accountNumber, YearMonth month) {
    Instant from = month.atDay(1).atStartOfDay(DemoData.KYIV).toInstant();
    Instant to = month.plusMonths(1).atDay(1).atStartOfDay(DemoData.KYIV).toInstant();
    try (EntityManager em = emf.createEntityManager()) {
        List<TypeTotal> totals = em.createQuery("""
                select new ua.minibank.TypeTotal(t.type, sum(t.amount), count(t))
                from Transaction t
                where t.account.number = :number and t.createdAt >= :from and t.createdAt < :to
                group by t.type
                """, TypeTotal.class)
            .setParameter("number", accountNumber)
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();

        BigDecimal credit = BigDecimal.ZERO, debit = BigDecimal.ZERO;
        long count = 0;
        for (TypeTotal t : totals) {
            if (t.type() == TransactionType.CREDIT) credit = t.total(); else debit = t.total();
            count += t.count();
        }
        return new MonthlyTurnover(accountNumber, month, credit, debit, count);
    }
}
```

Очікувані значення UA01: січень — надходження 20 000, витрати 6 000, 4 операції; лютий — 20 000 / 3 200 / 3.

Експеримент: заміни `DemoData.KYIV` на `ZoneOffset.UTC` і запусти `januaryTurnoverUsesKyivTime`. Тест покаже 6 700. Поверни назад.

**TODO 2. `findTransactions`** (10 хв): DTO `TransactionView`, сортування `createdAt desc, id desc`, `setFirstResult`/`setMaxResults`. Перша сторінка по 3 — «Подарунок», «Зарплата», «Таксі».

---

## Резерв (30 хв)

### Criteria API (12 хв)

JPQL — рядок, і зібрати його з довільного набору фільтрів через конкатенацію незручно й небезпечно. Criteria API будує запит об'єктами:

```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<TransactionView> query = cb.createQuery(TransactionView.class);
Root<Transaction> t = query.from(Transaction.class);
Join<Transaction, Account> a = t.join("account");

List<Predicate> where = new ArrayList<>();
if (filter.accountNumber() != null) where.add(cb.equal(a.get("number"), filter.accountNumber()));
if (filter.type() != null)          where.add(cb.equal(t.get("type"), filter.type()));
if (filter.minAmount() != null)     where.add(cb.greaterThanOrEqualTo(t.get("amount"), filter.minAmount()));
// ...

query.select(cb.construct(TransactionView.class,
            t.get("id"), t.get("type"), t.get("amount"), t.get("description"), t.get("createdAt")))
     .where(where.toArray(Predicate[]::new))
     .orderBy(cb.desc(t.get("createdAt")), cb.desc(t.get("id")));
```

Мінус: рядки `"amount"`, `"account"` не перевіряються компілятором. Рішення — JPA static metamodel (`Transaction_.amount`), який генерує annotation processor `hibernate-jpamodelgen`. У курсі не підключаємо, щоб не ускладнювати збірку.

| | JPQL | Criteria |
|---|---|---|
| Статичний запит | ✅ Читабельно | Багатослівно |
| Динамічні фільтри | Конкатенація рядків | ✅ |

### Native SQL (10 хв)

Коли JPQL не вистачає: window functions, CTE, `jsonb`, специфічні індекси, `EXPLAIN`.

```java
em.createNativeQuery("""
        select t.id, t.amount,
               sum(case when t.type = 'CREDIT' then t.amount else -t.amount end)
                   over (order by t.created_at, t.id) as balance_after
        from transactions t
        join accounts a on a.id = t.account_id
        where a.number = :number
        order by t.created_at, t.id
        """)
  .setParameter("number", accountNumber)
  .getResultList();       // List<Object[]>
```

Ціна native SQL:

- імена **таблиць і колонок**, а не класів і полів: перейменування в entity не оновить запит;
- результат — `Object[]`, перетворення вручну;
- код прив'язаний до PostgreSQL.

Правило вибору: JPQL за замовчуванням → Criteria для динамічних фільтрів → native для можливостей бази, яких немає в JPQL.

### Bulk update і Persistence Context (8 хв)

```java
em.createQuery("""
        update Account a set a.balance = a.balance - :fee
        where a.currency = :currency and a.balance >= :fee
        """)
  .setParameter("fee", fee)
  .setParameter("currency", currency)
  .executeUpdate();     // один UPDATE на всі рахунки
```

Bulk-операція йде **повз** Persistence Context: об'єкти, уже завантажені в пам'ять, лишаються зі старими значеннями. Тест `bulkUpdateBypassesPersistenceContext` показує: до `em.refresh` баланс 10 000, після — 9 950. Правило: bulk-операцію виконувати на початку транзакції або в окремому `EntityManager`, а після неї не довіряти завантаженим об'єктам.

Також bulk update не збільшує `@Version` і не викликає `@PreUpdate` (модулі 8–9).

---

## Контрольні питання

1. Чому `order by createdAt` недостатньо для пагінації?
   *Однакові значення дають нестабільний порядок: рядки дублюються або губляться між сторінками.*
2. Чому права межа періоду виключна?
   *Щоб момент рівно на межі не потрапив у два періоди.*
3. Чим DTO-проєкція відрізняється від завантаження entity?
   *Лише потрібні колонки, без Persistence Context і dirty checking.*
4. Коли обирати native SQL?
   *Коли потрібні можливості бази, яких немає в JPQL.*
5. Що станеться із завантаженими entity після bulk update?
   *Залишаться зі старими значеннями до `refresh` або нового `EntityManager`.*

---

## Домашнє завдання

- [ ] `topCustomersBySpending` на JPQL: топ клієнтів за сумою **списань** у заданій валюті за період.
- [ ] Той самий звіт на native SQL. Тест `nativeQueryGivesTheSameResult` зелений.
- [ ] Письмово: чому валюта — обов'язковий параметр звіту? Що покаже звіт без неї?

Очікуваний результат для UAH за I квартал 2026: Ivan — 10 000, Olena — 9 200.

## Джерела

- [Hibernate Query Language Guide (6.6)](https://docs.jboss.org/hibernate/orm/6.6/querylanguage/html_single/Hibernate_Query_Language.html)
- [Hibernate User Guide: Criteria](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#criteria)
- [Hibernate User Guide: Native SQL](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#sql)
- [PostgreSQL: Window Functions](https://www.postgresql.org/docs/16/tutorial-window.html)
