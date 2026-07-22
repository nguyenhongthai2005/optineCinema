package com.opticine.service;

import com.opticine.dto.admin.showtime.AdminShowtimeRequest;
import com.opticine.dto.admin.showtime.AdminShowtimeResponse;
import com.opticine.entity.*;
import com.opticine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminShowtimeService {

    private final ShowtimeRepository     showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final MovieRepository        movieRepository;
    private final RoomRepository         roomRepository;
    private final TimeSlotRepository     timeSlotRepository;
    private final SeatRepository         seatRepository;
    private final ShowtimeAvailabilityService showtimeAvailabilityService;
    private final ShowtimeStatusService  showtimeStatusService;

    @Value("${app.showtime.cleaning-buffer-minutes:15}")
    private long cleaningBufferMinutes;

    /**
     * Lấy tất cả suất chiếu cho admin, có thể lọc theo ngày và/hoặc movieId.
     * Khác với public endpoint: trả về mọi status (kể cả CANCELLED).
     */
    @Transactional(readOnly = true)
    public List<AdminShowtimeResponse> getAll(Long movieId, LocalDate date) {
        List<Showtime> list;
        if (date != null) {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to   = date.plusDays(1).atStartOfDay();
            if (movieId != null) {
                list = showtimeRepository.findByMovieIdAndStartTimeBetween(movieId, from, to);
            } else {
                list = showtimeRepository.findByStartTimeBetween(from, to);
            }
        } else {
            if (movieId != null) {
                list = showtimeRepository.findByMovieId(movieId);
            } else {
                list = showtimeRepository.findAll();
            }
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** Lấy chi tiết một suất chiếu. */
    @Transactional(readOnly = true)
    public AdminShowtimeResponse getById(Long id) {
        return toResponse(findShowtime(id));
    }

    /**
     * Tạo suất chiếu mới.
     * Sau khi lưu showtime, tự động tạo showtime_seats từ tất cả ghế trong phòng.
     */
    @Transactional
    public AdminShowtimeResponse create(AdminShowtimeRequest request) {
        validateTime(request.getStartTime(), request.getEndTime());

        Movie    movie    = findMovie(request.getMovieId());
        Room     room     = findRoom(request.getRoomId());
        TimeSlot timeSlot = request.getTimeSlotId() != null
                ? findTimeSlot(request.getTimeSlotId()) : null;

        LocalDateTime effectiveEnd = effectiveEnd(request.getStartTime(), movie);
        checkRoomConflict(room.getId(), request.getStartTime(), effectiveEnd, null);

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .room(room)
                .timeSlot(timeSlot)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(showtimeStatusService.normalizeManualStatus(request.getStatus()))
                .build();

        showtime = showtimeRepository.save(showtime);

        // Tự động tạo showtime_seats cho tất cả ghế ACTIVE trong phòng
        List<Seat> activeSeats = seatRepository.findByRoomId(room.getId()).stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .toList();

        final Showtime savedShowtime = showtime;
        List<ShowtimeSeat> showtimeSeats = activeSeats.stream()
                .map(seat -> ShowtimeSeat.builder()
                        .showtime(savedShowtime)
                        .seat(seat)
                        .status("AVAILABLE")
                        .build())
                .collect(Collectors.toList());

        showtimeSeatRepository.saveAll(showtimeSeats);

        return toResponse(savedShowtime);
    }

    /** Cập nhật thông tin suất chiếu (chỉ khi chưa có booking). */
    @Transactional
    public AdminShowtimeResponse update(Long id, AdminShowtimeRequest request) {
        Showtime showtime = findShowtime(id);
        validateTime(request.getStartTime(), request.getEndTime());

        long bookedCount = showtimeSeatRepository.countByShowtimeIdAndStatus(id, "BOOKED");
        if (bookedCount > 0) {
            throw new IllegalStateException("Không thể chỉnh sửa suất chiếu đã có khách đặt vé");
        }

        Movie    movie    = findMovie(request.getMovieId());
        Room     room     = findRoom(request.getRoomId());
        TimeSlot timeSlot = request.getTimeSlotId() != null
                ? findTimeSlot(request.getTimeSlotId()) : null;

        LocalDateTime effectiveEnd = effectiveEnd(request.getStartTime(), movie);
        checkRoomConflict(room.getId(), request.getStartTime(), effectiveEnd, id);

        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setTimeSlot(timeSlot);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getEndTime());
        if (StringUtils.hasText(request.getStatus())) {
            showtime.setStatus(showtimeStatusService.normalizeManualStatus(request.getStatus()));
        }

        return toResponse(showtimeRepository.save(showtime));
    }

    /** Cập nhật trạng thái thủ công của suất chiếu (SCHEDULED / CANCELLED). */
    @Transactional
    public AdminShowtimeResponse updateStatus(Long id, String status) {
        Showtime showtime = findShowtime(id);
        String normalized = showtimeStatusService.normalizeManualStatus(status);
        long bookedCount = showtimeSeatRepository.countByShowtimeIdAndStatus(id, "BOOKED");
        if ("SCHEDULED".equals(normalized) && showtimeStatusService.isPastEndTime(showtime)) {
            throw new IllegalArgumentException("Không thể chuyển suất chiếu đã qua về trạng thái sắp chiếu.");
        }
        if ("CANCELLED".equals(normalized) && bookedCount > 0) {
            throw new IllegalArgumentException("Không thể hủy suất chiếu đã có vé đã bán.");
        }
        showtime.setStatus(normalized);
        return toResponse(showtimeRepository.save(showtime));
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private Showtime findShowtime(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu id=" + id));
    }

    private Movie findMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim id=" + id));
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu id=" + id));
    }

    private TimeSlot findTimeSlot(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giờ chiếu id=" + id));
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endTime phải sau startTime");
        }
    }

    /**
     * Kiểm tra phòng đã có suất chiếu khác trùng thời gian không.
     * excludeId: id của showtime hiện tại (khi update), null khi create.
     */
    private void checkRoomConflict(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<Showtime> conflicts = showtimeRepository.findByRoomId(roomId).stream()
                .filter(s -> excludeId == null || !s.getId().equals(excludeId))
                .filter(s -> !"CANCELLED".equalsIgnoreCase(s.getStatus()))
                .filter(s -> overlaps(s.getStartTime(), s.getEndTime().plusMinutes(cleaningBufferMinutes), start, end))
                .toList();
        if (!conflicts.isEmpty()) {
            Showtime conflict = conflicts.get(0);
            throw new IllegalStateException("Phòng chiếu đã có lịch chiếu từ "
                    + conflict.getStartTime().toLocalTime()
                    + " đến "
                    + conflict.getEndTime().plusMinutes(cleaningBufferMinutes).toLocalTime()
                    + ".");
        }
    }

    private LocalDateTime effectiveEnd(LocalDateTime start, Movie movie) {
        int duration = movie.getDurationMinutes() != null ? movie.getDurationMinutes() : 0;
        return start.plusMinutes(duration + cleaningBufferMinutes);
    }

    private boolean overlaps(LocalDateTime existingStart, LocalDateTime existingEnd, LocalDateTime newStart, LocalDateTime newEnd) {
        return existingStart.isBefore(newEnd) && existingEnd.isAfter(newStart);
    }

    private AdminShowtimeResponse toResponse(Showtime s) {
        long total     = showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), "AVAILABLE")
                + showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), "LOCKED")
                + showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), "BOOKED");
        long available = showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), "AVAILABLE");
        long booked    = showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), "BOOKED");
        String displayStatus = showtimeStatusService.displayStatus(s);

        return AdminShowtimeResponse.builder()
                .id(s.getId())
                .movieId(s.getMovie().getId())
                .movieTitle(s.getMovie().getTitle())
                .roomId(s.getRoom().getId())
                .roomName(s.getRoom().getName())
                .screenType(s.getRoom().getScreenType())
                .timeSlotId(s.getTimeSlot() != null ? s.getTimeSlot().getId() : null)
                .timeSlotName(s.getTimeSlot() != null ? s.getTimeSlot().getName() : null)
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .statusLabel(showtimeStatusService.manualStatusLabel(s.getStatus()))
                .displayStatus(displayStatus)
                .displayStatusLabel(showtimeStatusService.displayStatusLabel(displayStatus))
                .canEditStatus(true)
                .canBook(showtimeAvailabilityService.isBookable(s))
                .totalSeats(total)
                .availableSeats(available)
                .bookedSeats(booked)
                .build();
    }
}
