# Модуль 3. Persistence Context і життєвий цикл entity

**Що вмітимеш після модуля:**
- пояснювати, що відбувається «всередині» `EntityManager`;
- знати чотири стани об'єкта і як він переходить між ними;
- передбачати, скільки SQL-запитів виконає Hibernate, ще до запуску;
- змінювати дані без жодного `update`, і розуміти, чому це працює.

**Код:** `m03-persistence-context/starter` → `m03-persistence-context/solution`

**Чому цей модуль найважливіший.** Без нього Hibernate виглядає як магія: «я нічого не зберігав, а дані змінилися», «я зберіг, а в базі нічого», «звідки взялися 100 запитів». Після нього кожна така ситуація пояснюється за хвилину. Якщо за одне заняття не вийшло — краще повторити, ніж іти далі.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | `CurrencyConverter`, питання про `equals` для `Address` |
| Теорія | 25 хв | Частини 1–5 |
| Практика | 25 хв | Сценарії 1–5, `rename` |
| **Резерв** | 30 хв | Частина 7, `merge`, `equals/hashCode` |

---

## Розбір домашнього завдання (10 хв)

- Конвертер обробляє `null` в обидва боки?
- Усі три місця на місці: `@Converter`, `@Convert` над полем, рядок у `persistence.xml`?
- `equals` для `Address` за всіма полями: адреса — value object, у неї немає id. Дві однакові адреси — одна й та сама адреса. Клієнт має id: два клієнти з однаковими іменами — **різні** люди.

---

# Частина 1. Що таке Persistence Context

## 1.1. Невидима «мапа» всередині EntityManager

Кожен `EntityManager` тримає всередині структуру, яка називається **Persistence Context** («контекст збереження»). Найпростіше уявити її як `Map`:

```
EntityManager
└── Persistence Context
    ├── Customer#1  →  об'єкт Customer + ЗНІМОК його полів у момент завантаження
    ├── Customer#2  →  об'єкт Customer + знімок
    └── Account#7   →  об'єкт Account  + знімок
```

Ключ — тип + id. Значення — сам об'єкт і **знімок** (копія значень полів, якими вони були, коли об'єкт прийшов з бази).

Живе ця мапа рівно стільки, скільки живе `EntityManager`. Закрили `EntityManager` — мапа зникла.

## 1.2. Три наслідки цієї мапи

**Наслідок 1. Кеш першого рівня.** `em.find` спочатку дивиться в мапу і лише потім іде в базу.

```java
try (EntityManager em = emf.createEntityManager()) {
    Customer a = em.find(Customer.class, 1L);   // у мапі немає → SELECT, покласти в мапу
    Customer b = em.find(Customer.class, 1L);   // у мапі є → без SQL
}
```

**Наслідок 2. Один рядок — один об'єкт.** В одному `EntityManager` `a == b` — це **той самий** об'єкт у пам'яті, а не дві копії.

```java
System.out.println(a == b);   // true
```

**Наслідок 3. Dirty checking.** Перед записом у базу Hibernate проходить по мапі і порівнює кожен об'єкт зі знімком. Змінилось поле — Hibernate сам пише `UPDATE`.

```java
Jpa.runInTransaction(em -> {
    Customer c = em.find(Customer.class, 1L);    // SELECT + знімок {lastName = "Koval"}
    c.setLastName("Kovalenko");                  // просто змінили поле в Java
});                                              // commit → порівняння: "Koval" ≠ "Kovalenko" → UPDATE
```

Методу `update()` у JPA **немає** — він не потрібен. Це найбільший «шок» для тих, хто прийшов з JDBC.

**Перевір себе:**
1. Де Hibernate бере об'єкт при другому `find` того самого id в одному `EntityManager`?
2. Навіщо Hibernate зберігає знімок кожного об'єкта?

---

# Частина 2. Чотири стани entity

Будь-який об'єкт entity в кожен момент перебуває в одному з чотирьох станів. Стан визначається тим, **чи є об'єкт у мапі якогось EntityManager**.

| Стан | Простими словами | Чи стежить Hibernate за змінами | Як потрапити |
|---|---|---|---|
| **transient** (новий) | Щойно створений `new`, Hibernate про нього не знає | Ні | `new Customer(...)` |
| **managed** (під наглядом) | Лежить у Persistence Context відкритого `EntityManager` | **Так** | `persist`, `find`, результат запиту, результат `merge` |
| **detached** (від'єднаний) | Колись був managed, але `EntityManager` закрито або об'єкт прибрали з мапи | Ні | `em.close()`, `em.detach(obj)`, `em.clear()` |
| **removed** (на видалення) | Позначений до видалення, `DELETE` буде при flush | — | `em.remove(managed)` |

```
                persist                       close() / detach() / clear()
 transient ─────────────▶ managed ─────────────────────────────────────▶ detached
  (new)                    │   ▲                                            │
                    remove │   │ find / запит                               │
                           ▼   │                                            │
                        removed                     merge → повертає НОВУ managed-копію
                           │                                                │
                     flush │                      managed (копія) ◀─────────┘
                           ▼
                        DELETE
```

### Приклад: увесь життєвий цикл одного клієнта

```java
Customer c = new Customer("Olena", "Koval", "olena@example.com");   // transient

try (EntityManager em = emf.createEntityManager()) {
    em.getTransaction().begin();
    em.persist(c);                     // managed: тепер Hibernate за ним стежить
    c.setLastName("Kovalenko");        // зміна managed-об'єкта — потрапить у базу
    em.getTransaction().commit();      // INSERT (одразу з новим прізвищем)
}                                      // EntityManager закрито → c тепер detached

c.setLastName("Petrenko");             // detached: Hibernate про зміну не знає, у базу НЕ потрапить
```

**Перевір себе:**
1. У якому стані об'єкт, отриманий з `em.find`, після закриття `EntityManager`?
2. Чи потрапить у базу зміна поля detached-об'єкта?

---

# Частина 3. Основні операції EntityManager

| Операція | Що робить зі станом | SQL | Коли SQL виконається |
|---|---|---|---|
| `persist(new)` | transient → managed | `INSERT` | При flush (див. частину 4) |
| `find(Class, id)` | → managed | `SELECT`, якщо об'єкта немає в мапі | Одразу |
| `getReference(Class, id)` | → managed proxy-заглушка | Нічого, поки не звернутись до поля | При першому зверненні |
| запит JPQL | Результати → managed | `SELECT` | Одразу |
| `remove(managed)` | managed → removed | `DELETE` | При flush |
| `merge(detached)` | Повертає **іншу**, managed-копію | `SELECT`, якщо копії немає в мапі; `UPDATE`, якщо є зміни | `SELECT` одразу, `UPDATE` при flush |
| `detach(obj)` | managed → detached | — | — |
| `clear()` | Усі об'єкти → detached, мапа порожня | — | — |
| `contains(obj)` | Перевірити, чи managed | — | — |

### merge — найчастіше джерело помилок

```java
Customer detached = ...;                 // завантажений раніше, EntityManager закритий
detached.setEmail("new@example.com");

Customer managed = em.merge(detached);   // merge НЕ «приєднує» detached, а повертає КОПІЮ
detached.setEmail("other@example.com");  // ця зміна НЕ збережеться — detached досі detached
managed.setEmail("other@example.com");   // ця — збережеться
```

Правило: **після `merge` працюй з тим, що він повернув**.

І друге правило: **не викликай `merge` для об'єкта, який і так managed**. Об'єкт з `find` уже під наглядом — достатньо змінити поле.

---

# Частина 4. Flush: коли SQL насправді йде в базу

## 4.1. Hibernate відкладає SQL

`persist`, `remove` і зміна полів **не** виконують SQL одразу. Hibernate накопичує зміни і відправляє їх у базу пакетом. Момент відправки називається **flush** («скинути»).

```java
em.getTransaction().begin();
em.persist(customer);          // SQL ще немає
customer.setEmail("x@y.z");    // SQL ще немає
em.getTransaction().commit();  // flush: INSERT (вже з новим email) + commit транзакції
```

**Flush ≠ commit.** Flush — відправити SQL у базу всередині транзакції. Commit — зафіксувати транзакцію. Після flush транзакцію ще можна відкотити — і все, що «скинули», скасується в базі.

## 4.2. Коли відбувається flush

| Момент | Чому |
|---|---|
| Перед `commit` | Щоб зафіксувати всі зміни |
| Перед JPQL-запитом, результат якого залежить від незбережених змін | Щоб запит побачив актуальні дані |
| Явний виклик `em.flush()` | Коли треба побачити SQL / помилку бази саме зараз |

Другий пункт — режим `FlushMode.AUTO`, він увімкнений за замовчуванням:

```java
em.getTransaction().begin();
Customer c = em.find(Customer.class, 1L);                    // SELECT
c.setLastName("Changed");                                   // SQL ще немає
em.createQuery("select c from Customer c where c.lastName = 'Changed'", Customer.class)
  .getResultList();                                          // flush: UPDATE, потім SELECT
em.getTransaction().rollback();                              // UPDATE скасовано в базі
```

Якби Hibernate не зробив flush перед запитом, база не знайшла б клієнта з прізвищем `Changed` — хоча в пам'яті він уже такий. Результат був би неконсистентним. Тому Hibernate «скидає» зміни перед запитом.

Rollback скасовує зміни **в базі**, але запити вже виконані — їх кількість не зменшиться.

**Перевір себе:**
1. Чим flush відрізняється від commit?
2. Чому Hibernate робить flush перед JPQL-запитом?

---

# Частина 5. Скільки SQL? Шпаргалка

Тримай цю таблицю відкритою на практиці.

| Дія | Іде в базу? |
|---|---|
| `em.find`, об'єкта **немає** в Persistence Context | `SELECT` |
| `em.find`, об'єкт **уже є** в Persistence Context цього `EntityManager` | Ні |
| `em.find` у **новому** `EntityManager` | `SELECT` (у нового `EntityManager` своя, порожня мапа) |
| JPQL-запит | `SELECT` — завжди, кеш першого рівня для запитів не працює |
| Знайдені запитом об'єкти | Потрапляють у мапу: наступний `find` їх уже не завантажує |
| Зміна поля managed-об'єкта | `UPDATE` при flush, **лише якщо значення справді змінилось** |
| Записати в поле те саме значення | Ні: знімок і поточне значення однакові |
| `em.remove(obj)` | `DELETE` при flush. Сам `remove` потребує managed-об'єкта — зазвичай перед ним `find` |
| `em.getReference(...)` + лише `getId()` | Ні: id відомий без бази |
| `em.getReference(...)` + будь-яке інше поле | `SELECT` у момент звернення |
| `em.clear()` | Ні, але після нього мапа порожня: наступний `find` — знову `SELECT` |
| `em.merge(detached)`, копії немає в мапі | `SELECT` одразу; `UPDATE` при flush, якщо є зміни |
| Rollback | Не скасовує вже виконані запити — лише їхній ефект у базі |

---

## Практика (25 хв)

### Завдання 1. Прогноз SQL (сценарії 1–5)

**Файли:** `Scenarios.java` (читати), `Predictions.java` (писати).

**Як працюємо — однаково для кожного сценарію:**
1. Учень читає метод `sN` у `Scenarios` **вголос**.
2. Для кожного рядка каже: іде в базу чи ні (дивимось шпаргалку з частини 5).
3. Вписує число в `Predictions` і над ним — коментар-пояснення.
4. Запускає `ScenarioTest`. Зелений — переходимо далі. Червоний — читаємо консоль цього тесту, знаходимо «зайвий» або «відсутній» SQL і виправляємо **пояснення**.

**Головне правило:** не підганяти число під тест. Цінність вправи — у поясненні.

**Розбір для викладача** (учню не показувати до його прогнозу):

| # | Код | Відповідь | Пояснення |
|---|---|---|---|
| 1 | `find`, `find` в одному EM | **1** | Другий `find` бере об'єкт з Persistence Context |
| 2 | `find` у двох EM | **2** | У кожного EM своя мапа |
| 3 | `find`, `setLastName("Kovalenko")`, commit | **2** | `SELECT` + `UPDATE` від dirty checking |
| 4 | `find`, `setLastName(c.getLastName())`, commit | **1** | Значення не змінилося — знімок однаковий, `UPDATE` немає |
| 5 | `find`, змінити, JPQL, rollback | **3** | `SELECT`, AUTO flush перед запитом → `UPDATE`, `SELECT` запиту. Rollback не зменшує кількість |

### Завдання 2. rename без update

**Файл:** `CustomerService.java`. Кроки — у коментарі над методом.

```java
public void rename(Long customerId, String newLastName) {
    Jpa.runInTransaction(em -> {
        Customer customer = em.find(Customer.class, customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Клієнта " + customerId + " не знайдено");
        }
        customer.setLastName(newLastName);
    });
}
```

`Jpa.runInTransaction` — готовий helper у цьому модулі: відкриває `EntityManager`, починає транзакцію, виконує код і робить commit (або rollback при помилці). Відкрий `Jpa.java` і подивись — це той самий шаблон, що ми писали руками в модулі 1.

**Перевірка:** тест «2 · rename(): лише SELECT + UPDATE». Якщо учень додав `em.merge(customer)` «про всяк випадок» — тест пройде, але спитай: навіщо цей рядок? (Він нічого не робить: об'єкт уже managed.)

---

# Частина 6. Резерв (30 хв)

## 6.1. getReference, detach, clear (7 хв)

`getReference` повертає **proxy** — «заглушку», підклас `Customer`, у якої заповнений лише id. Запит у базу буде при першому зверненні до будь-якого поля, крім `getId()`.

Навіщо: поставити зв'язок, не завантажуючи об'єкт.

```java
Account account = em.getReference(Account.class, accountId);   // без SELECT
em.persist(new Transaction(account, ...));                      // лише INSERT з account_id
```

Якщо такого id немає в базі, `EntityNotFoundException` вилетить не одразу, а при зверненні до поля.

`detach(obj)` — прибрати один об'єкт з мапи. `clear()` — очистити всю мапу. Незбережені зміни при цьому **втрачаються**. `clear()` знадобиться в модулі 10 для масового імпорту.

## 6.2. FlushMode.COMMIT (3 хв)

`em.setFlushMode(FlushModeType.COMMIT)` — flush лише при commit. Запити трохи швидші, але можуть не бачити власних незбережених змін. Змінювати режим без вагомої причини не варто.

## 6.3. merge на практиці (8 хв, завдання 3)

```java
public Customer changeEmail(Customer detached, String newEmail) {
    detached.setEmail(newEmail);
    return Jpa.inTransaction(em -> em.merge(detached));
}
```

Тест перевіряє `assertNotSame(detached, merged)`: це два різні об'єкти.

`persist` для detached-об'єкта кидає помилку: Hibernate бачить id і вважає, що такий об'єкт уже існує.

## 6.4. equals і hashCode для entity (12 хв, завдання 4)

Вимоги до `equals`/`hashCode` для entity з генерованим id:

1. Два **нових** клієнти (id ще `null`) — різні.
2. Клієнти з однаковим id — рівні, навіть якщо завантажені різними `EntityManager` (це різні об'єкти в пам'яті).
3. `hashCode` **не змінюється**, коли після `persist` з'являється id. Інакше об'єкт «губиться» в `HashSet`: його поклали в кошик за старим hashCode, а шукають у новому.

Наївна реалізація з заготовки порушує 1 і 3:

```java
public boolean equals(Object o) { ... return Objects.equals(id, other.id); }   // null == null → «рівні»
public int hashCode() { return Objects.hash(id); }                             // змінюється з появою id
```

Правильна:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Customer other)) return false;      // instanceof: proxy — підклас Customer
    return getId() != null && getId().equals(other.getId()); // getId(): у proxy поле порожнє
}

@Override
public int hashCode() {
    return Customer.class.hashCode();                        // константа — не залежить від id
}
```

Ціна константного `hashCode`: усі клієнти в `HashSet` в одному кошику, пошук повільніший. Для десятків-сотень об'єктів це неважливо; мільйон entity в `HashSet` тримати не варто взагалі.

Альтернатива — порівнювати за незмінним бізнес-ключем (email, IBAN), якщо такий є.

---

## Словник

| Термін | Простими словами |
|---|---|
| Persistence Context | «Мапа» managed-об'єктів усередині `EntityManager` |
| Знімок (snapshot) | Копія полів у момент завантаження, для dirty checking |
| Dirty checking | Порівняння об'єктів зі знімками і автоматичний `UPDATE` |
| Flush | Відправити накопичений SQL у базу (ще не commit) |
| transient / managed / detached / removed | Чотири стани entity |
| Proxy | Заглушка-підклас, що завантажує дані при першому зверненні |

---

## Контрольні питання

1. Що таке Persistence Context і скільки він живе?
   *Мапа managed-об'єктів і знімків у `EntityManager`; живе, поки живе `EntityManager`.*
2. Чому в JPA немає `update`?
   *Зміни managed-об'єктів знаходить dirty checking при flush.*
3. Чим flush відрізняється від commit?
   *Flush відправляє SQL усередині транзакції; commit фіксує транзакцію.*
4. Що поверне `merge` і з яким об'єктом працювати далі?
   *Managed-копію; працювати з нею.*
5. Чому в сценарії 5 три запити, хоча транзакцію відкочено?
   *AUTO flush виконав UPDATE перед запитом; rollback не скасовує виконаних запитів.*
6. Чому `hashCode` entity не повинен залежати від згенерованого id?
   *Об'єкт загубиться в `HashSet` після появи id.*

---

## Домашнє завдання

- [ ] Сценарії 6–10: прогноз у `Predictions` + коментар-пояснення над кожною константою.
- [ ] Усі тести `ScenarioTest` зелені.
- [ ] Резерв, якщо не встигли: `changeEmail` і виправити `equals/hashCode`.

**Відповіді до ДЗ (для викладача):** 6 → 2 (`clear` спорожнив мапу), 7 → 0 (proxy, id відомий), 8 → 2 (`SELECT` + `DELETE`), 9 → 1 (запит поклав клієнта в мапу), 10 → 1 (`merge` завантажив копію; змін немає — `UPDATE` немає).

## Типові помилки

| Помилка | Наслідок |
|---|---|
| `em.merge(managed)` «про всяк випадок» | Зайвий код і нерозуміння стану |
| Зміни в detached-об'єкті після `merge` | Зміни мовчки втрачаються |
| `EntityManager` у полі класу, один на весь застосунок | Мапа росте, дані застарівають, не потокобезпечно |
| `equals` через `Objects.equals(id, ...)` | Нові клієнти «рівні»; об'єкт губиться в `HashSet` |

## Джерела

- [Hibernate User Guide: Persistence Context](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#pc)
- [Hibernate User Guide: Flushing](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#flushing)
- [Jakarta Persistence 3.1](https://jakarta.ee/specifications/persistence/3.1/)
