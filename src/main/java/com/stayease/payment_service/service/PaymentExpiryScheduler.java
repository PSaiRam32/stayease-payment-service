package com.stayease.payment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private final PaymentService paymentService;

    @Scheduled(fixedRate = 60000)
    public void expirePayments() {

        paymentService.expirePendingPayments();

    }

}