package com.opticine.repository;

import com.opticine.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCode(String code);

    boolean existsByCode(String code);

    List<Promotion> findByStatus(String status);

    List<Promotion> findByStatusAndStartDateBeforeAndEndDateAfter(
            String status, LocalDateTime now1, LocalDateTime now2);
}
