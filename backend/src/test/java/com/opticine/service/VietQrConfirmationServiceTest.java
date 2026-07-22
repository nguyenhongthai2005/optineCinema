package com.opticine.service;

import com.opticine.entity.*;
import com.opticine.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VietQrConfirmationServiceTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    InvoiceRepository invoiceRepository;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    TicketRepository ticketRepository;

    @Mock
    ShowtimeSeatRepository showtimeSeatRepository;

    @Mock
    VietQrService vietQrService;

    @Mock
    SeatEventPublisher seatEventPublisher;

    @Mock
    TicketEmailService ticketEmailService;

    @Mock
    ShowtimeAvailabilityService showtimeAvailabilityService;

    @Mock
    MembershipService membershipService;

    @Mock
    BookingPricingService bookingPricingService;

    @InjectMocks
    VietQrConfirmationService confirmationService;

    @Test
    void confirmPayment_success_marksBookingPaid() {
        Booking booking = payableBooking();
        Ticket ticket = Ticket.builder().id(700L).showtimeSeat(booking.getSeats().iterator().next()).build();
        when(vietQrService.isDemoAmountEnabled()).thenReturn(true);
        when(vietQrService.calculateVietQrPayableAmount(BigDecimal.valueOf(120_000))).thenReturn(BigDecimal.valueOf(12_000));
        when(invoiceRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findTopByInvoiceBookingIdOrderByCreatedAtDesc(booking.getId())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.findByShowtimeSeatIn(booking.getSeats())).thenReturn(List.of());
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        Map<String, Object> response = confirmationService.confirmBooking(booking, null, "Thanh toán thành công.");

        assertEquals("CONFIRMED", booking.getStatus());
        assertEquals("PAID", booking.getPaymentStatus());
        assertEquals("PAID", response.get("paymentStatus"));
        verify(membershipService).processInvoice(any(Invoice.class));
        verify(showtimeSeatRepository).save(any(ShowtimeSeat.class));
        verify(ticketEmailService).sendTicketEmail(eq(booking), anyList());
    }

    @Test
    void confirmPayment_alreadyPaid_isIdempotent() {
        Booking booking = payableBooking();
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        when(ticketRepository.findByShowtimeSeatIn(booking.getSeats())).thenReturn(List.of());
        when(vietQrService.calculateVietQrPayableAmount(BigDecimal.valueOf(120_000))).thenReturn(BigDecimal.valueOf(12_000));

        Map<String, Object> response = confirmationService.confirmBooking(booking, null, "Đã thanh toán.");

        assertEquals("PAID", response.get("paymentStatus"));
        verifyNoInteractions(invoiceRepository, paymentRepository, membershipService);
    }

    @Test
    void confirmPayment_wrongOwner_throwsForbidden() {
        Booking booking = payableBooking();
        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(ResponseStatusException.class,
                () -> confirmationService.confirmCustomerTransfer(booking.getId(), 999L));
    }

    private Booking payableBooking() {
        User user = User.builder().id(5L).username("customer@opticine.test").build();
        Customer customer = Customer.builder().id(6L).user(user).build();
        Showtime showtime = Showtime.builder()
                .id(10L)
                .startTime(LocalDateTime.now().plusDays(1))
                .build();
        Seat seat = Seat.builder().id(20L).rowLabel("A").columnNumber(1).basePrice(BigDecimal.valueOf(120_000)).status("ACTIVE").build();
        ShowtimeSeat showtimeSeat = ShowtimeSeat.builder()
                .id(30L)
                .showtime(showtime)
                .seat(seat)
                .status("LOCKED")
                .lockedBy(user.getId())
                .lockedAt(LocalDateTime.now())
                .build();
        return Booking.builder()
                .id(40L)
                .customer(customer)
                .showtime(showtime)
                .seats(Set.of(showtimeSeat))
                .status("PENDING_PAYMENT")
                .paymentStatus("PENDING")
                .paymentMethod("VIETQR")
                .paymentReference("OPTI40")
                .finalAmount(BigDecimal.valueOf(120_000))
                .grossTotal(BigDecimal.valueOf(120_000))
                .discountAmount(BigDecimal.ZERO)
                .build();
    }
}
