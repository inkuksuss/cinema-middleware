package com.example.cinema_middleware.v1.domain.entity.base;

import com.example.cinema_middleware.v1.domain.entity.enums.YesOrNo;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @LastModifiedBy
    private Long updatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private YesOrNo isDelete = YesOrNo.N;

    public void initCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void changeUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void initCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void changeUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
