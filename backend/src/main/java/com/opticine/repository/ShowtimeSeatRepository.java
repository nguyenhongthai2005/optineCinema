package com.opticine.repository;

import com.opticine.entity.ShowtimeSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {

    List<ShowtimeSeat> findByShowtimeId(Long showtimeId);

    Optional<ShowtimeSeat> findByShowtimeIdAndSeatId(Long showtimeId, Long seatId);

    List<ShowtimeSeat> findByStatusAndLockedAtBefore(String status, LocalDateTime cutoff);

    long countByShowtimeIdAndStatus(Long showtimeId, String status);

    List<ShowtimeSeat> findByShowtimeIdAndLockedBy(Long showtimeId, Long userId);

    boolean existsByShowtimeIdAndSeatId(Long showtimeId, Long seatId);
    boolean existsBySeatId(Long seatId);

    @Query("""
            select count(ss) > 0 from ShowtimeSeat ss
            where ss.seat.id = :seatId
            and upper(ss.status) = 'LOCKED'
            """)
    boolean existsLockedBySeatId(@Param("seatId") Long seatId);

    @Query("""
            select count(ss) > 0 from ShowtimeSeat ss
            where ss.seat.id = :seatId
            and upper(ss.status) = 'BOOKED'
            and ss.showtime.startTime >= :now
            """)
    boolean existsFutureBookedBySeatId(@Param("seatId") Long seatId, @Param("now") LocalDateTime now);
}
