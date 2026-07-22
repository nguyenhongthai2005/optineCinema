package com.opticine.repository;

import com.opticine.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByRoomId(Long roomId);
    List<Seat> findByRoomIdAndStatus(Long roomId, String status);
    boolean existsByRoomIdAndRowLabelAndColumnNumber(Long roomId, String rowLabel, Integer columnNumber);
}
