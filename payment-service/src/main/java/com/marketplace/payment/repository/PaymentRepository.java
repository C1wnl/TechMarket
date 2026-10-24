package com.marketplace.payment.repository;

import com.marketplace.payment.model.Payment;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId, Sort sort);

    List<Payment> findByUserId(Long userId, Sort sort);
}
