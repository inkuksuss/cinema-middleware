package com.example.cinema_middleware.v1.service.dto;

import com.example.cinema_middleware.v1.domain.entity.MoviePayment;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.math.BigDecimal;

@Getter
@ToString
public class PaymentSummary {

    private BigDecimal paidAmount;

    public static PaymentSummary from(MoviePayment moviePayment) {
        Assert.notNull(moviePayment, "moviePayment can not be null");

        PaymentSummary paymentSummary = new PaymentSummary();
        paymentSummary.paidAmount = moviePayment.getPaidAmount();

        return paymentSummary;
    }
}
