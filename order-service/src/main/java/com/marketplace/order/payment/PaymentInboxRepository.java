package com.marketplace.order.payment;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.Instant;

public interface PaymentInboxRepository extends JpaRepository<PaymentInbox, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PaymentInbox e where e.id = :id")
    Optional<PaymentInbox> lockById(@Param("id") String id);
    @Query("select e.id from PaymentInbox e where e.status = 'PENDING' and e.nextAttempt <= :now order by e.nextAttempt, e.receivedAt")
    List<String> findPending(@Param("now") Instant now, Pageable page);
}
