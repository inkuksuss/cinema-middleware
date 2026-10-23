package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.TheaterStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

@Entity
@SQLRestriction("is_delete = false")
@SQLDelete(sql = "UPDATE theater SET is_delete = true WHERE theater_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class Theater extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theater_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TheaterStatus status;

    public static Theater create(String name, String description) {
        Assert.hasText(name, "name can not be null");
        Assert.hasText(description, "description can not be null");

        Theater theater = new Theater();
        theater.name = name;
        theater.description = description;
        theater.status = TheaterStatus.AVAILABLE;

        return theater;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
