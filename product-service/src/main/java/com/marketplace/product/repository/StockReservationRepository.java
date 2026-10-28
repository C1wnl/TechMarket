package com.marketplace.product.repository;

import com.marketplace.product.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    Optional<StockReservation> findByOrderIdAndProductId(Long orderId, Long productId);
}
