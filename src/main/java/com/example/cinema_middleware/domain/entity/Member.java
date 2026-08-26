package com.example.cinema_middleware.domain.entity;

import com.example.cinema_middleware.domain.entity.bases.SimpleBaseEntity;
import com.example.cinema_middleware.domain.entity.enums.MemberGrade;
import com.example.cinema_middleware.domain.entity.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE member SET is_delete = 'Y' WHERE member_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends SimpleBaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String phoneNumber;

    @Column(nullable = false, length = 100)
    private String birthDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private MemberGrade grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private SocialProvider socialProvider;
}
