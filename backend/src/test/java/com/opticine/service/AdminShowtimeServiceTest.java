package com.opticine.service;

import com.opticine.dto.admin.showtime.AdminShowtimeRequest;
import com.opticine.dto.admin.showtime.AdminShowtimeResponse;
import com.opticine.entity.Movie;
import com.opticine.entity.Room;
import com.opticine.entity.Seat;
import com.opticine.entity.Showtime;
import com.opticine.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminShowtimeServiceTest {

    @Mock
    ShowtimeRepository showtimeRepository;

    @Mock
    ShowtimeSeatRepository showtimeSeatRepository;

    @Mock
    MovieRepository movieRepository;

    @Mock
    RoomRepository roomRepository;

    @Mock
    TimeSlotRepository timeSlotRepository;

    @Mock
    SeatRepository seatRepository;

    @Mock
    ShowtimeAvailabilityService showtimeAvailabilityService;

    @Mock
    ShowtimeStatusService showtimeStatusService;

    @InjectMocks
    AdminShowtimeService adminShowtimeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminShowtimeService, "cleaningBufferMinutes", 15L);
    }

    @Test
    void createShowtime_success() {
        Movie movie = movie();
        Room room = room();
        Seat activeSeat = Seat.builder().id(30L).room(room).rowLabel("A").columnNumber(1).status("ACTIVE").basePrice(vnd(70_000)).build();
        AdminShowtimeRequest request = request(LocalDateTime.now().plusDays(1));
        when(movieRepository.findById(movie.getId())).thenReturn(Optional.of(movie));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(showtimeRepository.findByRoomId(room.getId())).thenReturn(List.of());
        when(showtimeStatusService.normalizeManualStatus("SCHEDULED")).thenReturn("SCHEDULED");
        when(showtimeRepository.save(any(Showtime.class))).thenAnswer(invocation -> {
            Showtime showtime = invocation.getArgument(0);
            showtime.setId(99L);
            return showtime;
        });
        when(seatRepository.findByRoomId(room.getId())).thenReturn(List.of(activeSeat));
        when(showtimeSeatRepository.countByShowtimeIdAndStatus(99L, "AVAILABLE")).thenReturn(1L);
        when(showtimeStatusService.displayStatus(any(Showtime.class))).thenReturn("UPCOMING");
        when(showtimeStatusService.manualStatusLabel("SCHEDULED")).thenReturn("Sắp chiếu");
        when(showtimeStatusService.displayStatusLabel("UPCOMING")).thenReturn("Sắp chiếu");
        when(showtimeAvailabilityService.isBookable(any(Showtime.class))).thenReturn(true);

        AdminShowtimeResponse response = adminShowtimeService.create(request);

        assertEquals(99L, response.getId());
        assertEquals(movie.getId(), response.getMovieId());
        verify(showtimeSeatRepository).saveAll(argThat(seats -> seats.iterator().hasNext()));
    }

    @Test
    void createShowtime_overlap_throwsConflict() {
        Movie movie = movie();
        Room room = room();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Showtime existing = Showtime.builder()
                .id(88L)
                .room(room)
                .movie(movie)
                .startTime(start.minusMinutes(30))
                .endTime(start.plusMinutes(30))
                .status("SCHEDULED")
                .build();
        when(movieRepository.findById(movie.getId())).thenReturn(Optional.of(movie));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(showtimeRepository.findByRoomId(room.getId())).thenReturn(List.of(existing));

        assertThrows(IllegalStateException.class, () -> adminShowtimeService.create(request(start)));
    }

    @Test
    void createShowtime_invalidTime_throwsValidation() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        AdminShowtimeRequest request = request(start);
        request.setEndTime(start.minusMinutes(1));

        assertThrows(IllegalArgumentException.class, () -> adminShowtimeService.create(request));
    }

    private AdminShowtimeRequest request(LocalDateTime start) {
        AdminShowtimeRequest request = new AdminShowtimeRequest();
        request.setMovieId(10L);
        request.setRoomId(20L);
        request.setStartTime(start);
        request.setEndTime(start.plusMinutes(120));
        request.setStatus("SCHEDULED");
        return request;
    }

    private Movie movie() {
        return Movie.builder().id(10L).title("Phim demo").durationMinutes(105).build();
    }

    private Room room() {
        return Room.builder().id(20L).name("Room 1").screenType("2D").priceMultiplier(BigDecimal.ONE).build();
    }

    private BigDecimal vnd(long value) {
        return BigDecimal.valueOf(value);
    }
}
