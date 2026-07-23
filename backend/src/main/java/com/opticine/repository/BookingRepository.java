package com.opticine.repository;

import com.opticine.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatusAndExpiredAtBefore(String status, LocalDateTime cutoff);
    List<Booking> findByCustomerUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Long countByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Query("select sum(coalesce(b.finalAmount, b.grossTotal, 0)) from Booking b where b.customer.id = :customerId and b.status in ('PAID', 'SUCCESS', 'CONFIRMED')")
    BigDecimal sumTotalSpentByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            select b from Booking b
            where b.customer is not null
            and upper(b.status) = 'CONFIRMED'
            and upper(b.paymentStatus) = 'PAID'
            """)
    List<Booking> findSuccessfulPaidBookings();

    @Query("""
            select count(b) from Booking b
            where b.customer.id = :customerId
            and upper(b.status) = 'CONFIRMED'
            and upper(b.paymentStatus) = 'PAID'
            """)
    Long countSuccessfulBookingsByCustomerId(@Param("customerId") Long customerId);

    List<Booking> findByPaymentMethodAndPaymentStatusOrderByCreatedAtDesc(String paymentMethod, String paymentStatus);
    long countByCreatedAtBetweenAndStatus(LocalDateTime from, LocalDateTime to, String status);

    @Query("select max(b.createdAt) from Booking b where b.customer.id = :customerId")
    Optional<LocalDateTime> findLatestBookingDate(@Param("customerId") Long customerId);

    @Query("select b from Booking b join b.seats s where s.id = :showtimeSeatId")
    List<Booking> findByShowtimeSeatId(@Param("showtimeSeatId") Long showtimeSeatId);

    @Query("""
            select distinct b from Booking b
            left join b.customer c
            left join b.showtime s
            left join s.movie m
            where (coalesce(cast(:from as String), '') = '' or b.createdAt >= :from)
            and (coalesce(cast(:to as String), '') = '' or b.createdAt < :to)
            and (coalesce(:status, '') = '' or upper(b.status) = upper(:status))
            and (coalesce(:keyword, '') = ''
                or cast(b.id as String) like concat('%', :keyword, '%')
                or lower(c.fullName) like lower(concat('%', :keyword, '%'))
                or lower(c.phone) like lower(concat('%', :keyword, '%'))
                or lower(c.email) like lower(concat('%', :keyword, '%'))
                or lower(m.title) like lower(concat('%', :keyword, '%')))
            order by b.createdAt desc
            """)
    List<Booking> searchStaffBookings(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") String status,
            @Param("keyword") String keyword
    );

    @EntityGraph(attributePaths = {"customer", "showtime", "showtime.movie", "showtime.room", "seats", "seats.seat", "comboItems"})
    @Query("""
            select distinct b from Booking b
            where b.createdAt >= :from and b.createdAt < :to
            order by b.createdAt desc
            """)
    List<Booking> findReportBookings(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

