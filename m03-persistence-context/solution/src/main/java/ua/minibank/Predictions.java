package ua.minibank;

/**
 * Скільки SQL-запитів виконає кожен сценарій зі Scenarios.
 */
public final class Predictions {

    /** SELECT. Другий find бере об'єкт із Persistence Context (first-level cache). */
    public static final int SCENARIO_1 = 1;

    /** SELECT + SELECT. У кожного EntityManager свій Persistence Context. */
    public static final int SCENARIO_2 = 2;

    /** SELECT + UPDATE. Dirty checking: при commit Hibernate порівнює стан зі знімком і сам пише UPDATE. */
    public static final int SCENARIO_3 = 2;

    /** SELECT. Значення не змінилось, dirty checking не бачить змін — UPDATE не потрібен. */
    public static final int SCENARIO_4 = 1;

    /**
     * SELECT + UPDATE + SELECT. FlushMode.AUTO: перед запитом до таблиці customers Hibernate
     * скидає незбережені зміни, щоб запит побачив нове прізвище. Rollback потім скасовує UPDATE у базі,
     * але запит уже виконано.
     */
    public static final int SCENARIO_5 = 3;

    /** SELECT + SELECT. clear() очищає Persistence Context, другий find знову йде в базу. */
    public static final int SCENARIO_6 = 2;

    /** 0. getReference повертає proxy без запиту. getId() не ініціалізує proxy: id відомий заздалегідь. */
    public static final int SCENARIO_7 = 0;

    /** SELECT + DELETE. Щоб видалити, entity має бути managed, тому спочатку find. */
    public static final int SCENARIO_8 = 2;

    /** SELECT. Запит поклав клієнта в Persistence Context, find знаходить його там. */
    public static final int SCENARIO_9 = 1;

    /**
     * SELECT. merge завантажує managed-копію з бази і копіює в неї стан detached-об'єкта.
     * Значення ті самі — UPDATE не буде.
     */
    public static final int SCENARIO_10 = 1;

    private Predictions() {
    }
}
