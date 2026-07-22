package com.opticine.repository;

import com.opticine.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByBookingId(Long bookingId);

    @Query("""
            select sum(i.totalAmount) from Invoice i
            where i.customer.id = :customerId
            and upper(i.status) = 'PAID'
            and i.totalAmount is not null
            """)
    BigDecimal sumPaidInvoiceAmountByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            select i from Invoice i
            where i.customer.id = :customerId
            and upper(i.status) = 'PAID'
            and i.totalAmount is not null
            """)
    List<Invoice> findPaidInvoicesByCustomerId(@Param("customerId") Long customerId);
}

