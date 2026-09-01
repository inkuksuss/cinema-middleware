package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE theater SET is_delete = 'Y' WHERE theater_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Theater extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theater_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    // TODO 향후 캐시 or 비정규화할지 정한다 for 남은 자리 계산
//    private Integer totalSeatCount = 0;

    public Theater(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
