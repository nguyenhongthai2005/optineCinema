package com.opticine.repository;

import com.opticine.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // ── Public (lọc theo status OPEN) ───────────────────────────────────────

    List<Showtime> findByMovieIdAndStartTimeBetweenAndStatus(
            Long movieId,
            LocalDateTime from,
            LocalDateTime to,
            String status
    );

    List<Showtime> findByStartTimeBetweenAndStatus(
            LocalDateTime from,
            LocalDateTime to,
            String status
    );

    // ── Admin (không lọc status) ─────────────────────────────────────────────

    List<Showtime> findByMovieId(Long movieId);
    List<Showtime> findByRoomId(Long roomId);
    long countByMovieId(Long movieId);
    boolean existsByMovieId(Long movieId);
    boolean existsByMovieIdAndStartTimeAfterAndStatusNot(Long movieId, LocalDateTime startTime, String status);

    List<Showtime> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);

    List<Showtime> findByMovieIdAndStartTimeBetween(Long movieId, LocalDateTime from, LocalDateTime to);

    // ── Conflict check theo phòng ────────────────────────────────────────────

    List<Showtime> findByRoomIdAndStartTimeBetween(Long roomId, LocalDateTime from, LocalDateTime to);

    List<Showtime> findByRoomIdAndEndTimeBetween(Long roomId, LocalDateTime from, LocalDateTime to);

    @Query("""
            select s from Showtime s
            where s.room.id = :roomId
            and (cast(:excludeId as Long) is null or s.id <> :excludeId)
            and upper(s.status) <> 'CANCELLED'
            and s.startTime < :newEnd
            and s.endTime > :newStart
            """)
    List<Showtime> findOverlappingShowtimes(
            @Param("roomId") Long roomId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd,
            @Param("excludeId") Long excludeId
    );

    Optional<Showtime> findByMovieIdAndRoomIdAndStartTime(Long movieId, Long roomId, LocalDateTime startTime);
}
