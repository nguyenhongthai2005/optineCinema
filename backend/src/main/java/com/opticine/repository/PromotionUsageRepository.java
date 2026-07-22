package com.opticine.repository;

import com.opticine.entity.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {

    long countByPromotionIdAndCustomerId(Long promotionId, Long customerId);

    List<PromotionUsage> findByPromotionId(Long promotionId);

    boolean existsByPromotionIdAndBookingId(Long promotionId, Long bookingId);

    @Modifying
    @Query("DELETE FROM PromotionUsage pu WHERE pu.booking.id = :bookingId")
    void deleteByBookingId(@Param("bookingId") Long bookingId);
}
