package com.example.adsportalbe.repositories;

import com.example.adsportalbe.models.payment.PaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {

    @Query("SELECT SUM(p.amountPaid) FROM PaymentReceipt p WHERE p.paidAt BETWEEN :start AND :end")
    Double sumAmountPaidByPaidAtBetween(Instant start, Instant end);
}
