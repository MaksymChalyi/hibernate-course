# Модуль 5. Зв'язки між entity

**Мета:** змапити зв'язки без зайвих таблиць, запитів і несподіваних видалень.

**Код:** `m05-relationships/starter` → `m05-relationships/solution`

**Критерій завершення:** учень пояснює, яка сторона зв'язку записує foreign key і чому.

> Один із найщільніших модулів. Якщо практика не вміщається, перенеси `@OneToOne` і `@ManyToMany` на наступне заняття.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | Перевірки балансу й валюти |
| Теорія | 25 хв | `@ManyToOne`, `@OneToMany(mappedBy)`, owning side, helper-методи, cascade |
| Практика | 25 хв | Customer → Account → Transaction |
| **Резерв** | 30 хв | Однонаправлений `@OneToMany`, `@OneToOne`, `@ManyToMany`, `orphanRemoval`, `List` vs `Set` |

---

## Розбір домашнього завдання (10 хв)

- Перевірка валюти стоїть **до** `debit`? Якщо після — у пам'яті баланс уже змінено; транзакцію відкотить rollback, але код читається як «спочатку списали, потім передумали».
- Виняток несе зрозуміле повідомлення з номером рахунку і сумами?
- Обговори: чи варто перевіряти `amount.scale() <= 2` (не більше копійок)? Де — в entity чи сервісі?

---

## Теорія (25 хв)

### 1. Зв'язок у базі завжди один (5 хв)

```
customers            accounts
---------            --------------------
id  (PK)  ◀───────── customer_id (FK)
```

У базі зв'язок «клієнт — рахунки» — це **одна** колонка `accounts.customer_id`. В об'єктах його можна бачити з двох боків: `account.getCustomer()` і `customer.getAccounts()`. Hibernate мусить знати, яке з двох полів керує цією колонкою. Це поле — **owning side**.

### 2. `@ManyToOne` — owning side (6 хв)

```java
@Entity
public class Account {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
```

- `@ManyToOne` **завжди** owning side: foreign key лежить у таблиці «багатьох».
- `fetch = LAZY`: за замовчуванням `@ManyToOne` — **EAGER**, і кожне завантаження рахунку тягне клієнта. Детально — у модулі 6.
- `optional = false` + `nullable = false`: рахунок без клієнта не існує.
- **Жодного cascade** на `@ManyToOne`. `CascadeType.REMOVE` тут означає «видалив рахунок — видалив клієнта», а з ним і FK-помилку на інших рахунках.

### 3. `@OneToMany(mappedBy)` — inverse side (6 хв)

```java
@Entity
public class Customer {
    @OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Account> accounts = new ArrayList<>();
}
```

`mappedBy = "customer"` означає: «колонкою керує поле `customer` у класі `Account`; цей список — лише дзеркало для навігації».

**Наслідок, який треба показати учню:** якщо додати рахунок лише в `customer.getAccounts()` і не встановити `account.setCustomer(...)`, у базу піде `customer_id = NULL`. Hibernate дивиться на owning side, а не на список.

### 4. Helper-методи (4 хв)

```java
public void addAccount(Account account) {
    accounts.add(account);
    account.setCustomer(this);       // owning side — обов'язково
}

public List<Account> getAccounts() {
    return Collections.unmodifiableList(accounts);   // щоб ніхто не обійшов addAccount
}
```

`Account.setCustomer` — package-private: змінювати зв'язок можна лише через `addAccount`.

**Чому немає `removeAccount`:** у банку рахунок не видаляють — його закривають (статус `CLOSED`), бо на ньому історія операцій. Видалення з колекції в такій моделі не має бізнес-сенсу. `removeAccount` з `orphanRemoval` — у резерві, на прикладі, де він доречний.

### 5. Cascade (4 хв)

| Тип | Що переходить з батька на дітей | Для `Customer.accounts` |
|---|---|---|
| `PERSIST` | Зберегти нового клієнта → зберегти нові рахунки | ✅ Зручно |
| `MERGE` | merge клієнта → merge рахунків | ✅ |
| `REMOVE` | Видалити клієнта → видалити рахунки | ❌ Рахунки з історією не видаляють |
| `ALL` | Усе разом | ❌ Включає `REMOVE` |

Правило: cascade лише від «власника» до «частин» (агрегат), і лише ті операції, які мають бізнес-сенс.

---

## Практика (25 хв)

**TODO 1. `Account.customer`** (5 хв) — `@ManyToOne(fetch = LAZY, optional = false)`, `@JoinColumn(name = "customer_id", nullable = false)`.

**TODO 2. `Customer.accounts`** (5 хв) — прибрати `@Transient`, `@OneToMany(mappedBy = "customer", cascade = {PERSIST, MERGE})`.

**TODO 3. `addAccount`** (3 хв) — встановити обидві сторони.

**TODO 4. `Transaction.account`** (5 хв) — `@ManyToOne(fetch = LAZY, optional = false)`, колонка `account_id`.

Обговори питання з коду: **чому в `Account` немає `List<Transaction> transactions`?**
Відповідь: на рахунку можуть бути сотні тисяч операцій. Колекцію неможливо завантажити сторінками; випадкове `account.getTransactions().size()` завантажить усе. Операції рахунку отримуємо запитом із пагінацією (модуль 7). Однонаправлений `@ManyToOne` від операції до рахунку — нормальна і часто найкраща модель.

**Перевірка (7 хв):** запусти `Main` і подивись у лог.

```sql
insert into customers (...) values (...)
insert into accounts (balance, currency, customer_id, number, id) values (...)   -- cascade PERSIST
insert into accounts (...) values (...)
```

Один `persist(olena)` → три INSERT. Потім подивись на SELECT при `olena.getAccounts()`: окремий запит, бо колекція lazy.

Тести обов'язкової частини `RelationshipsTest` мають бути зеленими. Тест `removingAccountDoesNotRemoveCustomer` ловить помилку з cascade на `@ManyToOne`.

---

## Резерв (30 хв)

### Однонаправлений `@OneToMany` без `mappedBy` (5 хв)

```java
@OneToMany
private List<Account> accounts;   // без mappedBy і без @JoinColumn
```

Hibernate не знає про колонку `accounts.customer_id` і створює **проміжну таблицю** `customers_accounts`, як для many-to-many. Зайва таблиця, зайві INSERT. Тест `foreignKeyLivesInAccountsTableAndNoJoinTableIsCreated` ловить саме це. Якщо однонаправлений `@OneToMany` справді потрібен — додай `@JoinColumn(name = "customer_id")`.

### `@OneToOne`: `Account` ↔ `Card` (10 хв)

```java
@Entity
public class Card {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;
}
```

- `unique = true` робить зв'язок справді «один до одного» на рівні бази.
- **Чому немає `Account.card`:** inverse side `@OneToOne` Hibernate не може зробити lazy. Для `@ManyToOne` FK у самому рядку рахунку, і Hibernate знає, чи є зв'язок. Для inverse side FK в іншій таблиці: щоб вирішити, повернути `null` чи proxy, треба зробити SELECT. Тому кожне завантаження рахунку тягнуло б SELECT по `cards`. Якщо навігація від рахунку до картки потрібна — запит `select c from Card c where c.account = :account`.

Номер картки зберігаємо маскованим (`4441 **** **** 1234`): повний номер — дані, на які поширюються вимоги PCI DSS.

### `@ManyToMany` і чому краще проміжна entity (7 хв)

Приклад: спільні рахунки — у рахунку кілька власників, у клієнта кілька рахунків.

```java
@ManyToMany
@JoinTable(name = "account_owners", ...)
private Set<Customer> owners;
```

Працює, доки в зв'язку немає даних. Але в реальності з'являється роль (власник / довірена особа), дата надання доступу, ліміт. У `@ManyToMany` їх нікуди покласти. Тому в реальних системах роблять entity `AccountOwnership(account, customer, role, grantedAt)` з двома `@ManyToOne`.

### `orphanRemoval` vs `CascadeType.REMOVE` (5 хв)

| | `CascadeType.REMOVE` | `orphanRemoval = true` |
|---|---|---|
| Видалили батька | Діти видаляються | Діти видаляються |
| Прибрали дитину з колекції | Нічого | Дитина видаляється |

Доречно для частин, які не існують окремо: рядки замовлення, телефони клієнта. Для рахунків — ні.

### `List` vs `Set` (3 хв)

`List` без `@OrderColumn` Hibernate називає **bag**. Два bag в одному `JOIN FETCH` дають `MultipleBagFetchException` (модуль 6). `Set` цієї проблеми не має, але потребує коректних `equals`/`hashCode` (модуль 3). Практичне правило: `List` за замовчуванням; два зв'язки-колекції одним запитом не тягнемо.

---

## Контрольні питання

1. Яка сторона зв'язку Customer–Account є owning і чому?
   *`Account.customer`: foreign key `customer_id` лежить у таблиці `accounts`.*
2. Що буде в `customer_id`, якщо додати рахунок лише в `customer.getAccounts()`?
   *`NULL` — Hibernate записує FK за owning side.*
3. Чому `CascadeType.REMOVE` на `@ManyToOne` — помилка?
   *Видалення рахунку видалить клієнта.*
4. Чому в `Account` немає колекції операцій?
   *Їх може бути дуже багато; колекцію не можна завантажити сторінками.*
5. Що створить однонаправлений `@OneToMany` без `mappedBy` і `@JoinColumn`?
   *Проміжну таблицю.*

---

## Домашнє завдання

- [ ] Зробити `Card` entity (`@OneToOne`, `unique = true`), розкоментувати в `persistence.xml` і в `Main`. Тести `cardBelongsToAccount` і `secondCardForTheSameAccountIsRejected` зелені.
- [ ] Намалювати ER-діаграму (Customer, Account, Transaction, Card): для кожного зв'язку вказати owning side, fetch, cascade і обґрунтувати одним реченням.

## Типові помилки

| Симптом | Причина |
|---|---|
| `null value in column "customer_id"` | Встановили лише inverse side |
| Зайва таблиця `customers_accounts` | `@OneToMany` без `mappedBy` |
| `TransientPropertyValueException` | Зберігаєте рахунок із новим, ще не збереженим клієнтом без cascade |
| `StackOverflowError` у `toString`/`equals` | Двонаправлений зв'язок у `toString` з обох боків |

## Джерела

- [Hibernate User Guide: Associations](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#associations)
- [Jakarta Persistence 3.1: Entity Relationships (розділ 2.9)](https://jakarta.ee/specifications/persistence/3.1/)
