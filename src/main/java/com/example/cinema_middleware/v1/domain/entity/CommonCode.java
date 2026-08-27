package com.example.cinema_middleware.v1.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE common_code SET is_delete = 'Y' WHERE common_code_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CommonCode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "common_code_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    private String subName;

    private String description;

    private String type;

    @Column(nullable = false, columnDefinition = "CHAR(1) DEFAULT 'N'")
    private String isDelete;
}
