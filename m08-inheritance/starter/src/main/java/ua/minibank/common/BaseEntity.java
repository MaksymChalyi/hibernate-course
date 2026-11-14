package ua.minibank.common;

import java.time.Instant;

/*
 * TODO 1. Зроби клас @MappedSuperclass і замапь поля:
 *   - id: @Id, @GeneratedValue(strategy = SEQUENCE);
 *   - created_at: not null, не оновлюється; updated_at: not null.
 * Зроби так, щоб onCreate викликався перед INSERT (@PrePersist), а onUpdate — перед UPDATE (@PreUpdate).
 * Питання: чим @MappedSuperclass відрізняється від @Entity з наслідуванням?
 */
public abstract class BaseEntity {

    private Long id;

    private Instant createdAt;

    private Instant updatedAt;

    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

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
