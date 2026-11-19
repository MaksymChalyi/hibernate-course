# Модуль 6. Fetching і проблема N+1

**Що вмітимеш після модуля:**
- розуміти, коли Hibernate завантажує пов'язані об'єкти: одразу чи «за потреби»;
- знаходити N+1 у логах — найпоширенішу проблему продуктивності в Hibernate;
- виправляти її `JOIN FETCH`, entity graph, batch fetching або DTO-проєкцією;
- розуміти причину `LazyInitializationException` і правильно її виправляти.

**Код:** `m06-fetching/starter` → `m06-fetching/solution`

**Що треба пам'ятати:** `@ManyToOne`/`@OneToMany` з модуля 5; Persistence Context і proxy з модуля 3.

> Щільний модуль. Головне — щоб учень сам знайшов N+1 у консолі. Резерв можна перенести.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | `Card`, ER-діаграма |
| Теорія | 25 хв | Частини 1–5 |
| Практика | 25 хв | Завдання 1–4 |
| **Резерв** | 30 хв | Частина 6: entity graph, DTO, дві колекції, пагінація |

---

## Розбір домашнього завдання (10 хв)

| Зв'язок | Owning side | Fetch | Cascade |
|---|---|---|---|
| Account → Customer | `Account.customer` | LAZY | немає |
| Customer → Accounts | inverse (`mappedBy`) | LAZY (за замовчуванням) | PERSIST, MERGE |
| Transaction → Account | `Transaction.account` | LAZY | немає |
| Card → Account | `Card.account` (`unique`) | LAZY | немає |

---

# Частина 1. LAZY і EAGER

## 1.1. Питання, яке Hibernate вирішує щоразу

Завантажуємо рахунок. А клієнта, якому він належить, завантажувати теж?

- **EAGER** («жадібно») — так, завжди, разом з рахунком.
- **LAZY** («ліниво») — ні. Завантажити лише тоді, коли код звернеться до `account.getCustomer().getXxx()`.

## 1.2. Значення за замовчуванням — пастка

| Зв'язок | За замовчуванням |
|---|---|
| `@ManyToOne`, `@OneToOne` | **EAGER** |
| `@OneToMany`, `@ManyToMany` | LAZY |

Тобто кожен `@ManyToOne` без `fetch = LAZY` тягне пов'язаний об'єкт завжди. Якщо в `Transaction` є `@ManyToOne Account`, а в `Account` — `@ManyToOne Customer`, то завантаження однієї операції потягне рахунок і клієнта.

## 1.3. Чому EAGER — майже завжди погано

Рішення «завантажувати завжди» приймається **в mapping**, раз і для всіх запитів. Одна сторінка показує ім'я клієнта, десять інших — ні, але платять усі. А ще EAGER **не** рятує від N+1 у JPQL (покажемо нижче).

**Правило:** усі зв'язки — `LAZY`. Що завантажити — вирішує **конкретний запит**.

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "customer_id", nullable = false)
private Customer customer;
```

---

# Частина 2. Proxy і LazyInitializationException

## 2.1. Що лежить у LAZY-полі

Коли Hibernate завантажує рахунок з LAZY-клієнтом, у поле `customer` він кладе **proxy** — «заглушку». Це підклас `Customer`, згенерований Hibernate, у якого заповнений лише id. При першому зверненні до будь-якого іншого поля proxy іде в базу і довантажує себе.

```java
Account a = em.find(Account.class, 10L);   // SELECT з accounts. customer — proxy з id = 1
a.getCustomer().getId();                    // 1 — без запиту, id відомий
a.getCustomer().getFullName();              // SELECT з customers where id = 1
```

Для колекцій (`customer.getAccounts()`) — те саме: Hibernate кладе свою реалізацію `List`, яка завантажує елементи при першому зверненні.

## 2.2. LazyInitializationException

Довантажити proxy можна лише через **відкритий** `EntityManager`:

```java
Customer customer;
try (EntityManager em = emf.createEntityManager()) {
    customer = em.find(Customer.class, 1L);
}                                           // EntityManager закрито
customer.getAccounts().size();              // LazyInitializationException: could not initialize proxy - no Session
```

Колекція не завантажена, а завантажити її вже нічим.

**Неправильні «рішення»** (часто трапляються в інтернеті):

| «Рішення» | Чому погано |
|---|---|
| Зробити колекцію `EAGER` | Вона вантажиться завжди, скрізь |
| Тримати `EntityManager` відкритим довше (Open Session in View, модуль 11) | Запити виконуються будь-де, N+1 ховається |
| `hibernate.enable_lazy_load_no_trans = true` | Кожне звернення — нове з'єднання і транзакція. N+1 у найгіршому вигляді |

**Правильне рішення:** завантажити все потрібне **в тому самому запиті** (`JOIN FETCH`, entity graph) або повернути DTO.

---

# Частина 3. N+1

## 3.1. Що це

```java
List<Account> accounts = em.createQuery("select a from Account a", Account.class).getResultList(); // 1 запит
for (Account a : accounts) {
    System.out.println(a.getNumber() + " — " + a.getCustomer().getFullName());                      // +1 на кожного клієнта
}
```

- **1** — запит списку.
- **N** — по одному запиту для кожного елемента, щоб довантажити зв'язок.

40 рахунків 20 клієнтів → 1 + 20 = **21** запит (клієнт, уже завантажений раніше, береться з Persistence Context). Сторінка зі 100 рядків — 101 запит. Кожен окремо швидкий (1–2 мс), але разом це сотні звернень по мережі.

## 3.2. Як знайти в консолі

Шукай **однакові** `select`, що відрізняються лише параметром:

```sql
select ... from accounts a1_0
select ... from customers c1_0 where c1_0.id=?
select ... from customers c1_0 where c1_0.id=?
select ... from customers c1_0 where c1_0.id=?
...                                              ← ось вони, N штук
```

У тестах — лічильник Hibernate Statistics: `stats.getPrepareStatementCount()`.

## 3.3. EAGER не рятує

Інтуїтивно: «зроблю EAGER — і клієнти прийдуть одразу». Але JPQL виконується **рівно так, як написаний**: `select a from Account a` вибирає з `accounts` без JOIN. Після цього Hibernate бачить EAGER-зв'язок і **довантажує** клієнтів окремими запитами. N+1 той самий, лише раніше.

---

# Частина 4. JOIN FETCH

## 4.1. join і join fetch — різниця

```sql
-- JPQL
select a from Account a join fetch a.customer order by a.id
```

```sql
-- SQL, який згенерує Hibernate
select a1_0.id, a1_0.number, ..., c1_0.id, c1_0.first_name, c1_0.last_name, ...
from accounts a1_0
join customers c1_0 on c1_0.id = a1_0.customer_id
order by a1_0.id
```

Один запит. Колонки клієнта прийшли разом з рахунком, і Hibernate одразу заповнив поле `customer` справжнім об'єктом.

| | `join a.customer c` | `join fetch a.customer` |
|---|---|---|
| Для чого | Фільтрувати / сортувати за полями клієнта | **Заповнити** поле `customer` в результаті |
| Колонки клієнта в SELECT | Ні | Так |
| Після запиту `a.getCustomer()` | Proxy → ще один запит | Справжній об'єкт |

## 4.2. JOIN FETCH для колекції

```sql
select c from Customer c left join fetch c.accounts order by c.id
```

- `left` — щоб не втратити клієнтів **без** рахунків (звичайний JOIN їх відкине).
- JOIN з колекцією множить рядки: клієнт з 2 рахунками — 2 рядки результату SQL. Hibernate 6 **сам** прибирає дублікати клієнтів. (У Hibernate 5 для цього писали `select distinct c`.)

## 4.3. Від якої сторони починати запит

Для історії операцій клієнта зручніше почати з **операцій**:

```sql
select t from Transaction t
join fetch t.account a
where a.customer.id = :customerId
order by t.createdAt desc, t.id desc
```

`a.customer.id` не потребує JOIN з `customers`: значення `customer_id` уже є в рядку рахунку.

---

# Частина 5. Batch fetching

```xml
<property name="hibernate.default_batch_fetch_size" value="16"/>
```

Код **не змінюється**, але LAZY-зв'язки довантажуються **пачками**: замість 20 запитів `where id = ?` — запити `where id in (?, ?, ..., ?)` по 16 id. Для 20 клієнтів: 1 + 2 = 3 запити.

| | JOIN FETCH | Batch fetching |
|---|---|---|
| Запитів | 1 | 1 + N / розмір пачки |
| Що змінюємо | Конкретний запит | Одне глобальне налаштування |
| Коли | Точно знаємо, що дані потрібні | Страховка від забутих N+1 |

Добра практика: увімкнути `default_batch_fetch_size` глобально (як страховку) **і** писати `JOIN FETCH` там, де відомо, що дані потрібні.

**Перевір себе:**
1. Скільки запитів зробить наївний цикл по 100 рахунках 50 клієнтів?
2. Чим `join` відрізняється від `join fetch`?
3. Чому EAGER не рятує від N+1 у JPQL?

---

## Практика (25 хв)

### Крок 0. Побачити проблему (3 хв)

Запусти `Main` у starter: «Наївний цикл: запитів = 21». У консолі — 20 однакових `select ... from customers where id = ?`. Учень сам рахує їх і називає проблему.

### Завдання 1. LAZY (2 хв)

`@ManyToOne(fetch = FetchType.LAZY, optional = false)`. Запусти `BatchFetchTest` до і після: кількість запитів у циклі **та сама**. LAZY лише переносить момент запитів.

### Завдання 2. accountOwnerLines (5 хв)

Змінити запит: `select a from Account a join fetch a.customer order by a.id`. Тест «2 · …» — 1 запит.

### Завдання 3. transactionHistory (10 хв)

Видалити цикл, написати один запит від `Transaction` (частина 4.3) і перетворити результат:

```java
public List<TransactionView> transactionHistory(Long customerId) {
    try (EntityManager em = emf.createEntityManager()) {
        return em.createQuery("""
                        select t from Transaction t
                        join fetch t.account a
                        where a.customer.id = :customerId
                        order by t.createdAt desc, t.id desc
                        """, Transaction.class)
                .setParameter("customerId", customerId)
                .getResultList()
                .stream()
                .map(t -> new TransactionView(t.getAccount().getNumber(), t.getType(), t.getAmount(), t.getCreatedAt()))
                .toList();
    }
}
```

`"""..."""` — text block (Java 15+): багаторядковий рядок, зручно для запитів.

### Завдання 4. customersWithAccounts (5 хв)

`select c from Customer c left join fetch c.accounts order by c.id`. Тест «4 · …»: 1 запит, 20 клієнтів.

**Якщо не працює:**

| Що бачиш | Причина |
|---|---|
| 40 клієнтів замість 20 | Hibernate 6 прибирає дублікати сам; перевір, що в тесті запит саме з `join fetch`, а не `join` |
| `LazyInitializationException` у тесті 4 | Немає `fetch` у запиті |
| `could not resolve attribute 'customers'` | Помилка в імені поля: `c.accounts`, `a.customer` |

---

# Частина 6. Резерв (30 хв)

## 6.1. Entity graph (7 хв)

```java
EntityGraph<Account> graph = em.createEntityGraph(Account.class);
graph.addAttributeNodes("customer");
em.createQuery("select a from Account a order by a.id", Account.class)
  .setHint("jakarta.persistence.fetchgraph", graph)
  .getResultList();
```

Те саме, що `JOIN FETCH`, але план завантаження **відокремлений** від тексту запиту: один запит можна виконати з різними графами. У Spring Data JPA це анотація `@EntityGraph` над методом репозиторію.

`fetchgraph` — завантажити лише вказане (решта — за mapping); `loadgraph` — вказане плюс усе EAGER з mapping.

## 6.2. DTO-проєкції (8 хв)

```java
public record AccountOwnerView(String accountNumber, String firstName, String lastName) { }

em.createQuery("""
        select new ua.minibank.AccountOwnerView(a.number, c.firstName, c.lastName)
        from Account a join a.customer c
        order by a.id
        """, AccountOwnerView.class)
```

- Вибираються **лише потрібні** колонки.
- Об'єкти не потрапляють у Persistence Context: немає знімків і dirty checking — менше пам'яті.
- N+1 неможливий: у record немає lazy-зв'язків.

**Правило:** для екранів «тільки читання» (списки, звіти, відповіді API) DTO-проєкція часто найкраща. Entity завантажуємо, коли збираємось їх **змінювати**.

## 6.3. Дві колекції в одному JOIN FETCH (8 хв)

```sql
select c from Customer c left join fetch c.accounts left join fetch c.cards
```

- Два `List` → `MultipleBagFetchException`.
- Навіть з `Set` — **декартовий добуток**: клієнт з 10 рахунками і 5 картками дає 10 × 5 = 50 рядків. На великих даних це гірше за N+1.

Рішення: два окремі запити (спочатку клієнти з рахунками, потім ті самі клієнти з картками — Persistence Context «склеїть» результати), або batch fetching для другої колекції.

## 6.4. Пагінація з JOIN FETCH колекції (7 хв)

```java
em.createQuery("select c from Customer c left join fetch c.accounts", Customer.class)
  .setMaxResults(20)
```

`LIMIT` у SQL обрізав би **рядки**, а не клієнтів (клієнт з 3 рахунками — 3 рядки). Тому Hibernate завантажує **всі** рядки і обрізає сторінку в пам'яті, з попередженням `HHH90003004` у лозі. На мільйоні клієнтів це `OutOfMemoryError`.

Рішення в два кроки: спочатку сторінка id (`select c.id from Customer c order by c.id` з `setMaxResults`), потім `left join fetch ... where c.id in :ids`. Щоб такі запити падали одразу: `hibernate.query.fail_on_pagination_over_collection_fetch = true`.

---

## Словник

| Термін | Простими словами |
|---|---|
| EAGER | Завантажувати зв'язок завжди |
| LAZY | Завантажувати при першому зверненні |
| Proxy | Заглушка-підклас з id, що довантажує себе |
| N+1 | 1 запит списку + N запитів для зв'язків |
| JOIN FETCH | JOIN, який ще й заповнює поле зв'язку |
| Batch fetching | Довантаження зв'язків пачками `where id in (...)` |
| DTO-проєкція | Запит, що одразу повертає record, а не entity |

---

## Контрольні питання

1. Яке значення `fetch` за замовчуванням у `@ManyToOne`?
   *EAGER.*
2. Чому EAGER не рятує від N+1 у JPQL?
   *JPQL виконується як написаний, EAGER-зв'язки довантажуються окремими запитами.*
3. Чим `join` відрізняється від `join fetch`?
   *`join` для фільтрації; `join fetch` ще й заповнює зв'язок.*
4. Три правильні способи уникнути `LazyInitializationException`.
   *`JOIN FETCH`, entity graph, DTO-проєкція.*
5. Чому пагінація з `JOIN FETCH` колекції небезпечна?
   *Hibernate обрізає сторінку в пам'яті після завантаження всіх рядків.*

---

## Домашнє завдання

- [ ] Поверни наївний `transactionHistory` (з циклом) і виправ N+1 **другим способом**: `hibernate.default_batch_fetch_size = 16` у `persistence.xml`. Запиши кількість запитів.
- [ ] Порівняй з `JOIN FETCH` і письмово відповідай: який спосіб обрати для сторінки «історія операцій клієнта» і чому.
- [ ] Поверни код з `JOIN FETCH`.

## Джерела

- [Hibernate User Guide: Fetching](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#fetching)
- [Hibernate User Guide: Batch fetching](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#fetching-batch)
