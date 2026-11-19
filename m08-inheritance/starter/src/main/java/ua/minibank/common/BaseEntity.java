package ua.minibank.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

/**
 * Спільні поля для всіх entity. @MappedSuperclass — не entity:
 * таблиці base_entity немає, поля просто копіюються в таблицю кожного нащадка.
 * Поліморфний запит "select b from BaseEntity b" неможливий — і не потрібен.
 */
/*
 * Завдання 1. Спільні поля всіх entity: id, created_at, updated_at.
 *
 * @MappedSuperclass — «батьківський клас для mapping». Це НЕ entity: таблиці base_entity не буде.
 * Поля цього класу просто з'являться в таблиці кожного нащадка.
 *
 * Кроки:
 *   1.1 Над класом: @MappedSuperclass
 *   1.2 Над id:     @Id і @GeneratedValue(strategy = GenerationType.SEQUENCE)
 *       (без @SequenceGenerator Hibernate 6 сам створить послідовність для кожної ієрархії)
 *   1.3 Над onCreate(): @PrePersist  — Hibernate викличе метод перед INSERT
 *   1.4 Над onUpdate(): @PreUpdate   — перед UPDATE (лише якщо dirty checking знайшов зміни)
 * Колонки created_at / updated_at уже описані анотаціями @Column — подивись, як.
 */
// TODO 1.1 Над класом @MappedSuperclass
public abstract class BaseEntity {

    /**
     * Без явного генератора Hibernate 6 створює окрему послідовність на кожну ієрархію
     * (назва на основі імені entity) з кроком 50.
     */
    // TODO 1.2 Над id: @Id і @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Lifecycle callback: Hibernate викликає метод перед INSERT. */
    // TODO 1.3 Над методом: @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    /** Перед UPDATE. Викликається лише тоді, коли dirty checking знайшов зміни. */
    // TODO 1.4 Над методом: @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
