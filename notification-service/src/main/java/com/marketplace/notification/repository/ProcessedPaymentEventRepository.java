
package com.marketplace.notification.repository;

import com.marketplace.notification.model.ProcessedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPaymentEventRepository
        extends JpaRepository<ProcessedPaymentEvent, String> {
}
