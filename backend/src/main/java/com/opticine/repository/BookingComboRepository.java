package com.opticine.repository;

import com.opticine.entity.BookingCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingComboRepository extends JpaRepository<BookingCombo, Long> {
    List<BookingCombo> findByBookingId(Long bookingId);
}
