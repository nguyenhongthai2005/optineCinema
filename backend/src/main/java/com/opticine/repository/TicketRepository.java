package com.opticine.repository;

import com.opticine.entity.Ticket;
import com.opticine.entity.ShowtimeSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByShowtimeSeatIn(Collection<ShowtimeSeat> seats);
    Optional<Ticket> findByQrCode(String qrCode);
    long countByInvoiceBookingCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByCheckedInAtBetween(LocalDateTime from, LocalDateTime to);
}

