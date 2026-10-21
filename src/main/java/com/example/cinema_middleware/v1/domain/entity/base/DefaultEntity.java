package com.example.cinema_middleware.v1.domain.entity.base;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class DefaultEntity extends TimeEntity {

    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @LastModifiedBy
    private Long updatedBy;

    public void initCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void changeUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
