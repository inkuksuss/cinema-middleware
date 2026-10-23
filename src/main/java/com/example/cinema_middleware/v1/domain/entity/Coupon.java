package com.example.cinema_middleware.v1.domain.entity;

import com.example.cinema_middleware.v1.domain.entity.base.DefaultEntity;
import com.example.cinema_middleware.v1.domain.entity.enums.CouponStatus;
import com.example.cinema_middleware.v1.domain.entity.enums.CouponUsage;
import com.example.cinema_middleware.v1.domain.entity.enums.DiscountType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@SQLRestriction("is_delete = false")
@SQLDelete(sql = "UPDATE coupon SET is_delete = true WHERE coupon_id = ?")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class Coupon extends DefaultEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CouponUsage usage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscountType discountType;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer issuedQuantity;

    private Integer validityDay;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CouponStatus status;
}
