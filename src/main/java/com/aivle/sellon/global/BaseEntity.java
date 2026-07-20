package com.aivle.sellon.global;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {
    @Setter
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(insertable = false)
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (Objects.isNull(updatedAt))
            updatedAt = createdDate;
    }

    protected void delete() {
        if (Objects.nonNull(deletedAt))
            deletedAt = LocalDateTime.now();
    }

    protected void restore() {
        this.deletedAt = null;
    }

    protected boolean isDeleted() {
        return deletedAt != null;
    }

}