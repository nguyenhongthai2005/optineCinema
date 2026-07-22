package com.opticine.service;

import com.opticine.dto.admin.seat.SeatResponse;
import com.opticine.entity.Room;
import com.opticine.entity.Seat;
import com.opticine.repository.RoomRepository;
import com.opticine.repository.SeatRepository;
import com.opticine.repository.ShowtimeSeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSeatServiceTest {

    @Mock
    SeatRepository seatRepository;

    @Mock
    RoomRepository roomRepository;

    @Mock
    ShowtimeSeatRepository showtimeSeatRepository;

    @InjectMocks
    AdminSeatService adminSeatService;

    @Test
    void updateSeatType_setsDefaultPrice() {
        Seat seat = seat();
        when(seatRepository.findById(seat.getId())).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeatResponse response = adminSeatService.updateType(seat.getId(), "VIP");

        assertEquals("VIP", response.getSeatType());
        assertMoney(BigDecimal.valueOf(90_000), response.getBasePrice());
    }

    @Test
    void markSeatMaintenance_success() {
        Seat seat = seat();
        when(seatRepository.findById(seat.getId())).thenReturn(Optional.of(seat));
        when(showtimeSeatRepository.existsLockedBySeatId(seat.getId())).thenReturn(false);
        when(showtimeSeatRepository.existsFutureBookedBySeatId(eq(seat.getId()), any())).thenReturn(false);
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeatResponse response = adminSeatService.updateMaintenance(seat.getId(), true);

        assertEquals("MAINTENANCE", response.getStatus());
    }

    @Test
    void restoreSeat_success() {
        Seat seat = seat();
        seat.setStatus("MAINTENANCE");
        when(seatRepository.findById(seat.getId())).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeatResponse response = adminSeatService.updateMaintenance(seat.getId(), false);

        assertEquals("ACTIVE", response.getStatus());
        verifyNoInteractions(showtimeSeatRepository);
    }

    @Test
    void seatValidationError_invalidType() {
        Seat seat = seat();
        when(seatRepository.findById(seat.getId())).thenReturn(Optional.of(seat));

        assertThrows(IllegalArgumentException.class, () -> adminSeatService.updateType(seat.getId(), "BALCONY"));
    }

    private Seat seat() {
        Room room = Room.builder().id(1L).name("Room 1").build();
        return Seat.builder()
                .id(10L)
                .room(room)
                .rowLabel("A")
                .columnNumber(1)
                .seatType("NORMAL")
                .basePrice(BigDecimal.valueOf(70_000))
                .status("ACTIVE")
                .build();
    }

    private void assertMoney(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }
}
