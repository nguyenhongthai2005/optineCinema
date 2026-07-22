package com.opticine.repository;

import com.opticine.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findTopByInvoiceCustomerIdAndInvoiceShowtimeIdOrderByCreatedAtDesc(Long customerId, Long showtimeId);
    Optional<Payment> findTopByInvoiceBookingIdOrderByCreatedAtDesc(Long bookingId);
    List<Payment> findByPaymentMethodAndStatusOrderByCreatedAtDesc(String paymentMethod, String status);

    @EntityGraph(attributePaths = {"invoice", "invoice.booking"})
    List<Payment> findByInvoiceBookingIdInOrderByCreatedAtDesc(Collection<Long> bookingIds);

    @EntityGraph(attributePaths = {"invoice", "invoice.booking", "invoice.booking.customer", "invoice.booking.showtime", "invoice.booking.showtime.movie", "invoice.booking.showtime.room"})
    @Query("""
            select p from Payment p
            where upper(p.status) = 'PAID'
              and p.invoice.booking is not null
              and coalesce(p.paidAt, p.invoice.booking.paidAt) >= :from
              and coalesce(p.paidAt, p.invoice.booking.paidAt) < :to
            order by coalesce(p.paidAt, p.invoice.booking.paidAt) desc
            """)
    List<Payment> findPaidBookingPayments(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
