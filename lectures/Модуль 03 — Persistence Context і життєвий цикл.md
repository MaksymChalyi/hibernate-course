# Модуль 3. Persistence Context і життєвий цикл entity

**Мета:** найважливіший модуль курсу. Без нього решта Hibernate виглядає як магія.

**Код:** `m03-persistence-context/starter` → `m03-persistence-context/solution`

**Критерій завершення:** учень правильно прогнозує кількість SQL для 4 із 5 сценаріїв домашнього завдання.

> Якщо учень повільно прогнозує сценарії — розтягни модуль на два заняття. Усе наступне (транзакції, lazy loading, N+1, merge у веб-шарі) тримається на цьому модулі.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | `CurrencyConverter`, питання про `getFullName()` |
| Теорія | 25 хв | Стани entity, операції, first-level cache, dirty checking, flush |
| Практика | 25 хв | Сценарії 1–5: прогноз → запуск → пояснення; `rename` |
| **Резерв** | 30 хв | `getReference`, `detach`/`clear`, `FlushMode`, `merge` vs `persist`, `equals`/`hashCode` |

---

## Розбір домашнього завдання (10 хв)

- Конвертер обробляє `null` в обидва боки?
- `@Converter` + `@Convert` + запис у `persistence.xml` — усі три на місці?
- `getFullName()`: Hibernate мапить поля (бо `@Id` на полі), методи для нього не існують.

---

## Теорія (25 хв)

### 1. Persistence Context (5 хв)

Кожен `EntityManager` має **Persistence Context** — мапу «тип + id → об'єкт» і знімки початкового стану кожного об'єкта.

```
EntityManager
 └─ Persistence Context
     ├─ Customer#1 → об'єкт + знімок {firstName=Olena, lastName=Koval, ...}
     └─ Account#7  → об'єкт + знімок {...}
```

Аналогія для Java-розробника: це `Map<EntityKey, Object>` усередині `EntityManager`, яка живе, доки живе `EntityManager`.

З цього випливають три речі:

1. **First-level cache.** Двічі `find(Customer.class, 1)` → один SELECT; другий раз об'єкт береться з мапи.
2. **Гарантія ідентичності.** В одному `EntityManager` `find(…, 1) == find(…, 1)` — той самий об'єкт.
3. **Dirty checking.** При flush Hibernate порівнює кожен об'єкт зі знімком і сам пише UPDATE для змінених.

### 2. Стани entity (7 хв)

| Стан | Що це | Як потрапити |
|---|---|---|
| **transient** | Новий об'єкт, Hibernate про нього не знає | `new Customer(...)` |
| **managed** | У Persistence Context, зміни відстежуються | `persist`, `find`, запит, `merge` (повертає managed-копію) |
| **detached** | Був managed, але `EntityManager` закрито / `detach` / `clear` | `em.close()`, `em.detach(c)`, `em.clear()` |
| **removed** | Позначений до видалення, DELETE буде при flush | `em.remove(managed)` |

```
         persist                  close / detach / clear
transient ───────▶ managed ─────────────────────────────▶ detached
                    │  ▲                                   │
              remove│  └──────────── merge (копія) ◀───────┘
                    ▼
                 removed ──flush──▶ DELETE
```

### 3. Основні операції (5 хв)

| Операція | Що робить | SQL |
|---|---|---|
| `persist(new)` | transient → managed | INSERT при flush |
| `find(Class, id)` | Шукає в Persistence Context, потім у базі | SELECT, якщо немає в контексті |
| `remove(managed)` | managed → removed | DELETE при flush |
| `merge(detached)` | Копіює стан detached-об'єкта в managed-копію і **повертає копію** | SELECT, якщо копії немає в контексті; UPDATE при flush, якщо є зміни |

### 4. Dirty checking (4 хв)

```java
Jpa.runInTransaction(em -> {
    Customer c = em.find(Customer.class, id);   // SELECT
    c.setLastName("Kovalenko");                 // нічого
});                                             // commit → flush → UPDATE
```

Методу `update()` в JPA немає — і він не потрібен. Найпоширеніша помилка новачків — викликати `merge` на об'єкті, який і так managed.

### 5. Flush (4 хв)

**Flush** — момент, коли Hibernate відправляє накопичені зміни в базу (INSERT/UPDATE/DELETE). Це **не commit**: після flush транзакція ще може відкотитися.

Flush відбувається:

- перед `commit`;
- перед JPQL-запитом, результат якого залежить від незбережених змін (`FlushMode.AUTO`);
- при явному `em.flush()`.

---

## Практика (25 хв)

### Сценарії (20 хв)

Правило: **спочатку прогноз, потім запуск**. Учень читає метод у `Scenarios`, записує число в `Predictions`, пояснює вголос, і лише тоді запускає `ScenarioTest`.

| # | Сценарій | Відповідь | Чому |
|---|---|---|---|
| 1 | Двічі `find` в одному EM | 1 | Другий — з first-level cache |
| 2 | `find` у двох різних EM | 2 | У кожного EM свій контекст |
| 3 | `find`, змінити прізвище, commit | 2 | SELECT + UPDATE від dirty checking |
| 4 | `find`, записати те саме значення, commit | 1 | Значення не змінилося — UPDATE не потрібен |
| 5 | `find`, змінити, JPQL по клієнтах, rollback | 3 | AUTO flush перед запитом: SELECT + UPDATE + SELECT |

Сценарій 5 зазвичай найнесподіваніший. Поясни: якщо не скинути UPDATE перед запитом `where c.lastName = 'Changed'`, запит не знайшов би щойно змінений об'єкт — результат був би неконсистентним. Rollback потім скасовує UPDATE у базі, але сам запит уже виконано.

Відповіді на сценарії 6–10 **не показуй**: це домашнє завдання.

### `CustomerService.rename` (5 хв)

```java
public void rename(Long customerId, String newLastName) {
    Jpa.runInTransaction(em -> {
        Customer customer = em.find(Customer.class, customerId);
        if (customer == null) throw new IllegalArgumentException("Клієнта " + customerId + " не знайдено");
        customer.setLastName(newLastName);
    });
}
```

Тест `renameUsesDirtyCheckingOnly` перевіряє, що виконано рівно 2 запити. Якщо учень додав `em.merge(customer)` — тест пройде, але запитай, навіщо цей рядок.

---

## Резерв (30 хв)

### `getReference`, `detach`, `clear` (7 хв)

- `getReference(Customer.class, id)` повертає **proxy** без SELECT. Запит буде при першому зверненні до поля (крім `getId()`). Корисно, щоб поставити зв'язок, не завантажуючи об'єкт:
  ```java
  Account account = em.getReference(Account.class, accountId);   // без SELECT
  em.persist(new Transaction(account, ...));                     // лише INSERT
  ```
  Якщо запису немає — `EntityNotFoundException`, але лише при зверненні до полів.
- `detach(entity)` — прибрати один об'єкт з контексту; незбережені зміни втрачаються.
- `clear()` — очистити весь контекст. Використовується в масовому імпорті (модуль 10).

### `FlushMode.AUTO` vs `COMMIT` (5 хв)

`em.setFlushMode(FlushModeType.COMMIT)` — flush лише при commit. Запити швидші (без перевірки змін), але можуть не бачити власних незбережених змін. За замовчуванням — `AUTO`, і змінювати його варто лише свідомо.

### `merge` vs `persist` (8 хв)

```java
Customer detached = ...;              // завантажений в іншому, закритому EM
detached.setEmail("new@example.com");

Customer managed = em.merge(detached);
detached.setEmail("other@example.com");   // ⚠️ ця зміна НЕ збережеться
managed.setEmail("other@example.com");    // ✅ ця — збережеться
```

`merge` не «приєднує» переданий об'єкт, а копіює його стан у managed-копію. Працювати далі треба з **результатом** `merge`. Тест `mergeReturnsManagedCopyAndSavesChanges` перевіряє саме це.

`persist` для detached-об'єкта кидає помилку: Hibernate бачить id і вважає, що об'єкт уже існує.

Сценарій 10 домашнього завдання показує ще одну деталь: `merge` без змін — це SELECT без UPDATE.

### `equals`/`hashCode` для entity (10 хв)

Вимоги:

1. Два нових об'єкти (id = `null`) — **не** рівні.
2. Об'єкти з однаковим id рівні, навіть якщо завантажені різними `EntityManager`.
3. `hashCode` не змінюється, коли після `persist` з'являється id.

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Customer other)) return false;
    return getId() != null && getId().equals(other.getId());
}

@Override
public int hashCode() {
    return Customer.class.hashCode();   // константа
}
```

- `instanceof`, а не `getClass() ==`: Hibernate-proxy — підклас `Customer`.
- `getId()`, а не поле: у proxy поле порожнє.
- Константний `hashCode` — ціна: усі клієнти в одному bucket `HashSet`. Для десятків-сотень елементів це неважливо; мільйон entity в `HashSet` тримати не треба взагалі.

Альтернатива — business key (`email`, IBAN), якщо він незмінний і обов'язковий.

---

## Контрольні питання

1. Що таке Persistence Context і скільки він живе?
   *Мапа managed-об'єктів і їхніх знімків у `EntityManager`; живе, доки живе `EntityManager`.*
2. Чим flush відрізняється від commit?
   *Flush відправляє SQL у базу всередині транзакції; commit фіксує транзакцію.*
3. Чому в JPA немає методу `update`?
   *Зміни managed-об'єктів знаходить dirty checking при flush.*
4. Що повертає `merge` і з яким об'єктом працювати після нього?
   *Managed-копію; далі працювати з нею, а не з переданим detached.*
5. Чому в сценарії 5 три запити, хоча транзакцію відкочено?
   *AUTO flush виконав UPDATE перед запитом; rollback скасовує зміни, але не «скасовує» вже виконані запити.*

---

## Домашнє завдання

- [ ] Сценарії 6–10: прогноз у `Predictions` + письмове пояснення кожного (1–2 речення) у коментарі над константою.
- [ ] Тест `ScenarioTest` зелений.
- [ ] Резерв, якщо не встигли на занятті: `changeEmail` через `merge` і виправити `equals`/`hashCode`.

## Типові помилки

| Помилка | Наслідок |
|---|---|
| `em.merge(managed)` «про всяк випадок» | Зайвий код, нерозуміння стану об'єкта |
| Зміни в detached-об'єкті після `merge` | Зміни мовчки втрачаються |
| `equals` за полем `id` через `Objects.equals` | Два нових клієнти «рівні»; об'єкт губиться в `HashSet` після `persist` |
| Довгоживучий `EntityManager` (поле класу, static) | Persistence Context росте, дані застарівають, не потокобезпечно |

## Джерела

- [Hibernate User Guide: Persistence Context](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#pc)
- [Hibernate User Guide: Flushing](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#flushing)
- [Jakarta Persistence 3.1: Entity Instance's Life Cycle (розділ 3.2)](https://jakarta.ee/specifications/persistence/3.1/)
