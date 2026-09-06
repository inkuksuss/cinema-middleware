package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.SimpleBaseEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.MemberGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.SocialProvider;
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
    private String birthday;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private MemberGrade grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private SocialProvider socialProvider;

    public static Member of(
            String email,
            String username,
            String password,
            String phoneNumber,
            String birthday,
            MemberGrade grade,
            SocialProvider socialProvider
    ) {
        Member member = new Member();
        member.email = email;
        member.username = username;
        member.password = password;
        member.phoneNumber = phoneNumber;
        member.birthday = birthday;
        member.grade = grade;
        member.socialProvider = socialProvider;

        return member;
    }


    public static Member of(
            String email,
            String username,
            String password,
            String phoneNumber,
            String birthday
    ) {
        return of(email, username, password, phoneNumber, birthday, MemberGrade.ROLE_COMMON, SocialProvider.NORMAL);
    }

    public static Member of(
            String email,
            String username,
            String password,
            String phoneNumber,
            String birthday,
            SocialProvider socialProvider
    ) {
        return of(email, username, password, phoneNumber, birthday, MemberGrade.ROLE_COMMON, socialProvider);
    }
}
