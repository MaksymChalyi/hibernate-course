package ua.minibank;

/*
 * Завдання 1. Прогноз кількості SQL-запитів.
 *
 * У класі Scenarios — 10 коротких сценаріїв роботи з Hibernate. Для кожного треба ДО запуску сказати:
 * скільки SQL-запитів (select / insert / update / delete) Hibernate відправить у базу.
 *
 * Як працювати з кожним сценарієм:
 *   1. Відкрий метод sN у Scenarios і прочитай його.
 *   2. Подумай: які рядки звертаються до бази, а які ні? (таблиця-шпаргалка в лекції, частина 6)
 *   3. Впиши число замість -1 нижче.
 *   4. Над константою напиши коментар — ОДНЕ речення «чому».
 *   5. Лише тепер запусти ScenarioTest. Тест сценарію зелений — прогноз правильний.
 *   6. Червоний — подивись у консоль: там видно кожен SQL. Знайди, де ти помилився, і виправ ПОЯСНЕННЯ, а не лише число.
 *
 * Перед кожним сценарієм у базі рівно один клієнт (його id = f.customerId()). Підготовка не рахується.
 *
 * Приклад відповіді:
 *     // find іде в базу: SELECT. Більше нічого.
 *     public static final int SCENARIO_0 = 1;
 *
 * На занятті: сценарії 1–5. Домашнє завдання: 6–10.
 */
public final class Predictions {

    // TODO 1.1 Сценарій 1: двічі find того самого клієнта в одному EntityManager
    public static final int SCENARIO_1 = -1;

    // TODO 1.2 Сценарій 2: find у двох різних EntityManager
    public static final int SCENARIO_2 = -1;

    // TODO 1.3 Сценарій 3: find, змінити прізвище, commit — без виклику update
    public static final int SCENARIO_3 = -1;

    // TODO 1.4 Сценарій 4: find, записати в прізвище ТЕ САМЕ значення, commit
    public static final int SCENARIO_4 = -1;

    // TODO 1.5 Сценарій 5: find, змінити, JPQL-запит, rollback (найхитріший — читай лекцію про flush)
    public static final int SCENARIO_5 = -1;

    // TODO 1.6 (ДЗ) Сценарій 6: find, em.clear(), знову find
    public static final int SCENARIO_6 = -1;

    // TODO 1.7 (ДЗ) Сценарій 7: getReference і лише getId()
    public static final int SCENARIO_7 = -1;

    // TODO 1.8 (ДЗ) Сценарій 8: find і remove у транзакції
    public static final int SCENARIO_8 = -1;

    // TODO 1.9 (ДЗ) Сценарій 9: JPQL усіх клієнтів, потім find одного з них
    public static final int SCENARIO_9 = -1;

    // TODO 1.10 (ДЗ) Сценарій 10: merge detached-клієнта без змін
    public static final int SCENARIO_10 = -1;

    private Predictions() {
    }
}
