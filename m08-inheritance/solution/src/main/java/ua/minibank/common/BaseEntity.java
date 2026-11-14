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
@MappedSuperclass
public abstract class BaseEntity {

    /**
     * Без явного генератора Hibernate 6 створює окрему послідовність на кожну ієрархію
     * (назва на основі імені entity) з кроком 50.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Lifecycle callback: Hibernate викликає метод перед INSERT. */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    /** Перед UPDATE. Викликається лише тоді, коли dirty checking знайшов зміни. */
    @PreUpdate
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
