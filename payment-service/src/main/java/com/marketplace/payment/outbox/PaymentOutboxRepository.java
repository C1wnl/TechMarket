
package com.marketplace.payment.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentOutboxRepository
        extends JpaRepository<PaymentOutboxEvent, Long> {

    List<PaymentOutboxEvent>
    findTop20ByEnviadoFalseOrderByIdAsc();
}
