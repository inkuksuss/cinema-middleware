package com.example.cinema_middleware.v1.domain.entity.base;

import com.example.cinema_middleware.v1.domain.entity.enums.HistoryType;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class HistoryEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HistoryType changeType;

    private String changeReason;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime historyCreatedAt;

    @CreatedBy
    @Column(updatable = false)
    private Long historyCreatedBy;

    public void initCreatedAt(LocalDateTime createdAt) {
        this.historyCreatedAt = createdAt;
    }

    public void initCreatedBy(Long createdBy) {
        this.historyCreatedBy = createdBy;
    }

    public void setChangeType(HistoryType changeType) {
        this.changeType = changeType;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}
