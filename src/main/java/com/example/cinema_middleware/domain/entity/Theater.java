package com.example.cinema_middleware.domain.entity;

import com.example.cinema_middleware.domain.entity.bases.BaseEntity;
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
}
