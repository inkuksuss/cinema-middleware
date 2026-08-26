package com.example.cinema_middleware.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    private Long id;

    private String name;

    private String subName;

    private String description;

    private String type;

    @Column(nullable = false, columnDefinition = "CHAR(1) DEFAULT 'N'")
    private String isDelete;
}
