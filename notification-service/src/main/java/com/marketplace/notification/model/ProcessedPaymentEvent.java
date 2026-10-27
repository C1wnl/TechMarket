
package com.marketplace.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_payment_events")
public class ProcessedPaymentEvent {

    @Id
    private String eventId;

    protected ProcessedPaymentEvent() {
    }

    public ProcessedPaymentEvent(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
