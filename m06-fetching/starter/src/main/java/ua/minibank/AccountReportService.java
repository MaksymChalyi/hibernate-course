package ua.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Звіти по рахунках. Усі методи тут ПРАЦЮЮТЬ ПРАВИЛЬНО, але роблять забагато SQL-запитів.
 * Завдання — не змінити результат, а зменшити кількість запитів. Тести FetchingTest рахують запити.
 *
 * Як шукати проблему: запусти тест, відкрий консоль і порахуй однакові select,
 * що відрізняються лише параметром (where id = ?). Це і є N+1.
 */
public class AccountReportService {

    private final EntityManagerFactory emf;

    public AccountReportService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /*
     * Завдання 2. Рядки «номер рахунку — ім'я власника» одним запитом.
     *
     * Що зараз: 1 запит на всі рахунки + по 1 запиту на кожного клієнта в циклі map(...) = 1 + 20 = 21.
     * Це класичний N+1: «1» — список, «N» — довантаження зв'язку для кожного елемента.
     *
     * Як виправити: попросити Hibernate завантажити клієнтів ТИМ САМИМ запитом — JOIN FETCH.
     *
     *   було:   select a from Account a order by a.id
     *   стане:  select a from Account a join fetch a.customer order by a.id
     *                                   ^^^^^^^^^^^^^^^^^^^^^ «приєднай і заповни поле customer»
     *
     * Змінити треба лише рядок запиту. Решта методу лишається як є.
     * Перевірка: тест «2 · …» — рівно 1 запит. У консолі один select з join.
     */
    public List<String> accountOwnerLines() {
        try (EntityManager em = emf.createEntityManager()) {
            // TODO 2 Додати join fetch a.customer у запит, щоб клієнти прийшли тим самим SELECT
            List<Account> accounts = em.createQuery("select a from Account a order by a.id", Account.class)
                    .getResultList();
            return accounts.stream()
                    .map(a -> a.getNumber() + " — " + a.getCustomer().getFullName())
                    .toList();
        }
    }

    /*
     * Завдання 3. Історія операцій клієнта одним запитом.
     *
     * Що зараз: find клієнта (1) + його рахунки (1) + окремий запит операцій для КОЖНОГО рахунку (N).
     *
     * Ідея: почати не з клієнта, а з операцій. Один запит «усі операції, рахунок яких належить клієнту»:
     *
     *   select t from Transaction t
     *   join fetch t.account a                  ← рахунок прийде разом з операцією
     *   where a.customer.id = :customerId       ← фільтр за власником рахунку
     *   order by t.createdAt desc, t.id desc    ← нові зверху; id — щоб порядок був однозначним
     *
     * Кроки:
     *   1. Видали весь поточний код усередині try.
     *   2. Виконай запит вище: em.createQuery("...", Transaction.class).setParameter("customerId", customerId).getResultList()
     *   3. Перетвори кожну операцію в TransactionView:
     *        .stream().map(t -> new TransactionView(t.getAccount().getNumber(), t.getType(), t.getAmount(), t.getCreatedAt())).toList()
     *   Сортувати в Java більше не треба — сортує база.
     *
     * Перевірка: тест «3 · …» — 1 запит і правильний порядок.
     */
    public List<TransactionView> transactionHistory(Long customerId) {
        try (EntityManager em = emf.createEntityManager()) {
            // TODO 3 Замінити цикл на один запит від Transaction з join fetch t.account (кроки над методом)
            Customer customer = em.find(Customer.class, customerId);
            List<TransactionView> result = new ArrayList<>();
            for (Account account : customer.getAccounts()) {
                List<Transaction> transactions = em.createQuery(
                                "select t from Transaction t where t.account = :account", Transaction.class)
                        .setParameter("account", account)
                        .getResultList();
                for (Transaction t : transactions) {
                    result.add(new TransactionView(account.getNumber(), t.getType(), t.getAmount(), t.getCreatedAt()));
                }
            }
            result.sort(Comparator.comparing(TransactionView::createdAt).reversed());
            return result;
        }
    }

    /*
     * Завдання 4. Клієнти разом із рахунками — так, щоб рахунками можна було користуватися
     * ПІСЛЯ закриття EntityManager.
     *
     * Що зараз: тест «4 · …» падає з LazyInitializationException. Метод повертає клієнтів,
     * EntityManager закривається, а потім тест викликає customer.getAccounts().size().
     * Колекція ще не завантажена, а завантажити її вже нічим — EntityManager закритий.
     *
     * Неправильні «рішення» (не роби так):
     *   - зробити колекцію EAGER — вона вантажитиметься завжди, навіть де не потрібна;
     *   - не закривати EntityManager — витік ресурсів.
     *
     * Правильне: завантажити рахунки тим самим запитом.
     *
     *   select c from Customer c left join fetch c.accounts order by c.id
     *                            ^^^^ left — щоб не загубити клієнтів БЕЗ рахунків
     *
     * Перевірка: тест «4 · …» — 1 запит, 20 клієнтів (не 40: Hibernate сам прибирає дублікати після JOIN).
     */
    public List<Customer> customersWithAccounts() {
        try (EntityManager em = emf.createEntityManager()) {
            // TODO 4 Додати left join fetch c.accounts у запит
            return em.createQuery("select c from Customer c order by c.id", Customer.class)
                    .getResultList();
        }
    }

    /*
     * Завдання 5 (резерв). Те саме, що завдання 2, але через entity graph.
     *
     * Entity graph — «план завантаження», окремий від тексту запиту. Запит лишається простим,
     * а що підвантажити — кажемо підказкою (hint).
     *
     *   EntityGraph<Account> graph = em.createEntityGraph(Account.class);
     *   graph.addAttributeNodes("customer");                     // ім'я поля, яке завантажити
     *   return em.createQuery("select a from Account a order by a.id", Account.class)
     *            .setHint("jakarta.persistence.fetchgraph", graph)
     *            .getResultList()
     *            .stream()
     *            .map(a -> a.getNumber() + " — " + a.getCustomer().getFullName())
     *            .toList();
     *
     * (import jakarta.persistence.EntityGraph)
     * Перевірка: тест «5 (резерв) · …».
     */
    public List<String> accountOwnerLinesWithEntityGraph() {
        // TODO 5 (резерв) Завантажити рахунки з клієнтами через entity graph (код над методом)
        throw new UnsupportedOperationException("TODO 5: accountOwnerLinesWithEntityGraph — див. коментар над методом");
    }

    /*
     * Завдання 6 (резерв). DTO-проєкція: одразу record AccountOwnerView, без entity.
     *
     *   select new ua.minibank.AccountOwnerView(a.number, c.firstName, c.lastName)
     *   from Account a join a.customer c
     *   order by a.id
     *
     * select new — Hibernate викличе конструктор record для кожного рядка. Повне ім'я класу обов'язкове.
     * Тут join БЕЗ fetch: нам потрібні колонки клієнта для select, а не заповнене поле customer.
     *
     *   return em.createQuery("...", AccountOwnerView.class).getResultList();
     *
     * Перевірка: тест «6 (резерв) · …».
     */
    public List<AccountOwnerView> accountOwnerViews() {
        // TODO 6 (резерв) DTO-проєкція select new ua.minibank.AccountOwnerView(...) (код над методом)
        throw new UnsupportedOperationException("TODO 6: accountOwnerViews — див. коментар над методом");
    }
}
