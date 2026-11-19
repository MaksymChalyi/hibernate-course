# Модуль 11. Hibernate у веб-застосунку на Jakarta

**Що вмітимеш після модуля:**
- підключити Hibernate до Servlet-застосунку: де створити `EntityManagerFactory`, де — `EntityManager`;
- розділити код на шари `web` → `service` → `domain` і пояснити, навіщо;
- віддавати клієнту DTO замість entity;
- керувати схемою бази через Flyway, а не через `hbm2ddl.auto=update`;
- перетворювати винятки на правильні HTTP-статуси.

**Код:** `m11-web-jakarta/starter` → `m11-web-jakarta/solution`

**Що треба пам'ятати:** `TransferService` і `Tx` з модуля 4; `LazyInitializationException` з модуля 6; `@Version` з модуля 9.

---

## Таймінг

| Блок | Час | Що робимо |
|---|---|---|
| Розбір ДЗ | 10 хв | Вимірювання імпорту |
| Теорія | 25 хв | Частини 1–5 |
| Практика | 25 хв | Завдання 1–4 |
| **Резерв** | 30 хв | Частина 6: статуси помилок, історія сторінками, Open Session in View |

---

## Розбір домашнього завдання (10 хв)

Очікуваний висновок: для щоденної виписки — `BatchImporter` (або `StatelessImporter`, якщо об'єкти після вставки не потрібні). Наївний варіант не проходить `-Xmx128m`. Чистий JDBC найшвидший, але дублює mapping і нічого не перевіряє при старті.

---

# Частина 1. Що ми будуємо

До цього модуля все запускалося з `main()` або тестів. Тепер Mini Bank стає **REST API** — до нього можна звертатися з браузера, мобільного застосунку чи фронтенду:

| Запит | Що робить |
|---|---|
| `GET /accounts/1` | Повертає рахунок 1 як JSON |
| `POST /transfers` з тілом `{"fromAccountId":1,"toAccountId":3,"amount":250}` | Переказ |
| `GET /accounts/1/transactions?page=0&size=20` | Історія операцій сторінками (резерв) |

Відповідь на `GET /accounts/1`:

```json
{"id":1,"number":"UA01","currency":"UAH","balance":10000.0000,"ownerName":"Olena Koval"}
```

## 1.1. Нагадування: Servlet

Servlet — клас, якому Tomcat передає HTTP-запит:

```java
public class AccountServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // request  — що прийшло: шлях, параметри, тіло
        // response — що відповідаємо: статус, заголовки, тіло
    }
}
```

| Метод `request` | Для `GET /accounts/1/transactions?page=2` при mapping `/accounts/*` |
|---|---|
| `getPathInfo()` | `"/1/transactions"` |
| `getParameter("page")` | `"2"` |
| `getReader()` | Тіло запиту (для POST) |

**Важливо:** Tomcat створює **один** об'єкт servlet-а на весь застосунок і викликає його з **багатьох потоків** одночасно — по потоку на запит.

## 1.2. Вбудований Tomcat

Ми не встановлюємо Tomcat окремо і не збираємо WAR. Tomcat — звичайна бібліотека в `pom.xml`, яку запускає `AppServer` з `main()`. Servlet-и реєструються кодом:

```java
Tomcat.addServlet(context, "accounts", new AccountServlet());
context.addServletMappingDecoded("/accounts/*", "accounts");
```

Так само працюють Spring Boot і більшість сучасних Java-сервісів.

## 1.3. JSON через Jackson

Jackson перетворює Java-об'єкт на JSON і назад:

```java
mapper.writeValue(response.getWriter(), accountDto);                 // об'єкт → JSON
TransferRequest r = mapper.readValue(request.getReader(), TransferRequest.class);  // JSON → об'єкт
```

Jackson бере поля з getter-ів (або компонентів record). У базовому класі `JsonServlet` це вже загорнуто в `readJson` і `writeJson`.

---

# Частина 2. Шари

```
HTTP ──▶ Servlet ──▶ Service ──▶ EntityManager ──▶ PostgreSQL
         (web)       (service)    (domain: entity)
```

| Шар | Пакет | Відповідає за | НЕ знає про |
|---|---|---|---|
| Web | `web` | Розібрати HTTP, JSON, статус відповіді | `EntityManager`, транзакції, SQL |
| Service | `service` | Транзакція, бізнес-операція, entity → DTO | HTTP, JSON |
| Domain | `domain` | Entity, правила (`Account.debit` перевіряє баланс) | Усе вище |

Навіщо: кожен шар можна змінити окремо. `TransferService` з модуля 4 працює без змін — servlet лише викликає його. Завтра поряд з HTTP з'явиться імпорт з черги повідомлень — він викличе той самий сервіс.

Аналогія зі Spring (вивчатимеш далі): Servlet ≈ `@RestController`, Service ≈ `@Service` з `@Transactional`, `AppContext` ≈ IoC-контейнер.

---

# Частина 3. Де живуть EntityManagerFactory і EntityManager

## 3.1. Одна EntityManagerFactory на застосунок

Фабрика — важкий об'єкт: читає mapping, перевіряє схему, тримає пул з'єднань. Створюється **один раз при старті** і закривається **при зупинці**.

Для цього в Servlet API є `ServletContextListener`:

```java
public class AppContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        // старт: Flyway → EntityManagerFactory → сервіси → покласти в ServletContext
    }
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // зупинка: emf.close() — закриває і пул з'єднань
    }
}
```

Усі «одинаки» застосунку зібрані в record `AppContext` і лежать в атрибуті `ServletContext`. Servlet дістає їх так:

```java
protected AppContext app() {
    return AppContext.from(getServletContext());
}
// app().accountService().getAccount(1L)
```

## 3.2. EntityManager — на одну операцію

`EntityManager` **не потокобезпечний** (модуль 3: у нього свій Persistence Context). Servlet — один на всі потоки. Отже:

```java
public class AccountServlet extends HttpServlet {
    private EntityManager em;   // ПОМИЛКА: два запити одночасно зіпсують один одному Persistence Context
}
```

Правильно: `EntityManager` створюється **всередині методу сервісу** і закривається там само (через `Tx` з модуля 4). Сервіс тримає лише `EntityManagerFactory` — вона потокобезпечна.

```java
public AccountDto getAccount(Long id) {
    return Tx.read(emf, em -> {           // EntityManager відкрито
        Account a = ...;
        return AccountDto.from(a);        // DTO створено, поки EM відкритий
    });                                    // EntityManager закрито
}
```

---

# Частина 4. DTO замість entity

DTO (Data Transfer Object) — простий об'єкт лише з тими полями, які бачить клієнт:

```java
public record AccountDto(Long id, String number, CurrencyCode currency, BigDecimal balance, String ownerName) {
    public static AccountDto from(Account a) {
        return new AccountDto(a.getId(), a.getNumber(), a.getCurrency(), a.getBalance(),
                a.getCustomer().getFullName());
    }
}
```

Чому не віддати `Account` у Jackson напряму:

| Проблема | Що станеться |
|---|---|
| Lazy-зв'язки | Jackson викличе `getCustomer()` після закриття EM → `LazyInitializationException`. Або, якщо EM відкритий, — N+1 запитів |
| Двонаправлені зв'язки | `Customer.accounts` → `Account.customer` → … нескінченна рекурсія, `StackOverflowError` |
| Службові поля | Клієнт бачить `version` та інше, що його не стосується |
| Контракт API | Перейменування поля в entity ламає мобільний застосунок |

**Правило:** DTO будується **всередині** відкритого `EntityManager`. Далі це звичайний незмінний об'єкт — з ним уже нічого не може статися.

Для `getAccount` сервіс завантажує рахунок одразу з клієнтом — `join fetch a.customer` (модуль 6), щоб `getFullName()` не робив окремого запиту.

---

# Частина 5. Flyway: схема бази як код

## 5.1. Чому не `hbm2ddl.auto=update`

`update` додає таблиці й колонки, але не видаляє, не перейменовує, не переносить дані і не створює потрібних індексів. На production цим не керують.

## 5.2. Як працює Flyway

```
src/main/resources/db/migration/
  V1__schema.sql        ← таблиці, послідовності, індекси
  V2__demo_data.sql     ← демо-дані (лише для навчання)
```

- При старті Flyway дивиться в службову таблицю `flyway_schema_history`, які версії вже виконано, і виконує нові **по порядку**.
- Виконаний файл **ніколи не змінюють**: Flyway зберігає його контрольну суму і відмовиться стартувати, якщо файл змінено. Нова зміна → новий файл `V3__...sql`.
- Назва: `V` + номер + **два** підкреслення + опис.

## 5.3. Hibernate лише перевіряє

У `persistence.xml`:

```xml
<property name="hibernate.hbm2ddl.auto" value="validate"/>
```

При створенні `EntityManagerFactory` Hibernate перевіряє: чи є таблиці й колонки для кожної entity, чи збігаються типи. Розбіжність → фабрика не створюється → застосунок не стартує. Краще впасти при деплої, ніж на запиті клієнта.

**Звідси порядок:** спочатку Flyway (створює таблиці), потім фабрика (перевіряє їх). Навпаки — `validate` не знайде таблиць.

## 5.4. Що узгоджувати між entity і міграцією

| Mapping | Міграція |
|---|---|
| `@SequenceGenerator(allocationSize = 50)` | `create sequence ... increment by 50` |
| `@Column(precision = 19, scale = 4)` | `numeric(19, 4)` |
| `Instant` | `timestamp(6) with time zone` |
| `@Version Long` | `version bigint not null` |
| Індекс під запит історії | `create index ... on transactions (account_id, created_at desc)` |

**Перевір себе:**
1. Чому `EntityManager` не можна тримати в полі servlet-а?
2. Чому Flyway має виконатися до створення фабрики?
3. Назви дві проблеми віддачі entity в JSON.

---

## Практика (25 хв)

Список завдань у коді: **View → Tool Windows → TODO**. Короткий план — `starter/ЗАВДАННЯ.md`.

Підготовка: нічого. Схему `m11`, таблиці й демо-дані створить Flyway при першому старті (база `minibank` і користувач уже є після `setup-local-db.sql`).

### Завдання 1. `AppContextListener.contextInitialized` (10 хв)

```java
AppConfig config = AppConfig.fromSystemProperties();

Flyway.configure()
        .dataSource(config.dbUrl(), config.dbUser(), config.dbPassword())
        .schemas(config.dbSchema())
        .load()
        .migrate();

EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibank", Map.of(
        "jakarta.persistence.jdbc.url", config.dbUrl(),
        "jakarta.persistence.jdbc.user", config.dbUser(),
        "jakarta.persistence.jdbc.password", config.dbPassword(),
        "hibernate.default_schema", config.dbSchema()));

// mapper уже створено вище
AppContext context = new AppContext(emf, mapper, new AccountService(emf), new TransferService(emf));
event.getServletContext().setAttribute(AppContext.ATTRIBUTE, context);
```

`AppConfig` читає system properties з дефолтами для локальної бази. У модулях 12–13 тести підставлять сюди іншу адресу.

### Завдання 2. `contextDestroyed` (1 хв)

```java
Object context = event.getServletContext().getAttribute(AppContext.ATTRIBUTE);
if (context instanceof AppContext app) {
    app.emf().close();
}
```

Запусти `Main`. У консолі — `Mini Bank API: http://localhost:8080/accounts/1`. Відкрий цю адресу в браузері: поки що `501 NOT_IMPLEMENTED` — це нормально, servlet ще не написано. Головне, що застосунок стартував. Подивись у pgAdmin: у схемі `m11` з'явились таблиці і `flyway_schema_history`.

### Завдання 3. `AccountServlet.doGet` (7 хв)

```java
String path = request.getPathInfo();                 // "/1"
String[] parts = path == null ? new String[0] : path.substring(1).split("/");

if (parts.length == 1 && !parts[0].isEmpty()) {
    handle(response, 200, () -> app().accountService().getAccount(parseId(parts[0])));
} else {
    writeJson(response, 404, new ErrorDto("NOT_FOUND", "Невідомий шлях"));
}
```

`handle(...)` виконує дію і записує результат як JSON; якщо дія кинула виняток — записує помилку. Перезапусти `Main` і онови сторінку в браузері — тепер JSON рахунку.

### Завдання 4. `TransferServlet.doPost` (5 хв)

```java
handle(response, 200, () -> {
    TransferRequest transfer = readJson(request, TransferRequest.class);
    app().transferService().transfer(transfer.fromAccountId(), transfer.toAccountId(), transfer.amount());
    return TransferResult.completed(transfer);
});
```

POST з браузера не відправиш. Варіанти: тест `WebApiTest` або `curl`.

**curl у Windows:** у PowerShell `curl` — це псевдонім іншої команди. Пиши `curl.exe`:

```powershell
curl.exe http://localhost:8080/accounts/1
curl.exe -X POST http://localhost:8080/transfers -H "Content-Type: application/json" -d '{\"fromAccountId\":1,\"toAccountId\":3,\"amount\":250}'
```

Найпростіше — запустити `WebApiTest`: тести «1–3 …» і «4 …» мають бути зелені.

**Якщо не працює:**

| Що бачиш | Причина |
|---|---|
| «Застосунок не стартував» | Виняток у `contextInitialized` — перша помилка вище в лозі |
| `Schema-validation: missing table [accounts]` | Фабрика створюється до Flyway |
| `FATAL: password authentication failed` | Не виконано `setup-local-db.sql` або інший пароль |
| `Address already in use: bind` | `Main` уже запущений — зупини попередній (червоний квадрат у IDEA) |
| `Validate failed: Migration checksum mismatch` | Змінено вже виконаний файл міграції. Поверни файл або очисти схему `m11` |
| Будь-яка помилка → 500 | Нормально до завдання 5 |

---

# Частина 6. Резерв (30 хв)

## 6.1. Завдання 5. HTTP-статуси для помилок (12 хв)

Зараз «немає рахунку» і «впала база» — обидва 500. Клієнт не може зрозуміти, що сталося.

| Виняток | Статус | Код |
|---|---|---|
| `AccountNotFoundException` | 404 Not Found | `ACCOUNT_NOT_FOUND` |
| `InsufficientFundsException` | 422 Unprocessable Content | `INSUFFICIENT_FUNDS` |
| `CurrencyMismatchException` | 422 | `CURRENCY_MISMATCH` |
| `IllegalArgumentException`, `JacksonException` | 400 Bad Request | `BAD_REQUEST` |
| `OptimisticLockException` (в ланцюжку причин) | 409 Conflict | `CONCURRENT_UPDATE` |
| Усе інше | 500 | `INTERNAL_ERROR` |

- **400** — запит синтаксично неправильний (кривий JSON, `id = "abc"`). **422** — запит правильний, але бізнес-правило не дозволяє.
- Для 500 **не віддавати** `e.getMessage()`: там може бути SQL, імена таблиць, внутрішні деталі — підказка для атакувальника. Повний виняток — у лог.

```java
private void writeError(HttpServletResponse response, Exception e) throws IOException {
    if (e instanceof AccountNotFoundException) {
        writeJson(response, 404, new ErrorDto("ACCOUNT_NOT_FOUND", e.getMessage()));
    } else if (e instanceof InsufficientFundsException) {
        writeJson(response, 422, new ErrorDto("INSUFFICIENT_FUNDS", e.getMessage()));
    } else if (e instanceof CurrencyMismatchException) {
        writeJson(response, 422, new ErrorDto("CURRENCY_MISMATCH", e.getMessage()));
    } else if (e instanceof IllegalArgumentException || e instanceof JacksonException) {
        writeJson(response, 400, new ErrorDto("BAD_REQUEST", e.getMessage()));
    } else if (isOptimisticLockFailure(e)) {
        writeJson(response, 409, new ErrorDto("CONCURRENT_UPDATE", "Дані змінилися, повторіть запит"));
    } else {
        LOG.log(Level.SEVERE, "Unexpected error", e);
        writeJson(response, 500, new ErrorDto("INTERNAL_ERROR", "Внутрішня помилка"));
    }
}

private static boolean isOptimisticLockFailure(Throwable error) {
    for (Throwable t = error; t != null; t = t.getCause()) {
        if (t instanceof jakarta.persistence.OptimisticLockException) {
            return true;
        }
    }
    return false;
}
```

Чому шукаємо в ланцюжку: при `commit` Hibernate загортає `OptimisticLockException` у `RollbackException`. Це ручна версія `@ControllerAdvice` зі Spring.

## 6.2. Завдання 6. Історія сторінками (10 хв)

У `AccountServlet` — гілка для `/1/transactions`:

```java
} else if (parts.length == 2 && parts[1].equals("transactions")) {
    handle(response, 200, () -> app().accountService().history(
            parseId(parts[0]),
            intParam(request, "page", 0),
            intParam(request, "size", 20)));
}
```

Подивись, як зроблено `AccountService.history` (уже готовий):

- `size` обмежений 100 — інакше клієнт попросить мільйон рядків;
- `setFirstResult(page * size).setMaxResults(size)` → `offset ... limit ...` у SQL;
- DTO-проєкція `select new ...TransactionDto(...)` — без entity і без Persistence Context;
- окремий `count` для `total`, щоб клієнт знав, скільки всього сторінок;
- `order by t.createdAt desc, t.id desc` — другий ключ сортування, щоб порядок був стабільним, коли час однаковий.

## 6.3. Open Session in View — антипатерн (8 хв)

Ідея: відкрити `EntityManager` на початку HTTP-запиту (у фільтрі) і закрити після відповіді. Тоді lazy-поля завантажуються будь-де, навіть під час серіалізації JSON. Зручно — і саме тому небезпечно:

- SQL виконується з шару представлення — N+1 ховається в серіалізаторі;
- з'єднання з базою зайняте весь запит, включно з повільним клієнтом;
- межа транзакції розмита.

У Spring Boot це `spring.jpa.open-in-view`, увімкнений за замовчуванням (з попередженням у лозі). Перше, що варто вимкнути в новому проєкті. Наш підхід — DTO всередині сервісу — робить OSIV непотрібним.

---

## Словник

| Термін | Простими словами |
|---|---|
| `ServletContextListener` | Код, який Tomcat викликає при старті й зупинці застосунку |
| `ServletContext` | Спільне сховище застосунку, доступне всім servlet-ам |
| DTO | Об'єкт лише з полями для клієнта |
| Flyway | Інструмент, що виконує SQL-міграції по версіях |
| `hbm2ddl.auto=validate` | Hibernate перевіряє схему, нічого не змінюючи |
| OSIV | `EntityManager`, відкритий на весь HTTP-запит (антипатерн) |

---

## Контрольні питання

1. Чому `EntityManager` не можна зберегти в полі servlet-а?
   *Servlet один на всі потоки, а `EntityManager` не потокобезпечний.*
2. Чому Flyway має виконатися до створення `EntityManagerFactory`?
   *`validate` перевіряє таблиці при створенні фабрики.*
3. Чому не можна змінювати вже виконаний файл міграції?
   *Flyway перевіряє контрольну суму і не стартує; на базах, де міграцію вже виконано, зміна не застосується.*
4. Назви три проблеми віддачі entity в JSON.
   *Lazy-поля (виняток або N+1), рекурсія у двонаправлених зв'язках, службові поля і залежність API від схеми.*
5. Чим 400 відрізняється від 422?
   *400 — некоректний запит; 422 — коректний, але порушує бізнес-правило.*

---

## Домашнє завдання

- [ ] Ендпоінт `POST /statements`: імпорт CSV-виписки з модуля 10 через batching.
- [ ] Нова міграція `V3__statement_lines.sql` (не змінюючи V1 і V2).
- [ ] Перевірити: `curl.exe --data-binary "@statement.csv" -H "Content-Type: text/csv" http://localhost:8080/statements`.

## Джерела

- [Jakarta Servlet 6.0 Specification](https://jakarta.ee/specifications/servlet/6.0/)
- [Flyway: Migrations](https://documentation.red-gate.com/flyway)
- [Hibernate User Guide: Schema validation](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#schema-generation)
- [Apache Tomcat 10.1: Embedded](https://tomcat.apache.org/tomcat-10.1-doc/api/org/apache/catalina/startup/Tomcat.html)
- [RFC 9110: HTTP Semantics — status codes](https://www.rfc-editor.org/rfc/rfc9110#name-status-codes)
