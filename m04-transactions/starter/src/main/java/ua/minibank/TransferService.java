package ua.minibank;

import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;

/**
 * Переказ грошей між рахунками.
 *
 * Переказ — це ЧОТИРИ зміни в базі:
 *   1. баланс рахунку-відправника зменшується;
 *   2. записується операція DEBIT (списання) для відправника;
 *   3. баланс рахунку-отримувача збільшується;
 *   4. записується операція CREDIT (зарахування) для отримувача.
 * Або всі чотири, або жодної. Тому весь переказ — ОДНА транзакція.
 */
public class TransferService {

    private final EntityManagerFactory emf;
    private final Runnable afterDebit;

    public TransferService(EntityManagerFactory emf) {
        this(emf, () -> {
        });
    }

    /**
     * afterDebit — «гачок» для тестів. Він виконується після списання, але до зарахування.
     * Тест «1 · збій посередині» кидає тут виняток, щоб перевірити: чи відкотиться списання.
     * У своєму коді просто виклич afterDebit.run() у потрібному місці (крок 6 нижче).
     */
    TransferService(EntityManagerFactory emf, Runnable afterDebit) {
        this.emf = emf;
        this.afterDebit = afterDebit;
    }

    /*
     * Завдання 1. Переказ в одній транзакції.
     *
     * Кроки:
     *   1. Перевір аргументи ДО будь-якої роботи з базою:
     *        amount == null або amount.signum() <= 0  → throw new IllegalArgumentException("Сума переказу має бути більшою за нуль");
     *        fromId.equals(toId)                       → throw new IllegalArgumentException("Не можна переказати кошти на той самий рахунок");
     *      (signum() повертає -1, 0 або 1 — знак числа.)
     *
     *   2. Шаблон транзакції — той самий, що в модулі 1:
     *        try (EntityManager em = emf.createEntityManager()) {
     *            EntityTransaction tx = em.getTransaction();
     *            try {
     *                tx.begin();
     *                ... кроки 3–8 ...
     *                tx.commit();
     *            } catch (RuntimeException e) {
     *                if (tx.isActive()) {
     *                    tx.rollback();
     *                }
     *                throw e;
     *            }
     *        }
     *
     *   3. Знайди обидва рахунки: Account from = em.find(Account.class, fromId); так само to.
     *      Якщо якийсь null → throw new AccountNotFoundException(id).
     *      Порада: винеси це в приватний метод findAccount(em, id), щоб не писати двічі.
     *
     *   4. (ДЗ, завдання 2b) Тут буде перевірка валют. На занятті пропусти.
     *
     *   5. Списання:  from.debit(amount);
     *                 em.persist(Transaction.debit(from.getId(), amount, "Переказ на " + to.getNumber()));
     *
     *   6. afterDebit.run();     ← не прибирай, на цьому тримається тест на rollback
     *
     *   7. Зарахування: to.credit(amount);
     *                   em.persist(Transaction.credit(to.getId(), amount, "Переказ з " + from.getNumber()));
     *
     *   8. Нічого не «зберігай» для балансів: from і to — managed (з em.find). Dirty checking сам зробить UPDATE
     *      при commit. Це модуль 3 у дії.
     *
     * Перевірка: тести «1 · …» у TransferServiceTest. У консолі успішного переказу: 2 SELECT, 2 INSERT, 2 UPDATE.
     */
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // TODO 1 Переказ в одній транзакції: перевірки, find, debit + операція, afterDebit, credit + операція
        // TODO 2b (ДЗ) Якщо валюти рахунків різні — throw new CurrencyMismatchException(from.getCurrency(), to.getCurrency())
        throw new UnsupportedOperationException("TODO 1: transfer() — кроки в коментарі над методом");
    }
}
