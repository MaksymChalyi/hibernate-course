package ua.minibank;

import jakarta.persistence.EntityManagerFactory;

import java.nio.file.Path;

/*
 * Завдання 3. Імпорт пачками: пам'ять не росте, INSERT відправляються пакетами.
 *
 * Проблема NaiveImporter: кожен persist кладе об'єкт у Persistence Context разом зі знімком (модуль 3),
 * і всі 100 000 об'єктів лежать там до кінця транзакції. Пам'ять росте лінійно.
 *
 * Рішення — кожні batchSize рядків:
 *     em.flush();   // відправити накопичені INSERT у базу (одним JDBC-пакетом, якщо увімкнено batching)
 *     em.clear();   // очистити Persistence Context — об'єкти стають detached і їх прибирає GC
 *
 * Кроки:
 *   1. Скопіюй тіло методу importFile з NaiveImporter.
 *   2. Заміни назву в ImportResult: "naive" → "batch".
 *   3. Після rows++ додай:
 *          if (rows % batchSize == 0) {
 *              em.flush();
 *              em.clear();
 *          }
 *   4. Обгорни роботу з транзакцією в try/catch з rollback (як у модулі 4):
 *          EntityTransaction tx = em.getTransaction();
 *          try { tx.begin(); ...цикл...; tx.commit(); }
 *          catch (RuntimeException e) { if (tx.isActive()) tx.rollback(); throw e; }
 *
 * Питання: що станеться, якщо викликати clear() БЕЗ flush()? (Підказка: модуль 3, стан detached.)
 *
 * Перевірка: тест «3 · batch: запитів у десятки разів менше». Потім Main з VM option -Xmx128m.
 */
// TODO 3 Імпорт пачками: копія NaiveImporter + flush() і clear() кожні batchSize рядків + rollback при помилці
public class BatchImporter implements Importer {

    private final EntityManagerFactory emf;
    private final int batchSize;

    public BatchImporter(EntityManagerFactory emf, int batchSize) {
        this.emf = emf;
        this.batchSize = batchSize;
    }

    @Override
    public ImportResult importFile(Path csv) {
        throw new UnsupportedOperationException("TODO 3: BatchImporter — кроки в коментарі над класом");
    }
}
