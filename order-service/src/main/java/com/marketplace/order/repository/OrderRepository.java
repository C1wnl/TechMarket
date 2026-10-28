package com.marketplace.order.repository;

import com.marketplace.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByIdDesc(Long userId);
    java.util.Optional<Order> findByRequestKey(String requestKey);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.id = :id")
    java.util.Optional<Order> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select o.id from Order o where o.integrationPhase in ('RESERVE','CREATE_PAYMENT','CONFIRM','RELEASE') and o.nextAttempt <= :now order by o.nextAttempt, o.id")
    java.util.List<Long> findRecoverable(@org.springframework.data.repository.query.Param("now") java.time.Instant now,
                                        org.springframework.data.domain.Pageable page);
}
