package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.CinemaStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieAgeRating;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieCategory;
import com.example.cinema_middleware.v1.domain.entity.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import java.time.LocalDate;

@Entity
@SQLRestriction("status != 'DELETE'")
@SQLDelete(sql = "UPDATE cinema SET status = 'DELETE' WHERE cinema_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class Cinema extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cinema_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CinemaStatus status;

}
