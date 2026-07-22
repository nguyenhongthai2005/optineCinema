package com.opticine.service;

import com.opticine.dto.admin.schedule.GenerateSchedulePlanRequest;
import com.opticine.dto.admin.schedule.SchedulePlanItemResponse;
import com.opticine.dto.admin.schedule.SchedulePlanResponse;
import com.opticine.dto.admin.showtime.AdminShowtimeResponse;
import com.opticine.entity.*;
import com.opticine.repository.*;
import com.opticine.service.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AutoScheduleService {

    private static final List<LocalTime> DEFAULT_START_TIMES = List.of(
            LocalTime.of(10, 0),
            LocalTime.of(13, 30),
            LocalTime.of(16, 0),
            LocalTime.of(19, 30),
            LocalTime.of(22, 0)
    );

    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final SchedulePlanRepository schedulePlanRepository;
    private final SchedulePlanItemRepository schedulePlanItemRepository;
    private final ShowtimeAvailabilityService showtimeAvailabilityService;
    private final ShowtimeStatusService showtimeStatusService;

    @Transactional
    public SchedulePlanResponse generate(GenerateSchedulePlanRequest request) {
        validateRequest(request);
        List<Movie> movies = movieRepository.findAll().stream()
                .filter(this::isNowShowing)
                .toList();
        List<Room> rooms = roomRepository.findAll().stream()
                .filter(room -> "ACTIVE".equalsIgnoreCase(room.getStatus()))
                .filter(room -> !activeSeats(room).isEmpty())
                .toList();
        if (movies.isEmpty()) {
            throw new IllegalArgumentException("Không có phim NOW_SHOWING để xếp lịch");
        }
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("Không có phòng ACTIVE để xếp lịch");
        }

        LocalDateTime rangeStart = request.getFromDate().atTime(request.getOpeningTime());
        LocalDateTime rangeEnd = request.getToDate().atTime(request.getClosingTime());
        List<Showtime> existingShowtimes = showtimeRepository.findByStartTimeBetween(
                request.getFromDate().atStartOfDay(),
                request.getToDate().plusDays(1).atStartOfDay()
        ).stream().filter(showtime -> !"CANCELLED".equalsIgnoreCase(showtime.getStatus())).toList();

        Map<Long, List<Interval>> roomCalendars = new HashMap<>();
        for (Room room : rooms) {
            roomCalendars.put(room.getId(), new ArrayList<>());
        }
        for (Showtime showtime : existingShowtimes) {
            if (showtime.getRoom() != null) {
                roomCalendars.computeIfAbsent(showtime.getRoom().getId(), ignored -> new ArrayList<>())
                        .add(new Interval(showtime.getStartTime(), showtime.getEndTime(), true));
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        for (LocalDate date = request.getFromDate(); !date.isAfter(request.getToDate()); date = date.plusDays(1)) {
            LocalDateTime dayOpen = date.atTime(request.getOpeningTime());
            LocalDateTime dayClose = date.atTime(request.getClosingTime());
            for (LocalTime time : DEFAULT_START_TIMES) {
                if (time.isBefore(request.getOpeningTime()) || time.isAfter(request.getClosingTime())) {
                    continue;
                }
                for (Movie movie : movies) {
                    for (Room room : rooms) {
                        Candidate candidate = buildCandidate(movie, room, date.atTime(time), request.getMode());
                        if (!candidate.endTime.plusMinutes(request.getCleaningMinutes()).isAfter(dayClose)
                                && !candidate.startTime.isBefore(dayOpen)
                                && !candidate.startTime.isBefore(rangeStart)
                                && !candidate.endTime.isAfter(rangeEnd)
                                && fits(roomCalendars.get(room.getId()), candidate.startTime, candidate.endTime, request.getCleaningMinutes())) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
        }

        candidates.sort(Comparator.comparing(Candidate::score).reversed());
        Map<String, Integer> movieHourCount = new HashMap<>();
        List<Candidate> selected = new ArrayList<>();
        for (Candidate candidate : candidates) {
            String movieHourKey = candidate.movie.getId() + "@" + candidate.startTime.toLocalDate() + "T" + candidate.startTime.toLocalTime();
            int sameMovieHour = movieHourCount.getOrDefault(movieHourKey, 0);
            if (sameMovieHour >= 1) {
                continue;
            }
            List<Interval> calendar = roomCalendars.get(candidate.room.getId());
            if (!fits(calendar, candidate.startTime, candidate.endTime, request.getCleaningMinutes())) {
                continue;
            }
            calendar.add(new Interval(candidate.startTime, candidate.endTime, false));
            movieHourCount.put(movieHourKey, sameMovieHour + 1);
            selected.add(candidate);
        }

        SchedulePlan plan = new SchedulePlan();
        plan.setBusinessDate(request.getFromDate());
        plan.setFromDate(request.getFromDate());
        plan.setToDate(request.getToDate());
        plan.setMode(normalizeMode(request.getMode()));
        plan.setStatus("DRAFT");
        plan.setOpeningTime(request.getOpeningTime().toString());
        plan.setClosingTime(request.getClosingTime().toString());
        plan.setCleaningMinutes(request.getCleaningMinutes());
        plan.setOverwriteExisting(Boolean.TRUE.equals(request.getOverwriteExisting()));
        plan.setWindowMinutes((int) Duration.between(request.getOpeningTime(), request.getClosingTime()).toMinutes());
        plan.setGeneratedAt(LocalDateTime.now());
        plan.setGeneratedBy(currentUser());
        plan.setNote("Greedy optimized schedule preview. Existing showtimes are preserved.");

        BigDecimal estimatedRevenue = selected.stream()
                .map(candidate -> candidate.expectedRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int estimatedTickets = selected.stream().mapToInt(candidate -> candidate.expectedTickets).sum();
        BigDecimal utilization = calculateUtilization(selected, rooms.size(), request);
        plan.setEstimatedRevenue(estimatedRevenue);
        plan.setEstimatedTickets(estimatedTickets);
        plan.setRoomUtilization(utilization);

        for (Candidate candidate : selected.stream().sorted(Comparator.comparing(Candidate::startTime)).toList()) {
            SchedulePlanItem item = SchedulePlanItem.builder()
                    .plan(plan)
                    .movie(candidate.movie)
                    .room(candidate.room)
                    .startTime(candidate.startTime)
                    .endTime(candidate.endTime)
                    .expectedTickets(candidate.expectedTickets)
                    .expectedRevenue(candidate.expectedRevenue)
                    .score(BigDecimal.valueOf(candidate.score).setScale(2, RoundingMode.HALF_UP))
                    .reason(candidate.reason)
                    .build();
            plan.getItems().add(item);
        }

        return toResponse(schedulePlanRepository.save(plan), true);
    }

    @Transactional(readOnly = true)
    public List<SchedulePlanResponse> recentPlans() {
        return schedulePlanRepository.findTop20ByOrderByGeneratedAtDesc().stream()
                .map(plan -> toResponse(plan, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public SchedulePlanResponse getPlan(Long id) {
        return toResponse(findPlan(id), true);
    }

    @Transactional
    public SchedulePlanResponse cancel(Long id) {
        SchedulePlan plan = findPlan(id);
        if (!"DRAFT".equals(plan.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy plan DRAFT");
        }
        plan.setStatus("CANCELLED");
        return toResponse(schedulePlanRepository.save(plan), true);
    }

    @Transactional
    public List<AdminShowtimeResponse> apply(Long id) {
        SchedulePlan plan = findPlan(id);
        if (!"DRAFT".equals(plan.getStatus())) {
            throw new IllegalStateException("Chỉ có thể áp dụng plan DRAFT");
        }
        List<AdminShowtimeResponse> applied = new ArrayList<>();
        List<SchedulePlanItem> items = schedulePlanItemRepository.findByPlanIdOrderByStartTimeAsc(plan.getId());
        for (SchedulePlanItem item : items) {
            Optional<Showtime> existing = showtimeRepository.findByMovieIdAndRoomIdAndStartTime(
                    item.getMovie().getId(), item.getRoom().getId(), item.getStartTime());
            Showtime showtime = existing.orElseGet(() -> {
                Showtime created = new Showtime();
                created.setMovie(item.getMovie());
                created.setRoom(item.getRoom());
                created.setStartTime(item.getStartTime());
                created.setEndTime(item.getEndTime());
                created.setTimeSlot(findMatchingTimeSlot(item.getStartTime().toLocalTime()));
                created.setStatus("SCHEDULED");
                return showtimeRepository.save(created);
            });
            ensureShowtimeSeats(showtime);
            item.setShowtime(showtime);
            applied.add(toShowtimeResponse(showtime));
        }
        plan.setStatus("APPLIED");
        schedulePlanRepository.save(plan);
        return applied;
    }

    private Candidate buildCandidate(Movie movie, Room room, LocalDateTime startTime, String mode) {
        LocalDateTime endTime = startTime.plusMinutes(movie.getDurationMinutes() != null ? movie.getDurationMinutes() : 120);
        int capacity = activeSeats(room).size();
        int popularity = movie.getPopularityScore() != null ? movie.getPopularityScore() : 50;
        double dayFactor = dayFactor(startTime.toLocalDate());
        double timeFactor = timeFactor(startTime.toLocalTime());
        double roomFactor = roomFactor(room);
        double genreFactor = genreFactor(movie);
        double predictedDemand = popularity * dayFactor * timeFactor * roomFactor * genreFactor;
        int expectedTickets = Math.max(0, Math.min(capacity, (int) Math.round(predictedDemand)));
        BigDecimal averagePrice = averageSeatPrice(room, startTime.toLocalTime());
        BigDecimal expectedRevenue = averagePrice.multiply(BigDecimal.valueOf(expectedTickets)).setScale(2, RoundingMode.HALF_UP);
        double occupancy = capacity == 0 ? 0 : expectedTickets / (double) capacity;
        double score = switch (normalizeMode(mode)) {
            case "MAX_UTILIZATION" -> occupancy * 1000 + expectedRevenue.doubleValue() / 2000.0;
            case "BALANCED" -> expectedRevenue.doubleValue() / 1000.0 + occupancy * 240 + diversityBoost(movie);
            default -> expectedRevenue.doubleValue() / 1000.0 + occupancy * 120;
        };
        if (occupancy < 0.25) {
            score -= 80;
        }
        if (isFamilyMovie(movie) && startTime.toLocalTime().isAfter(LocalTime.of(21, 0))) {
            score -= 100;
        }
        String reason = "Popularity " + popularity
                + ", day x" + round(dayFactor)
                + ", time x" + round(timeFactor)
                + ", room x" + round(roomFactor)
                + ", occupancy " + Math.round(occupancy * 100) + "%";
        return new Candidate(movie, room, startTime, endTime, expectedTickets, expectedRevenue, score, reason);
    }

    private boolean fits(List<Interval> intervals, LocalDateTime start, LocalDateTime end, int cleaningMinutes) {
        LocalDateTime bufferedStart = start.minusMinutes(cleaningMinutes);
        LocalDateTime bufferedEnd = end.plusMinutes(cleaningMinutes);
        for (Interval interval : intervals) {
            if (bufferedStart.isBefore(interval.end) && bufferedEnd.isAfter(interval.start)) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal calculateUtilization(List<Candidate> selected, int roomCount, GenerateSchedulePlanRequest request) {
        long days = Duration.between(request.getFromDate().atStartOfDay(), request.getToDate().plusDays(1).atStartOfDay()).toDays();
        long openMinutes = Duration.between(request.getOpeningTime(), request.getClosingTime()).toMinutes();
        long available = Math.max(1, days * roomCount * openMinutes);
        long used = selected.stream().mapToLong(candidate -> Duration.between(candidate.startTime, candidate.endTime).toMinutes()).sum();
        return BigDecimal.valueOf(used * 100.0 / available).setScale(2, RoundingMode.HALF_UP);
    }

    private void ensureShowtimeSeats(Showtime showtime) {
        for (Seat seat : activeSeats(showtime.getRoom())) {
            if (!showtimeSeatRepository.existsByShowtimeIdAndSeatId(showtime.getId(), seat.getId())) {
                ShowtimeSeat showtimeSeat = new ShowtimeSeat();
                showtimeSeat.setShowtime(showtime);
                showtimeSeat.setSeat(seat);
                showtimeSeat.setStatus("AVAILABLE");
                showtimeSeatRepository.save(showtimeSeat);
            }
        }
    }

    private TimeSlot findMatchingTimeSlot(LocalTime startTime) {
        return timeSlotRepository.findAll().stream()
                .filter(slot -> "ACTIVE".equalsIgnoreCase(slot.getStatus()))
                .filter(slot -> !startTime.isBefore(slot.getStartTime()) && startTime.isBefore(slot.getEndTime()))
                .findFirst()
                .orElse(null);
    }

    private AdminShowtimeResponse toShowtimeResponse(Showtime s) {
        long available = showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), "AVAILABLE");
        long locked = showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), "LOCKED");
        long booked = showtimeSeatRepository.countByShowtimeIdAndStatus(s.getId(), "BOOKED");
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
                .totalSeats(available + locked + booked)
                .availableSeats(available)
                .bookedSeats(booked)
                .build();
    }

    private SchedulePlanResponse toResponse(SchedulePlan plan, boolean includeItems) {
        List<SchedulePlanItemResponse> items = includeItems
                ? schedulePlanItemRepository.findByPlanIdOrderByStartTimeAsc(plan.getId()).stream().map(this::toItemResponse).toList()
                : List.of();
        return SchedulePlanResponse.builder()
                .planId(plan.getId())
                .fromDate(plan.getFromDate() != null ? plan.getFromDate() : plan.getBusinessDate())
                .toDate(plan.getToDate() != null ? plan.getToDate() : plan.getBusinessDate())
                .mode(plan.getMode())
                .status(plan.getStatus())
                .estimatedRevenue(plan.getEstimatedRevenue())
                .estimatedTickets(plan.getEstimatedTickets())
                .roomUtilization(plan.getRoomUtilization())
                .createdAt(plan.getGeneratedAt())
                .createdBy(plan.getGeneratedBy() != null ? plan.getGeneratedBy().getUsername() : null)
                .items(items)
                .build();
    }

    private SchedulePlanItemResponse toItemResponse(SchedulePlanItem item) {
        return SchedulePlanItemResponse.builder()
                .id(item.getId())
                .movieId(item.getMovie().getId())
                .movieTitle(item.getMovie().getTitle())
                .moviePopularity(item.getMovie().getPopularityScore() != null ? item.getMovie().getPopularityScore() : 50)
                .roomId(item.getRoom().getId())
                .roomName(item.getRoom().getName())
                .screenType(item.getRoom().getScreenType())
                .startTime(item.getStartTime())
                .endTime(item.getEndTime())
                .expectedTickets(item.getExpectedTickets())
                .expectedRevenue(item.getExpectedRevenue())
                .score(item.getScore())
                .reason(item.getReason())
                .showtimeId(item.getShowtime() != null ? item.getShowtime().getId() : null)
                .build();
    }

    private void validateRequest(GenerateSchedulePlanRequest request) {
        if (request.getFromDate() == null || request.getToDate() == null) {
            throw new IllegalArgumentException("fromDate và toDate là bắt buộc");
        }
        if (request.getToDate().isBefore(request.getFromDate())) {
            throw new IllegalArgumentException("toDate phải sau hoặc bằng fromDate");
        }
        if (!request.getClosingTime().isAfter(request.getOpeningTime())) {
            throw new IllegalArgumentException("closingTime phải sau openingTime");
        }
        if (request.getCleaningMinutes() == null || request.getCleaningMinutes() < 0) {
            request.setCleaningMinutes(15);
        }
    }

    private SchedulePlan findPlan(Long id) {
        return schedulePlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy schedule plan id=" + id));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }

    private List<Seat> activeSeats(Room room) {
        return seatRepository.findByRoomIdAndStatus(room.getId(), "ACTIVE");
    }

    private boolean isNowShowing(Movie movie) {
        return "NOW_SHOWING".equalsIgnoreCase(movie.getStatus()) || "ACTIVE".equalsIgnoreCase(movie.getStatus());
    }

    private double dayFactor(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case FRIDAY -> 1.2;
            case SATURDAY, SUNDAY -> 1.4;
            default -> 1.0;
        };
    }

    private double timeFactor(LocalTime time) {
        if (time.isBefore(LocalTime.NOON)) return 0.8;
        if (time.isBefore(LocalTime.of(16, 0))) return 1.0;
        if (time.isBefore(LocalTime.of(18, 30))) return 1.1;
        if (time.isBefore(LocalTime.of(21, 30))) return 1.5;
        return 0.9;
    }

    private double roomFactor(Room room) {
        String type = (room.getScreenType() != null ? room.getScreenType() : room.getName()).toUpperCase(Locale.ROOT);
        if (type.contains("IMAX")) return 1.25;
        if (type.contains("VIP")) return 1.15;
        if (type.contains("COUPLE")) return 1.10;
        return 1.0;
    }

    private double genreFactor(Movie movie) {
        String title = movie.getTitle() != null ? movie.getTitle().toLowerCase(Locale.ROOT) : "";
        if (title.contains("family") || title.contains("kid") || title.contains("cartoon")) return 1.08;
        if (title.contains("horror")) return 1.05;
        return 1.0;
    }

    private BigDecimal averageSeatPrice(Room room, LocalTime startTime) {
        List<Seat> seats = activeSeats(room);
        BigDecimal baseAverage = seats.isEmpty()
                ? BigDecimal.ZERO
                : seats.stream()
                    .map(seat -> seat.getBasePrice() != null ? seat.getBasePrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(seats.size()), 2, RoundingMode.HALF_UP);
        BigDecimal roomMultiplier = room.getPriceMultiplier() != null ? room.getPriceMultiplier() : BigDecimal.ONE;
        BigDecimal timeMultiplier = findMatchingTimeSlot(startTime) != null && findMatchingTimeSlot(startTime).getPriceMultiplier() != null
                ? findMatchingTimeSlot(startTime).getPriceMultiplier()
                : BigDecimal.ONE;
        return baseAverage.multiply(roomMultiplier).multiply(timeMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private double diversityBoost(Movie movie) {
        int priority = movie.getPriority() != null ? movie.getPriority() : 5;
        return Math.max(0, 60 - priority * 5);
    }

    private boolean isFamilyMovie(Movie movie) {
        String rating = movie.getAgeRating() != null ? movie.getAgeRating().toUpperCase(Locale.ROOT) : "";
        String title = movie.getTitle() != null ? movie.getTitle().toLowerCase(Locale.ROOT) : "";
        return rating.contains("P") || title.contains("family") || title.contains("kid") || title.contains("cartoon");
    }

    private String normalizeMode(String mode) {
        if ("BALANCED".equalsIgnoreCase(mode) || "MAX_UTILIZATION".equalsIgnoreCase(mode)) {
            return mode.toUpperCase(Locale.ROOT);
        }
        return "MAX_REVENUE";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record Interval(LocalDateTime start, LocalDateTime end, boolean existing) {}

    private record Candidate(
            Movie movie,
            Room room,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int expectedTickets,
            BigDecimal expectedRevenue,
            double score,
            String reason
    ) {}
}
