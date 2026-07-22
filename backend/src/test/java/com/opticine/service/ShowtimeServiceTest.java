package com.opticine.service;

import com.opticine.dto.showtime.response.ShowtimeResponse;
import com.opticine.entity.Movie;
import com.opticine.entity.Room;
import com.opticine.entity.Showtime;
import com.opticine.repository.ShowtimeRepository;
import com.opticine.repository.ShowtimeSeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {

    @Mock
    ShowtimeRepository showtimeRepository;

    @Mock
    ShowtimeSeatRepository showtimeSeatRepository;

    @Mock
    ShowtimeAvailabilityService showtimeAvailabilityService;

    @Mock
    ShowtimeStatusService showtimeStatusService;

    @InjectMocks
    ShowtimeService showtimeService;

    @Test
    void publicShowtimes_excludesEndedOrCancelled() {
        LocalDate date = LocalDate.now().plusDays(1);
        Showtime bookable = showtime(1L, "SCHEDULED");
        Showtime cancelled = showtime(2L, "CANCELLED");
        when(showtimeRepository.findByStartTimeBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(bookable, cancelled));
        when(showtimeAvailabilityService.isBookable(bookable)).thenReturn(true);
        when(showtimeAvailabilityService.isBookable(cancelled)).thenReturn(false);
        when(showtimeStatusService.displayStatus(bookable)).thenReturn("UPCOMING");
        when(showtimeStatusService.manualStatusLabel("SCHEDULED")).thenReturn("Sắp chiếu");
        when(showtimeStatusService.displayStatusLabel("UPCOMING")).thenReturn("Sắp chiếu");
        when(showtimeSeatRepository.countByShowtimeIdAndStatus(1L, "AVAILABLE")).thenReturn(42L);

        List<ShowtimeResponse> responses = showtimeService.getShowtimes(null, date);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(42L, responses.get(0).getAvailableSeats());
    }

    private Showtime showtime(Long id, String status) {
        Movie movie = Movie.builder()
                .id(10L + id)
                .title("Phim demo " + id)
                .durationMinutes(100)
                .ageRating("P")
                .build();
        Room room = Room.builder()
                .id(20L + id)
                .name("Room " + id)
                .screenType("2D")
                .priceMultiplier(BigDecimal.ONE)
                .build();
        return Showtime.builder()
                .id(id)
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .status(status)
                .build();
    }
}
