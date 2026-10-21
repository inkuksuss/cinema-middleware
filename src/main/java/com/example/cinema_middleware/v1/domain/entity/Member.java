package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.TimeEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.MemberGrade;
import com.example.cinema_middleware.v1.domain.entity.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

@Entity
@SQLRestriction("is_delete = false")
@SQLDelete(sql = "UPDATE member SET is_delete = true WHERE member_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class Member extends TimeEntity {

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

    @Column(nullable = false)
    private boolean isDelete = false;

    public static Member create(
            String email,
            String username,
            String password,
            String phoneNumber,
            String birthday,
            MemberGrade grade,
            SocialProvider socialProvider
    ) {
        Assert.hasText(email, "email can not be null");
        Assert.hasText(username, "username can not be null");
        Assert.hasText(password, "password can not be null");
        Assert.hasText(phoneNumber, "phoneNumber can not be null");
        Assert.hasText(birthday, "birthday can not be null");
        Assert.notNull(grade, "grade can not be null");
        Assert.notNull(socialProvider, "socialProvider can not be null");

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
}
