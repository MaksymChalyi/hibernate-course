# Модуль 6. Fetching і проблема N+1

**Мета:** уміти знайти і виправити N+1 — найпоширенішу проблему продуктивності в Hibernate.

**Код:** `m06-fetching/starter` → `m06-fetching/solution`

**Критерій завершення:** учень за логами знаходить N+1 і обирає рішення залежно від сценарію.

> Ще один щільний модуль. Якщо на практиці учень довго шукає N+1 у логах — це нормально, саме цього навику й треба. Резерв можна перенести на наступне заняття.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | `Card`, ER-діаграма |
| Теорія | 25 хв | LAZY/EAGER, proxy, `LazyInitializationException`, N+1, `JOIN FETCH`, batch fetch |
| Практика | 25 хв | TODO 1–4 в `AccountReportService` під лічильником запитів |
| **Резерв** | 30 хв | Entity graph, DTO-проєкції, дві колекції, пагінація з fetch |

---

## Розбір домашнього завдання (10 хв)

На ER-діаграмі має бути:

| Зв'язок | Owning side | Fetch | Cascade |
|---|---|---|---|
| Account → Customer | `Account.customer` | LAZY | немає |
| Customer → Accounts | inverse (`mappedBy`) | LAZY (за замовч.) | PERSIST, MERGE |
| Transaction → Account | `Transaction.account` | LAZY | немає |
| Card → Account | `Card.account` (`unique`) | LAZY | немає |

---

## Теорія (25 хв)

### 1. LAZY і EAGER (5 хв)

| Зв'язок | За замовчуванням |
|---|---|
| `@ManyToOne`, `@OneToOne` | **EAGER** |
| `@OneToMany`, `@ManyToMany` | LAZY |

- **EAGER** — пов'язаний об'єкт завантажується завжди, разом з основним (JOIN або окремим SELECT).
- **LAZY** — замість об'єкта стоїть proxy (для одного об'єкта) або незавантажена колекція; запит виконується при першому зверненні.

**Чому EAGER майже завжди погано:** рішення «завантажувати завжди» приймається в mapping, раз і для всіх запитів. Один екран потребує клієнта, десять інших — ні, але платять усі. До того ж EAGER не захищає від N+1 у JPQL-запитах — це покаже практика. Правило: **усі зв'язки LAZY**, а що завантажити — вирішує конкретний запит.

### 2. Proxy і `LazyInitializationException` (5 хв)

```java
Customer customer;
try (EntityManager em = emf.createEntityManager()) {
    customer = em.find(Customer.class, id);
}
customer.getAccounts().size();   // 💥 LazyInitializationException
```

Колекція `accounts` не завантажена, а `EntityManager`, через який її можна завантажити, уже закритий.

❌ Неправильні рішення:

- зробити колекцію EAGER;
- тримати `EntityManager` відкритим довше (Open Session in View, модуль 11);
- `hibernate.enable_lazy_load_no_trans=true` — кожне звернення відкриває нову сесію і транзакцію: N+1 у найгіршому вигляді.

✅ Правильне рішення: **завантажити все потрібне в тому самому запиті** (`JOIN FETCH`, entity graph) або повернути DTO.

### 3. N+1 (7 хв)

```java
List<Account> accounts = em.createQuery("select a from Account a", Account.class).getResultList(); // 1 запит
for (Account a : accounts) {
    a.getCustomer().getFullName();   // +1 запит на кожного нового клієнта
}
```

40 рахунків 20 клієнтів → **21 запит**. На сторінці зі 100 рядків — 101. Кожен окремий запит швидкий, але разом це сотні round-trip до бази.

**Як знайти:** у лозі `org.hibernate.SQL` бачиш однакові SELECT, що відрізняються лише параметром. У тестах — лічильник `Statistics.getPrepareStatementCount()`.

### 4. `JOIN FETCH` (5 хв)

```java
em.createQuery("select a from Account a join fetch a.customer order by a.id", Account.class)
```

Один SELECT з JOIN: рахунки разом із клієнтами. `join` без `fetch` лише фільтрує, а клієнта однаково завантажить окремо.

Для колекції:

```java
em.createQuery("select c from Customer c left join fetch c.accounts order by c.id", Customer.class)
```

`LEFT` — щоб не втратити клієнтів без рахунків. JOIN з колекцією множить рядки (клієнт із 2 рахунками — 2 рядки); Hibernate 6 сам прибирає дублікати клієнтів, `distinct` більше не потрібен.

### 5. `default_batch_fetch_size` (3 хв)

```xml
<property name="hibernate.default_batch_fetch_size" value="16"/>
```

Код не змінюється, але lazy-зв'язки довантажуються пачками: замість 20 запитів по одному клієнту — 2 запити по 16 і 4. Для 20 клієнтів: 1 + 2 = 3 запити.

| | `JOIN FETCH` | Batch fetch |
|---|---|---|
| Запитів | 1 | 1 + N / розмір пачки |
| Зміна коду | Конкретний запит | Глобальне налаштування |
| Коли | Точно знаєш, що потрібно | Страховка від забутих N+1 |

Добра практика: увімкнути `default_batch_fetch_size` глобально як страховку і **плюс** писати `JOIN FETCH` там, де відомо, що дані потрібні.

---

## Практика (25 хв)

Учень запускає `FetchingTest` у starter — тести падають з повідомленнями про кількість запитів.

**TODO 1. `Account.customer` → LAZY** (3 хв). Тест `BatchFetchTest.naiveLoopIsNPlusOneWithoutBatchFetching` показує: N+1 був і з EAGER, і з LAZY. EAGER лише переносить запити з циклу в момент виконання `getResultList()`.

**TODO 2. `accountOwnerLines`** (7 хв)

```java
em.createQuery("select a from Account a join fetch a.customer order by a.id", Account.class)
```

Тест: 1 запит замість 21.

**TODO 3. `transactionHistory`** (10 хв). Зараз: SELECT клієнта + SELECT рахунків + по SELECT операцій на кожен рахунок. Один запит:

```java
em.createQuery("""
        select t from Transaction t
        join fetch t.account a
        where a.customer.id = :customerId
        order by t.createdAt desc, t.id desc
        """, Transaction.class)
```

Зверни увагу: `a.customer.id` не потребує JOIN з `customers` — значення `customer_id` уже є в рядку рахунку.

**TODO 4. `customersWithAccounts`** (5 хв): `left join fetch c.accounts`. Тест перевіряє, що після закриття `EntityManager` можна викликати `getAccounts().size()` і що клієнтів 20, а не 40.

---

## Резерв (30 хв)

### Entity graph (7 хв)

```java
EntityGraph<Account> graph = em.createEntityGraph(Account.class);
graph.addAttributeNodes("customer");
em.createQuery("select a from Account a order by a.id", Account.class)
  .setHint("jakarta.persistence.fetchgraph", graph)
  .getResultList();
```

Те саме, що `JOIN FETCH`, але план завантаження відокремлений від тексту запиту: один запит можна виконати з різними графами. У Spring Data JPA це анотація `@EntityGraph` на методі репозиторію.

`fetchgraph` — завантажити лише вказане (решта за mapping); `loadgraph` — вказане плюс усе EAGER з mapping.

### DTO-проєкції (8 хв)

```java
em.createQuery("""
        select new ua.minibank.AccountOwnerView(a.number, c.firstName, c.lastName)
        from Account a join a.customer c
        order by a.id
        """, AccountOwnerView.class)
```

- Вибираються лише потрібні колонки.
- Об'єкти не потрапляють у Persistence Context: немає знімків і dirty checking.
- N+1 неможливий: lazy-зв'язків у record немає.

Для екранів «тільки читання» (списки, звіти, API-відповіді) DTO-проєкція часто найкраща. Entity завантажуємо, коли збираємось їх змінювати.

### Дві колекції в одному `JOIN FETCH` (8 хв)

```java
select c from Customer c left join fetch c.accounts left join fetch c.cards   // обидва List
```

- Два `List` → `MultipleBagFetchException`.
- Навіть із `Set` — **декартовий добуток**: клієнт із 10 рахунками і 5 картками дає 50 рядків. На великих даних це гірше за N+1.

Рішення: два окремі запити (спочатку клієнти з рахунками, потім ті самі клієнти з картками — Persistence Context склеїть результати), або batch fetch для другої колекції.

### Пагінація з `JOIN FETCH` колекції (7 хв)

```java
em.createQuery("select c from Customer c left join fetch c.accounts", Customer.class)
  .setMaxResults(20)
```

`LIMIT` у SQL обрізав би рядки, а не клієнтів (клієнт з 3 рахунками — 3 рядки). Тому Hibernate завантажує **всі** рядки і обрізає в пам'яті, з попередженням `HHH90003004` у лозі. На мільйоні клієнтів це OutOfMemoryError.

Рішення: спочатку сторінка id (`select c.id from Customer c order by c.id` з `setMaxResults`), потім `join fetch ... where c.id in :ids`. Щоб такі запити падали одразу, а не тихо: `hibernate.query.fail_on_pagination_over_collection_fetch=true`.

---

## Контрольні питання

1. Яке значення `fetch` за замовчуванням у `@ManyToOne` і чому його змінюють?
   *EAGER; завантажує зв'язок завжди, навіть коли він не потрібен.*
2. Чому EAGER не рятує від N+1 у JPQL?
   *JPQL виконується як написаний, а EAGER-зв'язки довантажуються окремими запитами після нього.*
3. Чим `join` відрізняється від `join fetch`?
   *`join` для фільтрації; `join fetch` ще й заповнює зв'язок у результаті.*
4. Назви три правильні способи уникнути `LazyInitializationException`.
   *`JOIN FETCH`, entity graph, DTO-проєкція.*
5. Чому пагінація з `JOIN FETCH` колекції небезпечна?
   *Hibernate обрізає сторінку в пам'яті після завантаження всіх рядків.*

---

## Домашнє завдання

- [ ] Учень отримує код із N+1 (дай свій приклад або використай наївну версію `transactionHistory` зі starter).
- [ ] Виправити двома способами: `JOIN FETCH` і `default_batch_fetch_size`. Записати кількість запитів для кожного.
- [ ] Письмово: який спосіб обрати для сторінки «історія операцій клієнта» і чому.

## Джерела

- [Hibernate User Guide: Fetching](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#fetching)
- [Hibernate User Guide: Batch fetching](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#fetching-batch)
- [Jakarta Persistence 3.1: Entity Graphs (розділ 3.7)](https://jakarta.ee/specifications/persistence/3.1/)
