package com.example.cinema_middleware.domain.entity;

import com.example.cinema_middleware.domain.entity.bases.SimpleBaseEntity;
import com.example.cinema_middleware.domain.entity.enums.IssuanceStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@SQLRestriction("is_delete = 'N'")
@SQLDelete(sql = "UPDATE coupon_issuance SET is_delete = 'Y' WHERE coupon_issuance_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CouponIssuance extends SimpleBaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_issuance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(nullable = false, length = 100)
    private IssuanceStatus status = IssuanceStatus.ISSUED;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    private LocalDateTime usedAt;
}
