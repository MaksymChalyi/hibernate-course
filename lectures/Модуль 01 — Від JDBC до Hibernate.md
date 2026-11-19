# Модуль 1. Від JDBC до Hibernate

**Що вмітимеш після модуля:**
- згадаєш, як Java-програма говорить з базою через JDBC: `Connection`, `PreparedStatement`, `ResultSet`;
- зрозумієш, яку проблему вирішує Hibernate і що таке ORM;
- зробиш свій перший entity-клас і налаштуєш Hibernate;
- напишеш DAO, який зберігає, шукає і видаляє клієнтів без жодного рядка SQL.

**Код:** `m01-jdbc-to-hibernate/starter`. Розв'язок: `m01-jdbc-to-hibernate/solution` (відкривати після своєї спроби).

**Тривалість:** 2 заняття.

| Заняття | Теорія | Практика |
|---|---|---|
| 1 | Частини 1–3: JDBC, ORM, entity | Завдання 0 (JDBC), 1 (entity), 2 (налаштування) |
| 2 | Частина 4: EntityManager і транзакції | Завдання 3–6 (DAO на Hibernate) |

---

## Перед першим заняттям

- [ ] JDK 21, IntelliJ IDEA, PostgreSQL 16 встановлені
- [ ] Виконано `setup-local-db.sql`: є база `minibank` і користувач `minibank` / `minibank`
- [ ] Проєкт відкритий в IDEA через кореневий `pom.xml`, внизу праворуч немає прогресу завантаження
- [ ] Клас `m01-jdbc-to-hibernate/starter/.../JdbcDemo` запускається і виводить «Збережено: Customer{...}» (а потім падає на `count()` — це завдання 0)

---

# Частина 1. Згадуємо JDBC

## 1.1. Як Java-програма розмовляє з базою

PostgreSQL — окрема програма, сервер. Наша Java-програма — клієнт. Вони спілкуються мережею, навіть коли обидві на одному комп'ютері (`localhost:5432`).

```
 Java-програма                                      PostgreSQL
 ─────────────                                      ──────────
 твій код
    │  викликає методи JDBC
    ▼
 JDBC API (java.sql.*)          ← стандартні інтерфейси Java
    │
    ▼
 Драйвер PostgreSQL             ← бібліотека org.postgresql:postgresql
    │  перекладає виклики в мережевий протокол
    └───────────── мережа (порт 5432) ───────────▶ база minibank
```

**JDBC** (Java Database Connectivity) — набір інтерфейсів у пакеті `java.sql`. Він однаковий для всіх баз. **Драйвер** — реалізація цих інтерфейсів для конкретної бази. Тому код на JDBC майже не залежить від того, PostgreSQL у нас чи MySQL: змінюються драйвер і рядок підключення.

## 1.2. Чотири головні об'єкти JDBC

| Об'єкт | Що це | Аналогія |
|---|---|---|
| `Connection` | Відкрите з'єднання з базою | Телефонний дзвінок: поки він триває, можна говорити |
| `PreparedStatement` | SQL-запит з місцями `?` для значень | Бланк заяви з порожніми полями |
| `ResultSet` | Результат `SELECT`: таблиця, по якій іде курсор | Аркуш з рядками і пальцем, що вказує на поточний рядок |
| `SQLException` | Будь-яка помилка бази | — |

### Connection

```java
Connection c = DriverManager.getConnection(
        "jdbc:postgresql://localhost:5432/minibank?currentSchema=m01",  // куди
        "minibank",                                                     // користувач
        "minibank");                                                    // пароль
```

Рядок підключення (JDBC URL) читається так: протокол `jdbc`, драйвер `postgresql`, сервер `localhost`, порт `5432`, база `minibank`, параметр `currentSchema=m01` — «працюй у схемі m01».

> **Схема** в PostgreSQL — папка всередині бази. У курсі кожен модуль працює у своїй схемі (`m01`, `m02`, …), щоб модулі не заважали один одному.

### PreparedStatement і знак питання

```java
PreparedStatement ps = c.prepareStatement(
        "insert into customers (first_name, last_name, email) values (?, ?, ?)");
ps.setString(1, "Olena");                 // перший  ?  — нумерація з 1, не з 0!
ps.setString(2, "Koval");                 // другий  ?
ps.setString(3, "olena@example.com");     // третій  ?
ps.executeUpdate();                       // виконати INSERT / UPDATE / DELETE
```

**Чому `?`, а не склеювання рядка?**

```java
// НІКОЛИ так не роби:
String sql = "select * from customers where email = '" + email + "'";
```

Якщо в `email` прийде `' or '1'='1`, запит перетвориться на `... where email = '' or '1'='1'` і поверне всіх клієнтів. Це **SQL injection** — одна з найпоширеніших вразливостей. З `?` значення передається базі окремо від тексту запиту, і база ніколи не сприйме його як частину SQL.

### ResultSet: курсор по рядках

```java
ResultSet rs = ps.executeQuery();         // виконати SELECT
while (rs.next()) {                       // перейти на наступний рядок; false — рядки скінчилися
    long id = rs.getLong("id");           // прочитати колонку за назвою
    String name = rs.getString("first_name");
}
```

Курсор спочатку стоїть **перед** першим рядком. Тому `rs.next()` треба викликати навіть тоді, коли рядок гарантовано один (як у `select count(*)`).

| Метод | Коли |
|---|---|
| `executeQuery()` | `SELECT` — повертає `ResultSet` |
| `executeUpdate()` | `INSERT`, `UPDATE`, `DELETE` — повертає кількість змінених рядків |
| `execute()` | Будь-що, зокрема `CREATE TABLE` |

### Закривати обов'язково: try-with-resources

`Connection`, `PreparedStatement` і `ResultSet` тримають ресурси на сервері бази. Якщо їх не закрити, PostgreSQL рано чи пізно відмовить у нових з'єднаннях.

```java
try (Connection c = connect();
     PreparedStatement ps = c.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    // робота
}   // тут усі три закриються автоматично, у зворотному порядку, навіть якщо була помилка
```

### SQLException

`SQLException` — checked exception: компілятор змушує його обробити. У DAO ми ловимо його і перекидаємо як `RuntimeException`, бо код вище (сервіс, `main`) однаково нічого не може з ним зробити:

```java
} catch (SQLException e) {
    throw new IllegalStateException("Не вдалося зберегти клієнта", e);   // e — щоб не втратити причину
}
```

## 1.3. Розбираємо JdbcCustomerDao

Відкрий `JdbcCustomerDao.java` у `starter`. Кожен метод прокоментований. Пройдіть разом метод `findById`:

```java
public Optional<Customer> findById(Long id) {
    String sql = "select id, first_name, last_name, email from customers where id = ?";
    try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {   // 1. з'єднання + запит
        ps.setLong(1, id);                                                               // 2. значення для ?
        try (ResultSet rs = ps.executeQuery()) {                                         // 3. виконати
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();                  // 4. є рядок? → об'єкт
        }
    } catch (SQLException e) {
        throw new IllegalStateException("Не вдалося знайти клієнта", e);
    }
}

private Customer map(ResultSet rs) throws SQLException {                                 // 5. рядок → об'єкт
    Customer customer = new Customer(rs.getString("first_name"), rs.getString("last_name"), rs.getString("email"));
    customer.setId(rs.getLong("id"));
    return customer;
}
```

Порахуй, скільки рядків тут про **бізнес** («знайти клієнта за id»), а скільки — **технічні**: з'єднання, параметри, курсор, перекладання колонок у поля, обробка помилок. Технічних — майже всі. І вони повторюються в кожному методі.

## 1.4. Транзакції в JDBC (коротко)

За замовчуванням JDBC працює в режимі **autocommit**: кожен запит — окрема транзакція, яка фіксується одразу. Щоб кілька запитів виконались «або всі, або жоден»:

```java
c.setAutoCommit(false);    // почати транзакцію вручну
try {
    // кілька executeUpdate
    c.commit();            // зафіксувати
} catch (SQLException e) {
    c.rollback();          // скасувати все, що зроблено в транзакції
    throw e;
}
```

У Hibernate буде те саме, тільки іншими словами: `begin`, `commit`, `rollback`.

**Перевір себе:**
1. Чому в `PreparedStatement` значення передаються через `?`?
2. Що поверне `rs.next()`, якщо `SELECT` не знайшов жодного рядка?
3. Чим `executeQuery` відрізняється від `executeUpdate`?

---

## Практика заняття 1. Завдання 0: розминка на JDBC

**Мета:** руками згадати JDBC, перш ніж переходити до Hibernate.

**Файл:** `starter/src/main/java/ua/minibank/JdbcCustomerDao.java`, метод `count()`.

**Що зробити.** Метод має повернути кількість клієнтів у таблиці. SQL: `select count(*) from customers`. Результат цього запиту — **один рядок з однією колонкою**.

**Кроки:**
1. Подивись на метод `findAll()` вище в тому ж файлі — `count()` будується так само, тільки без циклу.
2. Відкрий три ресурси в try-with-resources: `Connection`, `PreparedStatement`, `ResultSet`.
3. Виклич `rs.next()` один раз — перейти на єдиний рядок.
4. Поверни `rs.getLong(1)` — значення першої колонки.
5. У `catch (SQLException e)` кинь `IllegalStateException` з повідомленням і `e`.

**Перевірка:**
- тест `JdbcCustomerDaoTest` → «0 · JDBC: count() рахує клієнтів» зелений;
- `JdbcDemo` виводить «Кількість клієнтів: N» і більше не падає.

**Якщо не працює:**

| Помилка | Причина |
|---|---|
| `ResultSet not positioned properly, perhaps you need to call next` | Забули `rs.next()` |
| `The column index is out of range: 0` | `rs.getLong(0)` — колонки нумеруються з 1 |
| `Connection refused` | PostgreSQL не запущений |

---

# Частина 2. Що таке ORM і Hibernate

## 2.1. Проблема: два різні світи

У Java дані живуть як **об'єкти**, у базі — як **рядки таблиць**.

```
 Java                                        PostgreSQL, таблиця customers
 ────                                        ─────────────────────────────
 Customer {                                  id │ first_name │ last_name │ email
   id = 1                                    ───┼────────────┼───────────┼──────────────────
   firstName = "Olena"            ⇄           1 │ Olena      │ Koval     │ olena@example.com
   lastName = "Koval"
   email = "olena@example.com"
 }
```

Хтось мусить перекладати туди й назад: поле `firstName` ⇄ колонка `first_name`, `Long` ⇄ `bigint`, об'єкт ⇄ рядок. У JDBC це робимо ми самі — метод `map(ResultSet)` і `setString(...)` у `save`.

## 2.2. ORM — автоматичний перекладач

**ORM** (Object-Relational Mapping) — бібліотека, яка робить це перекладання сама. Ми один раз описуємо, як клас відповідає таблиці (анотаціями), і далі працюємо з об'єктами:

| Що хочемо | JDBC | Hibernate |
|---|---|---|
| Зберегти клієнта | SQL INSERT + 3 × `setString` + прочитати id | `em.persist(customer)` |
| Знайти за id | SQL SELECT + `rs.next()` + `map(rs)` | `em.find(Customer.class, id)` |
| Видалити | SQL DELETE + `setLong` | `em.remove(customer)` |

SQL при цьому нікуди не зникає: Hibernate генерує його сам і виконує через той самий JDBC. Ми побачимо цей SQL у консолі.

```
 твій код  ──▶  Hibernate  ──▶  JDBC  ──▶  драйвер PostgreSQL  ──▶  база
               (генерує SQL)
```

**Важливо:** Hibernate не замінює JDBC, а працює **поверх** нього. Тому розуміння JDBC допомагає розуміти, що робить Hibernate.

## 2.3. JPA і Hibernate — у чому різниця

Ці слова постійно трапляються разом:

| Назва | Що це | Аналогія з Java |
|---|---|---|
| **JPA** (Jakarta Persistence) | Специфікація: набір інтерфейсів і анотацій у пакеті `jakarta.persistence` | Інтерфейс `List` |
| **Hibernate ORM** | Найпопулярніша реалізація JPA + свої додаткові можливості | `ArrayList` — реалізація `List` |
| **Spring Data JPA** | Надбудова, яка генерує DAO автоматично (вивчатимемо пізніше) | Утиліта, що працює з будь-яким `List` |

Ми пишемо код через інтерфейси JPA (`EntityManager`, `@Entity`), а під капотом працює Hibernate. Тому в import буде `jakarta.persistence.*`, а не `org.hibernate.*`.

> Якщо в інтернеті бачиш `javax.persistence` — це стара назва того самого пакета (до 2020 року). Hibernate 6, який ми використовуємо, працює тільки з `jakarta.persistence`.

## 2.4. Коли ORM не потрібен

Hibernate зручний, коли є об'єкти з бізнес-логікою: клієнт, рахунки, перекази. Але:
- складні звіти з агрегатами зручніше писати чистим SQL;
- масові операції (оновити мільйон рядків) швидше одним SQL-запитом;
- для сервісу з однією-двома таблицями JDBC може бути простішим.

Ми побачимо ці випадки в модулях 7 і 10.

**Перевір себе:**
1. Що робить ORM?
2. Чи використовує Hibernate JDBC?
3. Чим JPA відрізняється від Hibernate?

---

# Частина 3. Перший entity і налаштування Hibernate

## 3.1. Entity — клас, який зберігається в базі

**Entity** — Java-клас, об'єкти якого Hibernate вміє зберігати в таблицю і читати з неї. Щоб звичайний клас став entity, на нього ставлять анотації.

```java
@Entity                                   // 1. «цей клас — entity»
@Table(name = "customers")                // 2. таблиця customers
public class Customer {

    @Id                                   // 3. первинний ключ
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // 4. значення генерує база
    private Long id;

    @Column(name = "first_name", nullable = false)        // 5. колонка first_name, NOT NULL
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)  // UNIQUE
    private String email;

    protected Customer() { }              // 6. конструктор без аргументів — для Hibernate

    public Customer(String firstName, String lastName, String email) { ... }
}
```

Розберемо кожну анотацію.

**`@Entity`** — без неї Hibernate не вважає клас своїм. Помилка, якщо забути: `Not an entity: class ua.minibank.Customer`.

**`@Table(name = "customers")`** — ім'я таблиці. Якщо не вказати, Hibernate візьме ім'я класу: `customer`. У нашій базі таблиця називається `customers` (множина), тому вказуємо явно.

**`@Id`** — поле, яке є первинним ключем. За ним Hibernate шукає об'єкти (`em.find(Customer.class, 1)`) і відрізняє один рядок від іншого. У кожного entity має бути рівно одне поле з `@Id`.

**`@GeneratedValue(strategy = GenerationType.IDENTITY)`** — значення id не задаємо ми, його видає база під час `INSERT`. Порівняй з таблицею в `JdbcCustomerDao`:

```sql
id bigint generated by default as identity primary key
--        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ось що означає IDENTITY
```

У модулі 2 побачимо інший спосіб (`SEQUENCE`) і чому він кращий.

**`@Column`** — налаштування колонки:

| Атрибут | Що означає | У SQL |
|---|---|---|
| `name = "first_name"` | Як називається колонка | ім'я колонки |
| `nullable = false` | Не може бути порожньою | `NOT NULL` |
| `unique = true` | Значення не повторюються | `UNIQUE` |
| `length = 100` | Максимальна довжина рядка | `varchar(100)` |

Якщо `@Column` не поставити, Hibernate однаково збереже поле — у колонку з **таким самим ім'ям, як поле**: `firstName` → колонка `firstname` (PostgreSQL переводить імена в нижній регістр). Сам «чистий» Hibernate **не** перетворює `firstName` на `first_name`. Тому для полів з кількох слів `name` обов'язковий. (Spring Boot, який вивчатимемо пізніше, вмикає таке перетворення автоматично — звідси плутанина в статтях.)

**Конструктор без аргументів.** Коли Hibernate читає рядок з бази, він робить приблизно так:

```java
Customer c = new Customer();          // 1. порожній об'єкт
c.id = 1;                             // 2. заповнити поля напряму (через reflection)
c.firstName = "Olena";
...
```

Нашого конструктора з трьома параметрами він не знає, тому потрібен конструктор без аргументів. `protected` — щоб у звичайному коді його ніхто не використовував і не створював «порожніх» клієнтів.

## 3.2. persistence.xml — налаштування Hibernate

Hibernate потрібно знати: до якої бази підключатися і які класи є entity. Це описується у файлі `src/main/resources/META-INF/persistence.xml`. Шлях саме такий — Hibernate шукає файл там.

```xml
<persistence-unit name="minibank" transaction-type="RESOURCE_LOCAL">
    <class>ua.minibank.Customer</class>                         <!-- 1. список entity -->
    <exclude-unlisted-classes>true</exclude-unlisted-classes>

    <properties>
        <!-- 2. підключення: ті самі URL / user / password, що в JDBC -->
        <property name="jakarta.persistence.jdbc.url" value="jdbc:postgresql://localhost:5432/minibank?currentSchema=m01"/>
        <property name="jakarta.persistence.jdbc.user" value="minibank"/>
        <property name="jakarta.persistence.jdbc.password" value="minibank"/>

        <!-- 3. що робити зі схемою бази при старті -->
        <property name="hibernate.hbm2ddl.auto" value="create"/>

        <!-- 4. гарний вивід SQL у консоль -->
        <property name="hibernate.format_sql" value="true"/>
    </properties>
</persistence-unit>
```

**Persistence unit** — «набір entity + підключення». Його ім'я (`minibank`) потім передаємо в код:

```java
EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank");
```

`RESOURCE_LOCAL` означає, що транзакціями керуємо ми самі з коду.

### hbm2ddl.auto: хто створює таблиці

| Значення | Що робить Hibernate при старті | Де використовують |
|---|---|---|
| `none` | Нічого | Production (таблиці створюють міграції, модуль 11) |
| `validate` | Перевіряє, що таблиці відповідають entity. Не відповідають — помилка | Production |
| `create` | **Видаляє** таблиці entity і створює заново | Навчання, тести |
| `create-drop` | Як `create` + видаляє при зупинці | Тести |
| `update` | Пробує «дописати» схему | Ніде, крім швидких експериментів |

У модулях 1–10 ми використовуємо `create`: кожен запуск починається з чистої бази. Наслідок: **дані між запусками не зберігаються** — це нормально.

Чому `update` небезпечний: він додає нові колонки, але не видаляє старі, не змінює типи і не переносить дані. Перейменував поле — отримав нову порожню колонку поруч зі старою, де лишились дані.

### Як побачити SQL, який генерує Hibernate

У `src/main/resources/logback.xml` уже увімкнений логер:

```xml
<logger name="org.hibernate.SQL" level="DEBUG"/>
```

Кожен SQL з'явиться в консолі з позначкою `org.hibernate.SQL`. `format_sql = true` розбиває його на рядки.

Щоб бачити також **значення** параметрів (що підставилось замість `?`), розкоментуй там рядок з `org.hibernate.orm.jdbc.bind`. Корисно для налагодження, але в реальних проєктах вимкнено: у лог потраплять персональні дані.

**Перевір себе:**
1. Що станеться без `@Entity`?
2. Навіщо `@GeneratedValue`?
3. Що зробить `hbm2ddl.auto = create` з даними, які були в таблиці?

---

## Практика заняття 1. Завдання 1 і 2: перший entity

Працюємо у `starter`. Відкрий вікно зі списком завдань: **View → Tool Windows → TODO**. Там видно всі `TODO 1.1`, `TODO 1.2` … — клацни, щоб перейти до місця.

### Завдання 1. Зробити Customer entity

**Файл:** `Customer.java`. Над класом є таблиця «поле → колонка» — тримай її перед очима.

| Крок | Що зробити | Де |
|---|---|---|
| 1.1 | Додати `@Entity` | Над `public class Customer` |
| 1.2 | Додати `@Table(name = "customers")` | Там само |
| 1.3 | Додати `@Id` і `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Над полем `id` |
| 1.4 | Додати `@Column(...)` з правильною назвою колонки | Над `firstName`, `lastName`, `email` |
| 1.5 | Додати `protected Customer() { }` | Будь-де серед конструкторів |

**Імпорти.** Коли пишеш `@Entity`, IDEA підкреслить його червоним. Стань курсором на слово, натисни **Alt+Enter → Import class** і обери `jakarta.persistence.Entity`. **Не** `org.hibernate...` і не `javax...`.

**Уважно на кроці 1.4.** Найчастіша помилка — скопіювати рядок і забути змінити `name`. Якщо в `email` залишиться `name = "last_name"`, Hibernate при старті напише: `Column 'last_name' is duplicated`.

### Завдання 2. Налаштувати persistence.xml

**Файл:** `src/main/resources/META-INF/persistence.xml`.

| Крок | Що додати |
|---|---|
| 2.1 | `<class>ua.minibank.Customer</class>` — на місці коментаря `TODO 2.1` |
| 2.2 | `<property name="hibernate.hbm2ddl.auto" value="create"/>` |
| 2.3 | `<property name="hibernate.format_sql" value="true"/>` |

### Перевірка завдань 1–2

1. Запусти тест `HibernateCustomerDaoTest` → перший тест «1–2 · Hibernate знає entity Customer» має бути **зеленим**. Решта поки червоні — це завдання наступного заняття.
2. Запусти `HibernateDemo`. Він упаде на `save` (завдання 3), але **перед падінням** у консолі має бути приблизно такий SQL (порядок колонок і спосіб запису `unique` залежать від версії Hibernate):

```sql
drop table if exists m01.customers cascade
create table m01.customers (
    id bigint generated by default as identity,
    email varchar(255) not null unique,
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    primary key (id)
)
```

Це SQL, який Hibernate **сам** згенерував з анотацій. Порівняй з таблицею в `JdbcCustomerDao.createTableIfNotExists()` — він майже такий самий.

**Якщо не працює:**

| Помилка в консолі | Причина |
|---|---|
| `Not an entity: class ua.minibank.Customer` | Немає `@Entity` або `<class>` у persistence.xml |
| `No identifier specified for entity` | Немає `@Id` |
| `No default (no-argument) constructor for class` | Немає `protected Customer() { }` |
| `Column '...' is duplicated` | Дві `@Column` з однаковим `name` |
| `No Persistence provider for EntityManager named minibank` | persistence.xml не в `META-INF` або ім'я unit не `minibank` |

---

# Частина 4. EntityManager і транзакції

## 4.1. Дві головні «деталі» Hibernate

```java
EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank");  // один раз
EntityManager em = emf.createEntityManager();                                   // на кожну операцію
```

| | `EntityManagerFactory` | `EntityManager` |
|---|---|---|
| Скільки створюємо | Один на весь застосунок | Новий на кожну операцію |
| Створення | Дороге: читає анотації, підключається до бази | Дешеве |
| Можна з кількох потоків | Так | Ні |
| Що робить | «Фабрика» сесій, знає всі entity | Робоча сесія: зберегти, знайти, видалити |
| Аналог у JDBC | Налаштування підключення / пул з'єднань | `Connection` |

`EntityManager` треба закривати, як і `Connection`. З Hibernate 6.2 він реалізує `AutoCloseable`, тож працює try-with-resources:

```java
try (EntityManager em = emf.createEntityManager()) {
    // робота
}
```

## 4.2. Чотири операції

```java
em.persist(customer);                        // зберегти новий об'єкт         → INSERT
Customer c = em.find(Customer.class, 1L);    // знайти за id                  → SELECT ... where id = ?
List<Customer> all = em.createQuery(         // запит                          → SELECT
        "select c from Customer c", Customer.class).getResultList();
em.remove(c);                                // видалити                       → DELETE
```

**`persist`** не повертає нічого. Після збереження id з'являється в тому самому об'єкті: `customer.getId()`.

**`find`** повертає `null`, якщо такого id немає. Не виняток — саме `null`.

**`remove`** приймає **об'єкт**, а не id. Тому спочатку `find`, потім `remove`.

## 4.3. JPQL — запити до класів, а не таблиць

```
SQL:   select id, first_name, last_name, email from customers order by id
JPQL:  select c from Customer c order by c.id
```

| | SQL | JPQL |
|---|---|---|
| Після `from` | Таблиця `customers` | **Клас** `Customer` (з великої літери!) |
| Колонки / поля | `first_name` | **Поле** `c.firstName` |
| `select c` | — | Поверни цілі об'єкти |

Hibernate перекладає JPQL у SQL для конкретної бази. Якщо написати `from customers`, буде помилка: `customers is not mapped` — Hibernate не знає такого класу.

Другий аргумент `createQuery(..., Customer.class)` — тип результату: тоді `getResultList()` повертає `List<Customer>`.

## 4.4. Транзакції в Hibernate

Усе, що **змінює** базу (`persist`, `remove`, зміна полів), — тільки в транзакції. Без неї Hibernate кине `TransactionRequiredException` або просто нічого не запише.

```java
try (EntityManager em = emf.createEntityManager()) {
    EntityTransaction tx = em.getTransaction();
    try {
        tx.begin();               // JDBC: setAutoCommit(false)
        em.persist(customer);
        tx.commit();              // JDBC: commit(). Саме тут Hibernate виконує INSERT
    } catch (RuntimeException e) {
        if (tx.isActive()) {
            tx.rollback();        // JDBC: rollback()
        }
        throw e;
    }
}
```

**Цікавий момент:** `em.persist` не виконує `INSERT` одразу. Hibernate запам'ятовує «треба зберегти» і виконує SQL пізніше — при `commit`. Детально про це в модулі 3. Поки що: **без `commit` у базу нічого не потрапить.**

Читання (`find`, запити) транзакції не потребує.

**Перевір себе:**
1. Чому `EntityManager` створюється на кожну операцію, а `EntityManagerFactory` — один раз?
2. Що поверне `em.find`, якщо id не існує?
3. Чому в JPQL пишемо `Customer`, а не `customers`?
4. Коли Hibernate виконає INSERT після `em.persist`?

---

## Практика заняття 2. Завдання 3–6: DAO на Hibernate

**Файл:** `HibernateCustomerDao.java`. Над кожним методом — коментар «Завдання N» з кроками і шаблоном коду. Порядок: 3 → 4 → 5 → 6. Після кожного завдання запускай тести з його номером.

### Завдання 3. save()

**Що:** зберегти нового клієнта.
**Як:** шаблон транзакції з частини 4.4, а всередині — `em.persist(customer)`.

```java
public Customer save(Customer customer) {
    try (EntityManager em = emf.createEntityManager()) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(customer);
            tx.commit();
            return customer;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
```

**Перевірка:** тести «3 · save() …» зелені. У консолі тесту видно:

```sql
insert into m01.customers (email, first_name, last_name, id) values (?, ?, ?, default)
```

`default` замість id — бо значення генерує база (`IDENTITY`).

### Завдання 4. findById()

**Що:** знайти клієнта за id.
**Як:** `em.find(Customer.class, id)`, результат загорнути в `Optional.ofNullable(...)`. Транзакція не потрібна.

**Перевірка:** тести «4 · …». Запитай себе: чому тест «повертає Optional.empty()» пройшов би з `Optional.of(...)`? (Не пройшов би: `Optional.of(null)` кидає `NullPointerException`.)

### Завдання 5. findAll()

**Що:** усі клієнти, відсортовані за id.
**Як:** `em.createQuery("select c from Customer c order by c.id", Customer.class).getResultList()`.

**Перевірка:** тест «5 · …». У консолі — SQL, який Hibernate зробив з JPQL:

```sql
select c1_0.id, c1_0.email, c1_0.first_name, c1_0.last_name
from m01.customers c1_0
order by c1_0.id
```

`c1_0` — псевдонім, який Hibernate придумав сам.

### Завдання 6. deleteById()

**Що:** видалити клієнта за id; якщо такого немає — нічого не робити.
**Як:** шаблон транзакції, а всередині:

```java
Customer customer = em.find(Customer.class, id);
if (customer != null) {
    em.remove(customer);
}
```

**Перевірка:** тести «6 · …». У консолі: спочатку `select`, потім `delete`.

### Фінальна перевірка

1. Усі тести `HibernateCustomerDaoTest` і `JdbcCustomerDaoTest` зелені.
2. `HibernateDemo` відпрацьовує без помилок.
3. Порівняйте `JdbcCustomerDao` і `HibernateCustomerDao`: скільки рядків у кожному?

---

## Резерв (якщо залишився час)

### Значення параметрів у лозі (5 хв)

Розкоментуй у `logback.xml` логер `org.hibernate.orm.jdbc.bind` і запусти `HibernateDemo`. Під кожним `insert` з'являться рядки `binding parameter (1:VARCHAR) <- [ivan@example.com]`.

### Session vs EntityManager (5 хв)

`Session` — «рідний» інтерфейс Hibernate, ширший за `EntityManager`. Дістати його: `em.unwrap(Session.class)`. Ми пишемо через стандартний `EntityManager`, а `Session` використовуємо, коли потрібна можливість, якої немає в JPA (модуль 10).

### Що зробив би `update` (10 хв)

Експеримент: у `persistence.xml` заміни `create` на `update`, у `Customer` перейменуй поле `email` на `emailAddress` (і `@Column(name = "email_address")`). Запусти `HibernateDemo` і подивись у pgAdmin: стара колонка `email` лишилась, поруч з'явилась `email_address`. Поверни все назад.

---

## Словник

| Термін | Простими словами |
|---|---|
| JDBC | Стандартний Java API для роботи з базами |
| Драйвер | Бібліотека, що реалізує JDBC для конкретної бази |
| ORM | Автоматичне перекладання між об'єктами і таблицями |
| JPA | Специфікація ORM у Java (інтерфейси й анотації) |
| Hibernate | Реалізація JPA |
| Entity | Клас, об'єкти якого зберігаються в таблиці |
| Persistence unit | Набір entity + налаштування підключення |
| EntityManagerFactory | Фабрика сесій, одна на застосунок |
| EntityManager | Сесія роботи з базою |
| JPQL | Мова запитів до класів і полів |
| DAO | Клас, що ховає роботу з базою від решти програми |

---

## Контрольні питання

1. Навіщо `?` у `PreparedStatement`?
   *Значення передається окремо від SQL — захист від SQL injection.*
2. Чим JPA відрізняється від Hibernate?
   *JPA — специфікація, Hibernate — її реалізація.*
3. Чи замінює Hibernate JDBC?
   *Ні, працює поверх JDBC: генерує SQL і виконує його через JDBC.*
4. Навіщо entity конструктор без аргументів?
   *Hibernate створює порожній об'єкт і потім заповнює поля з бази.*
5. Що робить `hbm2ddl.auto = create` і чому його не використовують у production?
   *Видаляє і створює таблиці при старті — усі дані зникають.*
6. Чим `EntityManagerFactory` відрізняється від `EntityManager`?
   *Фабрика — одна на застосунок, дорога, потокобезпечна; EntityManager — на операцію, дешевий, не потокобезпечний.*
7. Чому в JPQL пишемо `from Customer`, а не `from customers`?
   *JPQL працює з класами, а не таблицями.*
8. Коли виконується INSERT після `em.persist`?
   *Пізніше, при `commit` (або flush, модуль 3).*

---

## Домашнє завдання

- [ ] Додати в `JdbcCustomerDao` і `HibernateCustomerDao` метод `Optional<Customer> findByEmail(String email)`.
  - JDBC: `select ... where email = ?`.
  - Hibernate: JPQL `select c from Customer c where c.email = :email`, параметр — `.setParameter("email", email)`, результат — `getResultStream().findFirst()`.
- [ ] Написати по одному тесту на кожен метод (за зразком тестів модуля).
- [ ] Увімкнути логер параметрів і зробити скріншот консолі з реальним значенням email у `where`.
- [ ] Письмово, 3–5 речень: що в `HibernateCustomerDao` стало простіше, ніж у JDBC, і що стало менш очевидним.

## Джерела

- [Hibernate ORM 6.6: Introduction](https://docs.jboss.org/hibernate/orm/6.6/introduction/html_single/Hibernate_Introduction.html)
- [Hibernate ORM 6.6 User Guide](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html)
- [Jakarta Persistence 3.1](https://jakarta.ee/specifications/persistence/3.1/)
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/documentation/use/)
