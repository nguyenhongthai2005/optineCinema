package com.opticine.service;

import com.opticine.dto.booking.BookingRequest;
import com.opticine.entity.*;
import com.opticine.repository.BookingRepository;
import com.opticine.repository.CustomerRepository;
import com.opticine.repository.PaymentRepository;
import com.opticine.repository.ShowtimeSeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    CustomerRepository customerRepository;

    @Mock
    ShowtimeSeatRepository showtimeSeatRepository;

    @Mock
    ComboService comboService;

    @Mock
    VietQrService vietQrService;

    @Mock
    BookingPricingService bookingPricingService;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    PromotionService promotionService;

    @Mock
    ShowtimeAvailabilityService showtimeAvailabilityService;

    @InjectMocks
    BookingService bookingService;

    @Test
    void maintenanceSeat_cannotBeBooked() {
        Long userId = 5L;
        ShowtimeSeat seat = lockedSeat(userId, "LOCKED", "MAINTENANCE");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(Customer.builder().id(1L).build()));
        when(showtimeSeatRepository.findAllById(List.of(seat.getId()))).thenReturn(List.of(seat));

        assertThrows(RuntimeException.class, () -> bookingService.createBooking(userId, request(seat.getId())));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void counterSale_alreadyBookedSeat_throwsConflict() {
        Long userId = 5L;
        ShowtimeSeat seat = lockedSeat(userId, "BOOKED", "ACTIVE");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(Customer.builder().id(1L).build()));
        when(showtimeSeatRepository.findAllById(List.of(seat.getId()))).thenReturn(List.of(seat));

        assertThrows(RuntimeException.class, () -> bookingService.createBooking(userId, request(seat.getId())));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    private BookingRequest request(Long showtimeSeatId) {
        BookingRequest request = new BookingRequest();
        request.setShowtimeSeatIds(List.of(showtimeSeatId));
        return request;
    }

    private ShowtimeSeat lockedSeat(Long userId, String status, String physicalSeatStatus) {
        Showtime showtime = Showtime.builder()
                .id(20L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();
        Seat seat = Seat.builder()
                .id(30L)
                .rowLabel("A")
                .columnNumber(1)
                .basePrice(BigDecimal.valueOf(70_000))
                .status(physicalSeatStatus)
                .build();
        return ShowtimeSeat.builder()
                .id(40L)
                .showtime(showtime)
                .seat(seat)
                .status(status)
                .lockedBy(userId)
                .lockedAt(LocalDateTime.now())
                .build();
    }
}
