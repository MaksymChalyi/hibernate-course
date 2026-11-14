# Модуль 1. Від JDBC до Hibernate: налаштування

**Мета:** побачити, яку частину JDBC-коду забирає Hibernate, і запустити його з видимими SQL-запитами.

**Код:** `m01-jdbc-to-hibernate/starter` → розв'язок у `m01-jdbc-to-hibernate/solution`

**Критерій завершення:** учень пояснює різницю між JPA і Hibernate; проєкт запускається однією командою, SQL видно в логах.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Вступ і перевірка середовища | 10 хв | PostgreSQL, IDEA, перший запуск `Main` |
| Теорія | 25 хв | Біль JDBC, JPA vs Hibernate, `persistence.xml`, `EntityManager` |
| Практика | 25 хв | TODO 1–6: entity, `persistence.xml`, `HibernateCustomerDao` |
| **Резерв** | 30 хв | Impedance mismatch, коли ORM не потрібен, `Session` vs `EntityManager`, параметри SQL |

На першому занятті розбору домашнього завдання немає: ці 10 хвилин ідуть на середовище. Встановлення PostgreSQL може з'їсти все заняття — попроси учня зробити його заздалегідь за інструкцією нижче.

---

## Перед заняттям

Docker у курсі не потрібен: працюємо з PostgreSQL, встановленим локально. Docker можна підключити пізніше, коли учень його вивчить — проєкт це підтримує.

- [ ] Встановлені JDK 21 і IntelliJ IDEA
- [ ] Встановлений PostgreSQL 16 (інструкція нижче), служба запущена
- [ ] Виконано `setup-local-db.sql` з кореня проєкту: створено користувача й базу `minibank`
- [ ] Проєкт `hibernate-course` відкритий в IDEA як Maven-проєкт (через кореневий `pom.xml`)
- [ ] Клас `m01-jdbc-to-hibernate/starter/.../Main` запускається: JDBC-частина працює, Hibernate-частина падає з помилкою — так і має бути

### Встановлення PostgreSQL

| ОС | Як встановити |
|---|---|
| Windows | Інсталятор з [postgresql.org/download/windows](https://www.postgresql.org/download/windows/). Залишити порт 5432, запам'ятати пароль користувача `postgres`. Разом встановиться pgAdmin |
| macOS | [Postgres.app](https://postgresapp.com/) (найпростіше) або `brew install postgresql@16` + `brew services start postgresql@16` |
| Linux | `sudo apt install postgresql` |

Створити користувача й базу курсу:

```bash
psql -U postgres -f setup-local-db.sql
```

```sql
-- вміст setup-local-db.sql
create user minibank with password 'minibank';
create database minibank owner minibank;
```

Без терміналу: відкрити файл у pgAdmin (Query Tool від користувача `postgres`) і виконати.

**Переглядати таблиці** зручно у вбудованій вкладці Database в IntelliJ IDEA Ultimate або в безкоштовних pgAdmin / DBeaver: підключення `localhost:5432`, база `minibank`, користувач і пароль `minibank`. Кожен модуль створює свою схему (`m01`, `m02`, …) — показати учню, де шукати таблиці.

---

## Теорія (25 хв)

### 1. Що болить у JDBC (5 хв)

Відкрий `JdbcCustomerDao` і порахуйте разом:

- ~110 рядків на 4 операції з однією таблицею;
- метод `map(ResultSet)` вручну перекладає колонки в поля;
- кожен метод повторює `try (Connection ...; PreparedStatement ...)`, `catch (SQLException)`;
- SQL з назвами колонок розкиданий по рядках: перейменування колонки — пошук по всьому коду.

А тепер уяви, що в клієнта є рахунки, у рахунків — операції, і треба завантажити клієнта з усіма рахунками. На JDBC це JOIN, ручне групування рядків і збирання графа об'єктів. Саме тут ORM дає найбільший виграш.

### 2. JPA, Hibernate і Spring Data JPA (7 хв)

| Що | Що це | Аналогія |
|---|---|---|
| **JPA / Jakarta Persistence** | Специфікація: набір інтерфейсів і анотацій (`@Entity`, `EntityManager`) у пакеті `jakarta.persistence` | Інтерфейс `List` |
| **Hibernate ORM** | Реалізація JPA плюс власні розширення (`Session`, `StatelessSession`, `@BatchSize`) | `ArrayList` |
| **Spring Data JPA** | Абстракція над JPA: генерує репозиторії (`findByEmail`) | Утиліта, що працює з будь-яким `List` |

Код пишемо через JPA-інтерфейси (`jakarta.persistence.*`), а Hibernate-специфічні можливості беремо свідомо, коли вони потрібні.

> Чому `jakarta`, а не `javax`? З 2019 Java EE передали Eclipse Foundation, і пакети перейменували на `jakarta.*`. Hibernate 6 працює лише з `jakarta`. Старі статті з `javax.persistence` — для Hibernate 5.

### 3. `persistence.xml` (5 хв)

Файл `src/main/resources/META-INF/persistence.xml` описує **persistence unit** — набір entity плюс налаштування підключення.

```xml
<persistence-unit name="minibank" transaction-type="RESOURCE_LOCAL">
    <class>ua.minibank.Customer</class>
    <exclude-unlisted-classes>true</exclude-unlisted-classes>
    <properties>
        <property name="jakarta.persistence.jdbc.url" value="jdbc:postgresql://localhost:5432/minibank?currentSchema=m01"/>
        <property name="jakarta.persistence.jdbc.user" value="minibank"/>
        <property name="jakarta.persistence.jdbc.password" value="minibank"/>
        <property name="hibernate.hbm2ddl.auto" value="create"/>
        <property name="hibernate.format_sql" value="true"/>
    </properties>
</persistence-unit>
```

- `RESOURCE_LOCAL` — транзакціями керуємо самі (`em.getTransaction()`). Альтернатива `JTA` — транзакціями керує сервер застосунків.
- `currentSchema=m01` і `hibernate.default_schema` — кожен модуль курсу у своїй схемі бази, щоб не заважати іншим.

### 4. `hibernate.hbm2ddl.auto` (4 хв)

| Значення | Що робить | Де використовувати |
|---|---|---|
| `none` | Нічого | Production разом із Flyway/Liquibase |
| `validate` | Перевіряє, що таблиці відповідають entity; не відповідають — старт падає | Production |
| `create` | Видаляє і створює таблиці при старті | Навчання, тести |
| `create-drop` | Як `create`, плюс видаляє при закритті фабрики | Тести |
| `update` | Пробує доповнити схему | **Ніде, крім експериментів** |

Чому `update` небезпечний: він додає нові таблиці й колонки, але не видаляє старі, не змінює типи, не переносить дані і не має історії змін. Перейменування поля перетворюється на «нова порожня колонка + стара з даними». З модуля 11 схемою керуватиме Flyway.

### 5. `EntityManagerFactory` і `EntityManager` (4 хв)

```java
EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank"); // один на застосунок
try (EntityManager em = emf.createEntityManager()) {                           // один на операцію
    em.getTransaction().begin();
    em.persist(customer);
    em.getTransaction().commit();
}
```

| | `EntityManagerFactory` | `EntityManager` |
|---|---|---|
| Скільки | Один на застосунок | Один на операцію / запит |
| Вартість створення | Дорого: читає mapping, підключається до бази | Дешево |
| Потокобезпечність | Так | Ні |
| Аналогія з JDBC | `DataSource` / пул з'єднань | Робота з одним `Connection` |

Чому `EntityManager` не можна тримати довго — головна тема модуля 3.

---

## Практика (25 хв)

Відкрий **View → Tool Windows → TODO**, щоб бачити всі завдання модуля.

**TODO 1. `Customer` → entity** (5 хв)

```java
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;
    // ...
    protected Customer() { } // Hibernate створює об'єкт через нього
}
```

`IDENTITY` тут — для простоти. У модулі 2 перейдемо на `SEQUENCE` і пояснимо чому.

**TODO 2. `persistence.xml`** (5 хв): `<class>`, `hbm2ddl.auto = create`, `format_sql = true`. Запусти `Main`: тепер у лозі видно `create table customers ...`.

**TODO 3–6. `HibernateCustomerDao`** (15 хв)

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
            if (tx.isActive()) tx.rollback();
            throw e;
        }
    }
}

public Optional<Customer> findById(Long id) {
    try (EntityManager em = emf.createEntityManager()) {
        return Optional.ofNullable(em.find(Customer.class, id));
    }
}

public List<Customer> findAll() {
    try (EntityManager em = emf.createEntityManager()) {
        return em.createQuery("select c from Customer c order by c.id", Customer.class).getResultList();
    }
}
```

Зверни увагу учня:

- `"select c from Customer c"` — це **JPQL**: ім'я класу і полів, а не таблиці й колонок;
- `em.find` повертає `null`, якщо запису немає, — тому `Optional.ofNullable`;
- для `remove` об'єкт спершу треба знайти: видаляти можна лише те, що знає `EntityManager`.

**Перевірка:** тест `HibernateCustomerDaoTest` зелений. Порівняйте `JdbcCustomerDao` і `HibernateCustomerDao` за кількістю рядків.

---

## Резерв (30 хв)

### Object-relational impedance mismatch (8 хв)

Об'єкти й таблиці по-різному моделюють те саме:

| Проблема | В об'єктах | У таблицях |
|---|---|---|
| Ідентичність | `a == b` і `a.equals(b)` | Primary key |
| Зв'язки | Посилання, колекції, у будь-який бік | Foreign key, лише з одного боку |
| Наслідування | `extends` | Немає; емулюється (модуль 8) |
| Навігація | `customer.getAccounts().get(0).getTransactions()` | JOIN, який треба написати заздалегідь |
| Гранулярність | `Address` як окремий клас | Колонки в тій самій таблиці (модуль 2) |

Hibernate закриває ці розриви, але кожне рішення має ціну. Найдорожча — навігація: зручний `getAccounts()` може непомітно виконати сотню запитів (модуль 6).

### Коли ORM не потрібен (7 хв)

- **Аналітика і звіти:** складні агрегати, window functions, CTE. Прямий SQL читабельніший (native-запити в модулі 7, або jOOQ).
- **Масові операції:** оновити мільйон рядків одним `UPDATE` швидше, ніж завантажити мільйон об'єктів (модуль 10).
- **Дуже простий сервіс** із 2–3 таблицями: JDBC або `JdbcTemplate` може бути простішим.

ORM виграє там, де є **бізнес-логіка над графом об'єктів**: клієнт → рахунки → операції, правила, транзакції. Це якраз банківський домен.

### `Session` vs `EntityManager` (5 хв)

`Session` — нативний інтерфейс Hibernate. З Hibernate 6 він розширює `EntityManager`:

```java
Session session = em.unwrap(Session.class);
```

У курсі пишемо через `EntityManager` (стандарт), а `unwrap` використовуємо там, де потрібна можливість лише Hibernate: `StatelessSession` у модулі 10, статистика в модулі 3.

### Значення параметрів у лозі (5 хв)

У `logback.xml` розкоментуй:

```xml
<logger name="org.hibernate.orm.jdbc.bind" level="TRACE"/>
```

Тепер замість `?` видно реальні значення. Корисно для налагодження, **небезпечно в production**: у лог потраплять персональні дані й паролі.

---

## Контрольні питання

1. Чим JPA відрізняється від Hibernate?
   *JPA — специфікація (інтерфейси й анотації), Hibernate — одна з її реалізацій із власними розширеннями.*
2. Чому `EntityManagerFactory` створюється один раз, а `EntityManager` — на кожну операцію?
   *Фабрика дорога і потокобезпечна; `EntityManager` дешевий, не потокобезпечний і накопичує стан.*
3. Чому `hbm2ddl.auto=update` не можна використовувати в production?
   *Не видаляє й не змінює колонки, не мігрує дані, не має історії змін і code review.*
4. Що таке JPQL і чим він відрізняється від SQL?
   *Запити до класів і полів, а не таблиць і колонок; Hibernate перекладає їх у SQL конкретної бази.*
5. Навіщо entity конструктор без аргументів?
   *Hibernate створює порожній об'єкт через reflection і заповнює поля з `ResultSet`.*

---

## Домашнє завдання

- [ ] Переписати ще одне своє JDBC-DAO на Hibernate (будь-яка таблиця з попередніх занять).
- [ ] Увімкнути логування параметрів SQL і надіслати скріншот логу з реальними значеннями.
- [ ] Письмово (3–5 речень): що змінилось у коді DAO і що стало менш очевидним.

## Типові помилки

| Симптом | Причина |
|---|---|
| `No Persistence provider for EntityManager named minibank` | `persistence.xml` не в `META-INF` або ім'я unit інше |
| `Not an entity: class ua.minibank.Customer` | Немає `@Entity` або класу немає в `<class>` |
| `No default constructor for entity` | Немає конструктора без аргументів |
| `Connection refused` | PostgreSQL не запущений (служба `postgresql-x64-16` у Windows, Postgres.app у macOS) |
| `password authentication failed for user "minibank"` | Не виконано `setup-local-db.sql` |
| `TransactionRequiredException` при `persist` | Забули `em.getTransaction().begin()` |

## Джерела

- [Hibernate ORM 6.6 User Guide](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html)
- [Jakarta Persistence 3.1 Specification](https://jakarta.ee/specifications/persistence/3.1/)
- [Hibernate ORM Introduction (6.6)](https://docs.jboss.org/hibernate/orm/6.6/introduction/html_single/Hibernate_Introduction.html)
