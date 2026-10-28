package com.marketplace.payment.repository;

import com.marketplace.payment.model.Payment;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId, Sort sort);

    List<Payment> findByUserId(Long userId, Sort sort);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.id = :id")
    java.util.Optional<Payment> lockById(@org.springframework.data.repository.query.Param("id") Long id);
}
