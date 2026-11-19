# Модуль 5. Зв'язки між entity

**Що вмітимеш після модуля:**
- описувати зв'язки «один до багатьох», «багато до одного», «один до одного»;
- розуміти, яка сторона зв'язку записує зовнішній ключ, а яка лише «дзеркало»;
- тримати обидві сторони зв'язку синхронними;
- свідомо обирати cascade і не видаляти дані випадково.

**Код:** `m05-relationships/starter` → `m05-relationships/solution`

**Що треба пам'ятати:** зовнішній ключ (foreign key) з SQL; persist / managed з модуля 3.

> Щільний модуль. Якщо не вміщається — `@OneToOne` і `@ManyToMany` (частина 6) переносимо на наступне заняття.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | Перевірки балансу й валюти |
| Теорія | 25 хв | Частини 1–5 |
| Практика | 25 хв | Завдання 1–4 |
| **Резерв** | 30 хв | Частина 6: однонаправлений `@OneToMany`, `@OneToOne`, `@ManyToMany`, `orphanRemoval` |

---

## Розбір домашнього завдання (10 хв)

- Перевірка валют — **до** `from.debit(...)`. Rollback відкотив би все й так, але код «спочатку списали, потім передумали» читається погано і робить зайву роботу.
- `balance.compareTo(amount) < 0`, а не `balance < amount` (для `BigDecimal` оператора `<` немає).

---

# Частина 1. Як зв'язки виглядають у базі

## 1.1. Зовнішній ключ — один на зв'язок

У клієнта кілька рахунків. У базі це **одна** колонка в таблиці рахунків:

```
customers                         accounts
──────────────                    ─────────────────────────────────
id │ first_name                   id │ number │ balance │ customer_id
───┼───────────                   ───┼────────┼─────────┼────────────
 1 │ Olena          ◀───────────── 10 │ UA01   │ 1000    │ 1
                    ◀───────────── 11 │ UA02   │ 50      │ 1
 2 │ Ivan           ◀───────────── 12 │ UA03   │ 3000    │ 2
```

`accounts.customer_id` — **зовнішній ключ**: його значення — id з таблиці `customers`. База не дасть записати туди id неіснуючого клієнта.

Зверни увагу: у таблиці `customers` **немає** колонки зі списком рахунків. Реляційна база в принципі не зберігає списки в рядку. Щоб дізнатися рахунки Ольги, робимо запит: `select * from accounts where customer_id = 1`.

## 1.2. А в Java — з двох боків

В об'єктах зручно ходити в обидва боки:

```java
account.getCustomer();      // рахунок → клієнт
customer.getAccounts();     // клієнт → рахунки
```

Виходить, що в Java **два** поля описують **одну** колонку `customer_id`. Hibernate має знати, яке з двох полів «головне» — за ним він і записуватиме колонку.

---

# Частина 2. @ManyToOne — сторона, що записує ключ

```java
@Entity
public class Account {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
```

Читаємо: «**багато** рахунків (Many) належать **одному** клієнту (One)».

| Що | Значення |
|---|---|
| `@ManyToOne` | Тип зв'язку з боку рахунку |
| `@JoinColumn(name = "customer_id")` | Яка колонка зберігає зв'язок |
| `nullable = false`, `optional = false` | Рахунок без клієнта не існує |
| `fetch = FetchType.LAZY` | Не завантажувати клієнта разом з рахунком (модуль 6) |

**`@ManyToOne` завжди керує зовнішнім ключем**: колонка лежить у таблиці «багатьох». Таку сторону називають **owning side** («сторона-власник»).

Що робить Hibernate:

```java
Account a = new Account("UA01", ...);
a.setCustomer(olena);          // olena.getId() == 1
em.persist(a);
// INSERT INTO accounts (number, ..., customer_id) VALUES ('UA01', ..., 1)
//                                                                     ^ узяв з customer.getId()
```

## 2.1. Чому без cascade

`cascade` — «передай цю операцію на пов'язаний об'єкт». На `@ManyToOne` він майже завжди помилка:

```java
@ManyToOne(cascade = CascadeType.REMOVE)    // НЕ так
private Customer customer;
```

Означає: «видалив рахунок → видали клієнта». Клієнт зник би разом з одним із рахунків, а інші рахунки посилались би на неіснуючий рядок (база цього не дасть — помилка FK).

**Перевір себе:**
1. У якій таблиці лежить колонка `customer_id`?
2. Яка сторона зв'язку записує зовнішній ключ?

---

# Частина 3. @OneToMany(mappedBy) — «дзеркало»

```java
@Entity
public class Customer {

    @OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Account> accounts = new ArrayList<>();
}
```

**`mappedBy = "customer"`** означає: «колонкою керує поле `customer` у класі `Account`. Цей список — лише для зручності навігації». Таку сторону називають **inverse side** («зворотна»).

Наслідок, який треба добре запам'ятати:

> Hibernate **дивиться лише на owning side**. Зміни в списку `accounts` на SQL **не впливають**.

```java
Account a = new Account("UA01", ...);
olena.getAccounts().add(a);           // лише в список
em.persist(a);
// INSERT INTO accounts (..., customer_id) VALUES (..., NULL)   ← customer не встановлено!
// → помилка: null value in column "customer_id" violates not-null constraint
```

## 3.1. Без mappedBy — зайва таблиця

```java
@OneToMany                             // без mappedBy
private List<Account> accounts;
```

Hibernate вирішить, що це **окремий** зв'язок, не пов'язаний з `Account.customer`, і створить для нього проміжну таблицю `customers_accounts (customer_id, accounts_id)`. Тепер зв'язок записується двічі, у двох місцях. Тест «немає зайвої таблиці» ловить саме це.

## 3.2. Ініціалізуй список

```java
private List<Account> accounts = new ArrayList<>();   // не null
```

Для нового клієнта (ще не з бази) список має бути порожнім, а не `null` — інакше `addAccount` впаде з `NullPointerException`.

---

# Частина 4. Helper-метод: тримаємо обидві сторони

Щоб не забути встановити owning side, зв'язок змінюють **лише через helper-метод**:

```java
public void addAccount(Account account) {
    accounts.add(account);          // inverse side — для Java
    account.setCustomer(this);      // owning side — для бази
}

public List<Account> getAccounts() {
    return Collections.unmodifiableList(accounts);   // ззовні не додати в обхід addAccount
}
```

А в `Account`:

```java
void setCustomer(Customer customer) {   // package-private: не public
    this.customer = customer;
}
```

Тепер неправильно змінити зв'язок просто не вийде.

## 4.1. Чому в Account немає списку операцій

```java
public class Account {
    // @OneToMany(mappedBy = "account") private List<Transaction> transactions;   ← свідомо НЕ робимо
}
```

На рахунку через кілька років — десятки тисяч операцій. Колекцію Hibernate завантажує **цілком**: `account.getTransactions().size()` прочитає всі. Посторінково колекцію не завантажиш. Тому операції рахунку отримуємо **запитом** з пагінацією (модуль 7):

```java
em.createQuery("select t from Transaction t where t.account = :account order by t.createdAt desc", Transaction.class)
  .setParameter("account", account)
  .setMaxResults(20)
  .getResultList();
```

**Правило:** однонаправлений `@ManyToOne` (від «багатьох» до «одного») — нормальна і часто найкраща модель. Зворотну колекцію додаємо, лише коли вона справді потрібна і завжди невелика (рахунки клієнта — так; операції рахунку — ні).

---

# Частина 5. Cascade

`cascade` вказується на стороні **«батька»** і передає операцію на **«дітей»**.

| Тип | Що передається | Для `Customer.accounts` |
|---|---|---|
| `PERSIST` | `persist(клієнт)` → `persist` кожного нового рахунку зі списку | Так: зручно створити клієнта з рахунками одним викликом |
| `MERGE` | `merge(клієнт)` → `merge` рахунків | Так |
| `REMOVE` | `remove(клієнт)` → `remove` рахунків | **Ні**: рахунки з історією операцій не видаляють |
| `DETACH`, `REFRESH` | Відповідні операції | Рідко потрібні |
| `ALL` | Усе разом, **включно з REMOVE** | Ні |

```java
Customer olena = new Customer("Olena", "Koval", "olena@example.com");
olena.addAccount(new Account("UA01", CurrencyCode.UAH, new BigDecimal("1000")));
olena.addAccount(new Account("UA02", CurrencyCode.USD, new BigDecimal("50")));
em.persist(olena);
// INSERT customers + INSERT accounts × 2 — завдяки cascade PERSIST
```

**Правило:** cascade — лише від власника до частин, які без нього не існують, і лише ті операції, що мають бізнес-сенс.

**Перевір себе:**
1. Що станеться, якщо додати рахунок лише в `customer.getAccounts()`?
2. Чому `CascadeType.ALL` на `Customer.accounts` — погана ідея для банку?

---

## Практика (25 хв)

Порядок: **1 → 4 → 2 → 3**. Поки не зроблені 1 і 4, Hibernate не стартує: поля типу `Customer`/`Account` без анотацій він не вміє зберігати (помилка `Could not determine recommended JdbcType`).

### Завдання 1. Account.customer (5 хв)

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "customer_id", nullable = false)
private Customer customer;
```

### Завдання 4. Transaction.account (3 хв)

Те саме, колонка `account_id`.

### Завдання 2. Customer.accounts (5 хв)

Прибрати `@Transient`, додати:

```java
@OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private List<Account> accounts = new ArrayList<>();
```

### Завдання 3. addAccount (2 хв)

Додати `account.setCustomer(this);`.

### Перевірка (10 хв)

1. Тести `RelationshipsTest` 1–4 зелені. Тести «5 (ДЗ)» — червоні, це нормально.
2. Запусти `Main` і знайди в консолі:

```sql
create table m05.accounts (
    ...
    customer_id bigint not null,
    ...
)
alter table if exists m05.accounts
   add constraint FK... foreign key (customer_id) references m05.customers
```

Це зовнішній ключ, який Hibernate створив з `@JoinColumn`.

```sql
insert into m05.customers (...) values (...)
insert into m05.accounts (..., customer_id, ...) values (...)
insert into m05.accounts (..., customer_id, ...) values (...)
```

Один `em.persist(olena)` — три `insert` (cascade PERSIST).

```sql
select ... from m05.customers c1_0 where c1_0.id=?
select ... from m05.accounts a1_0 where a1_0.customer_id=?
```

Другий `select` з'являється лише в момент циклу `for (Account account : olena.getAccounts())` — колекція завантажується ліниво.

**Експеримент (якщо є час):** закоментуй `account.setCustomer(this)` і запусти тест «3 · …» та `Main`. Подивись помилку `null value in column "customer_id"`. Поверни рядок.

**Якщо не працює:**

| Помилка | Причина |
|---|---|
| `Could not determine recommended JdbcType for ...Customer` | Немає `@ManyToOne` над полем |
| `mappedBy reference an unknown target entity property` | У `mappedBy` ім'я, якого немає в `Account` (перевір регістр: `customer`) |
| `null value in column "customer_id"` | `addAccount` не встановлює owning side |
| Тест «немає зайвої таблиці» червоний | `@OneToMany` без `mappedBy` |

---

# Частина 6. Резерв (30 хв)

## 6.1. @OneToOne: Account ↔ Card (12 хв, ДЗ)

Один рахунок — одна картка.

```java
@Entity
@Table(name = "cards")
public class Card {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;
}
```

- Owning side — `Card`: колонка `account_id` у таблиці `cards`.
- `unique = true` робить зв'язок справді «один до одного» **на рівні бази**: друга картка на той самий рахунок — помилка `UNIQUE`.

**Чому немає `Account.card`.** Зворотну сторону `@OneToOne` Hibernate **не може** завантажити ліниво. Для `@ManyToOne` зовнішній ключ лежить у рядку самого об'єкта: якщо там `null` — зв'язку немає, якщо число — створюємо proxy. Для зворотної сторони ключ лежить в **іншій** таблиці: щоб вирішити, повертати `null` чи proxy, Hibernate мусить зробити `SELECT` по `cards`. Тобто кожне завантаження рахунку тягнуло б зайвий запит. Якщо потрібна картка рахунку — запит `select c from Card c where c.account = :account`.

## 6.2. @ManyToMany і чому краще проміжна entity (8 хв)

Приклад: спільний рахунок — у рахунку кілька власників, у клієнта кілька рахунків.

```java
@ManyToMany
@JoinTable(name = "account_owners",
        joinColumns = @JoinColumn(name = "account_id"),
        inverseJoinColumns = @JoinColumn(name = "customer_id"))
private Set<Customer> owners;
```

Працює, доки зв'язок — просто «пара id». Але в реальному житті з'являється: роль (власник / довірена особа), дата надання доступу, ліміт. Покласти їх у `@ManyToMany` нікуди. Тому в реальних системах роблять entity-зв'язок:

```java
@Entity
public class AccountOwnership {
    @Id @GeneratedValue private Long id;
    @ManyToOne private Account account;
    @ManyToOne private Customer customer;
    @Enumerated(EnumType.STRING) private OwnershipRole role;
    private Instant grantedAt;
}
```

## 6.3. orphanRemoval (5 хв)

| | `CascadeType.REMOVE` | `orphanRemoval = true` |
|---|---|---|
| Видалили батька | Діти видаляються | Діти видаляються |
| **Прибрали дитину зі списку** | Нічого | Дитина **видаляється** з бази |

```java
@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
private List<PhoneNumber> phones;

customer.getPhones().remove(phone);   // при commit — DELETE з phone_numbers
```

Доречно для частин, які без батька не існують: телефони клієнта, рядки замовлення. Для рахунків — ні.

## 6.4. List чи Set (5 хв)

- `List` без `@OrderColumn` Hibernate називає **bag** («мішок»). Два bag в одному `JOIN FETCH` → помилка `MultipleBagFetchException` (модуль 6).
- `Set` цієї проблеми не має, але потребує правильних `equals`/`hashCode` (модуль 3).

Практичне правило: `List` за замовчуванням, і не тягнути дві колекції одним запитом.

---

## Словник

| Термін | Простими словами |
|---|---|
| Зовнішній ключ (FK) | Колонка, що зберігає id рядка іншої таблиці |
| Owning side | Сторона зв'язку, за якою Hibernate записує FK |
| Inverse side | «Дзеркало», позначене `mappedBy`, на SQL не впливає |
| `mappedBy` | Ім'я поля на owning side, яке керує зв'язком |
| Cascade | Передача операції від батька до дітей |
| Helper-метод | Метод, що змінює обидві сторони зв'язку разом |

---

## Контрольні питання

1. Яка сторона зв'язку Customer–Account є owning і чому?
   *`Account.customer`: FK `customer_id` лежить у таблиці `accounts`.*
2. Що буде в `customer_id`, якщо додати рахунок лише в список?
   *`NULL` — Hibernate записує FK за owning side.*
3. Що станеться без `mappedBy`?
   *Hibernate створить зайву проміжну таблицю.*
4. Чому `CascadeType.REMOVE` на `@ManyToOne` — помилка?
   *Видалення рахунку видалить клієнта.*
5. Чому в `Account` немає колекції операцій?
   *Їх може бути дуже багато; колекцію не завантажиш посторінково.*
6. Чим `orphanRemoval` відрізняється від `CascadeType.REMOVE`?
   *`orphanRemoval` видаляє ще й дитину, яку прибрали зі списку.*

---

## Домашнє завдання

- [ ] Завдання 5: entity `Card` (кроки 5.1–5.5 у коді). Тести «5 (ДЗ) · …» зелені.
- [ ] ER-діаграма (папір, draw.io або dbdiagram.io): 4 таблиці, зв'язки, для кожного зв'язку — owning side, fetch, cascade і одне речення «чому».

## Джерела

- [Hibernate User Guide: Associations](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#associations)
- [Jakarta Persistence 3.1](https://jakarta.ee/specifications/persistence/3.1/)
