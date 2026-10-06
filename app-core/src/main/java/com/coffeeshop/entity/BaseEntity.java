package com.coffeeshop.entity;

import com.coffeeshop.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The 6 audit columns every table has. Rows are never physically deleted:
 * {@link #softDelete()} sets del_flag / del_user and every entity carries
 * {@code @SQLRestriction("del_flag = false")} so deleted rows disappear from queries.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "created_by", nullable = false, updatable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "del_flag", nullable = false)
    private boolean delFlag;

    @Column(name = "del_user", length = 50)
    private String delUser;

    /** Soft delete; the change is flushed as an UPDATE with the rest of the transaction. */
    public void softDelete() {
        this.delFlag = true;
        this.delUser = CurrentUser.username();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (createdBy == null) createdBy = CurrentUser.username();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updatedBy = CurrentUser.username();
    }
}
