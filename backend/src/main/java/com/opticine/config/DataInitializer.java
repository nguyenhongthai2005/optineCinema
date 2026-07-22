package com.opticine.config;

import com.opticine.entity.*;
import com.opticine.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Autowired
    private ComboRepository comboRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private PasswordEncoder encoder;

    @Value("${demo.staff-password:}")
    private String demoStaffPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CUSTOMER").build()));
        Role staffRole = roleRepository.findByName("ROLE_STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_STAFF").build()));
        roleRepository.findByName("ROLE_MANAGER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_MANAGER").build()));

        seedAdmin(adminRole);
        seedStaffIfConfigured(staffRole);
        seedCustomer(customerRole);

        List<Movie> movies = seedMovies();
        List<Room> rooms = seedRooms();
        rooms.forEach(this::seedSeats);
        List<TimeSlot> slots = seedTimeSlots();
        seedShowtimes(movies, rooms, slots);
        seedMemberships();
        seedCombos();
    }

    private void seedAdmin(Role adminRole) {
        if (userRepository.existsByUsername("admin"))
            return;

        User admin = new User();
        admin.setFullName("Quan Tri Vien");
        admin.setEmail("admin@gmail.com");
        admin.setPhone("0999999999");
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("123456"));
        admin.setStatus("ACTIVE");
        admin.setEnabled(true);
        admin.setRoles(new HashSet<>(Set.of(adminRole)));
        userRepository.save(admin);
    }

    private void seedStaffIfConfigured(Role staffRole) {
        if (userRepository.existsByUsername("staff")) {
            userRepository.findByUsername("staff").ifPresent(existing -> {
                boolean changed = false;
                if (existing.getStaffPosition() == null || "COUNTER_STAFF".equals(existing.getStaffPosition())) {
                    existing.setStaffPosition("COUNTER_SALES");
                    changed = true;
                }
                if (existing.getEmploymentType() == null) {
                    existing.setEmploymentType("FULL_TIME");
                    changed = true;
                }
                if (changed) {
                    userRepository.save(existing);
                }
            });
            return;
        }
        if (demoStaffPassword == null || demoStaffPassword.isBlank())
            return;

        User staff = new User();
        staff.setFullName("Demo Staff");
        staff.setEmail("staff@opticine.local");
        staff.setPhone("0777777777");
        staff.setUsername("staff");
        staff.setPassword(encoder.encode(demoStaffPassword));
        staff.setStatus("ACTIVE");
        staff.setEnabled(true);
        staff.setEmploymentType("FULL_TIME");
        staff.setStaffPosition("COUNTER_SALES");
        staff.setRoles(new HashSet<>(Set.of(staffRole)));
        userRepository.save(staff);
    }

    private void seedCustomer(Role customerRole) {
        User user = userRepository.findByUsername("user").orElseGet(() -> {
            User created = new User();
            created.setFullName("Khach Hang");
            created.setEmail("user@gmail.com");
            created.setPhone("0888888888");
            created.setUsername("user");
            created.setPassword(encoder.encode("123456"));
            created.setStatus("ACTIVE");
            created.setEnabled(true);
            created.setRoles(new HashSet<>(Set.of(customerRole)));
            return userRepository.save(created);
        });

        customerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Customer customer = new Customer();
            customer.setUser(user);
            customer.setFullName(user.getFullName());
            customer.setEmail(user.getEmail());
            customer.setPhone(user.getPhone());
            customer.setTotalSpent(BigDecimal.ZERO);
            customer.setPoints(0);
            return customerRepository.save(customer);
        });
    }

    private List<Movie> seedMovies() {
        MovieSpec[] specs = {
                new MovieSpec("Doraemon: Nobita's Art World Tales",
                        "Doraemon và nhóm bạn bước vào thế giới hội họa kỳ ảo để bảo vệ một báu vật cổ.", "Hoạt hình",
                        105, "P", "https://image.tmdb.org/t/p/w500/9YEGawvjaRgnyW6QVcUhFJPFDco.jpg",
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", LocalDate.now().minusDays(20), 1, 72),
                new MovieSpec("Detective Conan Movie",
                        "Conan đối đầu một vụ án lớn với những bí mật liên quan đến cảnh sát và tổ chức áo đen.",
                        "Trinh thám", 110, "C13", "https://image.tmdb.org/t/p/w500/7sJrNKwzyJWnFPFpDL9wnZ859LZ.jpg",
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", LocalDate.now().minusDays(14), 2, 80),
                new MovieSpec("Lat Mat 8",
                        "Một câu chuyện gia đình, tuổi trẻ và lựa chọn nghề nghiệp trong màu sắc điện ảnh Việt.",
                        "Tâm lý", 135, "C13", "https://image.tmdb.org/t/p/w500/5MwkWH9tYHv3mV9OdYTMR5qreIz.jpg",
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", LocalDate.now().minusDays(10), 3, 76),
                new MovieSpec("Mission: Impossible",
                        "Ethan Hunt tiếp tục một nhiệm vụ nghẹt thở với các pha hành động tốc độ cao.", "Hành động",
                        150, "C16", "https://image.tmdb.org/t/p/w500/z53D7bOFuXFvVjdRkJXqRiZpaSx.jpg",
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", LocalDate.now().minusDays(30), 4, 86),
                new MovieSpec("Inside Out 2",
                        "Riley lớn lên cùng những cảm xúc mới, mở ra một hành trình hài hước và ấm áp.", "Gia đình", 96,
                        "P", "https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg",
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", LocalDate.now().minusDays(25), 5, 88),
                new MovieSpec("Avengers: Endgame",
                        "Các siêu anh hùng còn lại tập hợp cho trận chiến cuối cùng để đảo ngược cú búng tay.",
                        "Siêu anh hùng", 181, "C13", "https://image.tmdb.org/t/p/w500/or06FN3Dka5tukK1e9sl16pB3iy.jpg",
                        "https://www.youtube.com/watch?v=dQw4w9WgXcQ", LocalDate.now().minusYears(1), 6, 90)
        };

        for (MovieSpec spec : specs) {
            movieRepository.findByTitle(spec.title()).map(existing -> {
                boolean changed = false;
                if (existing.getGenre() == null || existing.getGenre().isBlank()) {
                    existing.setGenre(spec.genre());
                    changed = true;
                }
                if (existing.getPopularityScore() == null) {
                    existing.setPopularityScore(spec.popularityScore());
                    changed = true;
                }
                if (existing.getTrailerUrl() == null || existing.getTrailerUrl().isBlank()) {
                    existing.setTrailerUrl(spec.trailerUrl());
                    changed = true;
                }
                return changed ? movieRepository.save(existing) : existing;
            }).orElseGet(() -> {
                Movie movie = new Movie();
                movie.setTitle(spec.title());
                movie.setDescription(spec.description());
                movie.setGenre(spec.genre());
                movie.setDurationMinutes(spec.duration());
                movie.setAgeRating(spec.ageRating());
                movie.setPosterUrl(spec.posterUrl());
                movie.setTrailerUrl(spec.trailerUrl());
                movie.setReleaseDate(spec.releaseDate());
                movie.setEndDate(LocalDate.now().plusMonths(3));
                movie.setStatus("NOW_SHOWING");
                movie.setPriority(spec.priority());
                movie.setPopularityScore(spec.popularityScore());
                return movieRepository.save(movie);
            });
        }
        return movieRepository.findByStatusOrderByPriorityAscTitleAsc("NOW_SHOWING");
    }

    private List<Room> seedRooms() {
        Room standard = seedRoom("Room 1 - Standard", 5, 10, "2D", BigDecimal.valueOf(1.0));
        Room imax = seedRoom("Room 2 - IMAX", 5, 12, "IMAX", BigDecimal.valueOf(1.25));
        Room couple = seedRoom("Room 3 - Couple", 5, 8, "COUPLE", BigDecimal.valueOf(1.15));
        return List.of(standard, imax, couple);
    }

    private void seedCombos() {
        if (comboRepository.count() > 0)
            return;
        seedCombo("Combo Solo", "1 bắp rang bơ vừa + 1 nước ngọt", "POPCORN_DRINK", BigDecimal.valueOf(79000),
                "https://images.unsplash.com/photo-1578849278619-e73505e9610f?auto=format&fit=crop&w=800&q=80");
        seedCombo("Combo Couple", "1 bắp rang bơ lớn + 2 nước ngọt", "COMBO", BigDecimal.valueOf(129000),
                "https://images.unsplash.com/photo-1585647347384-2593bc35786b?auto=format&fit=crop&w=800&q=80");
        seedCombo("Combo Family", "2 bắp vừa + 4 nước ngọt", "COMBO", BigDecimal.valueOf(219000),
                "https://images.unsplash.com/photo-1512149177596-f817c7ef5d4c?auto=format&fit=crop&w=800&q=80");
        seedCombo("Pepsi Ly Lớn", "Nước ngọt ly lớn dùng tại rạp", "DRINK", BigDecimal.valueOf(35000),
                "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=800&q=80");
    }

    private void seedCombo(String name, String description, String category, BigDecimal price, String imageUrl) {
        Combo combo = new Combo();
        combo.setName(name);
        combo.setDescription(description);
        combo.setCategory(category);
        combo.setPrice(price);
        combo.setImageUrl(imageUrl);
        combo.setStatus("ACTIVE");
        combo.setStockQuantity(500);
        comboRepository.save(combo);
    }

    private Room seedRoom(String name, int rows, int columns, String screenType, BigDecimal multiplier) {
        return roomRepository.findByName(name).orElseGet(() -> {
            Room room = new Room();
            room.setName(name);
            room.setTotalRows(rows);
            room.setTotalColumns(columns);
            room.setScreenType(screenType);
            room.setPriceMultiplier(multiplier);
            room.setStatus("ACTIVE");
            return roomRepository.save(room);
        });
    }

    private void seedSeats(Room room) {
        String[] rows = { "A", "B", "C", "D", "E" };
        for (int r = 0; r < rows.length; r++) {
            for (int col = 1; col <= room.getTotalColumns(); col++) {
                if (seatRepository.existsByRoomIdAndRowLabelAndColumnNumber(room.getId(), rows[r], col)) {
                    continue;
                }
                String type = r < 2 ? "NORMAL" : r < 4 ? "VIP" : "COUPLE";
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setRowLabel(rows[r]);
                seat.setColumnNumber(col);
                seat.setSeatType(type);
                seat.setBasePrice("NORMAL".equals(type) ? BigDecimal.valueOf(70_000)
                        : "VIP".equals(type) ? BigDecimal.valueOf(90_000)
                                : BigDecimal.valueOf(140_000));
                seat.setStatus("ACTIVE");
                seatRepository.save(seat);
            }
        }
    }

    private List<TimeSlot> seedTimeSlots() {
        TimeSlot morning = seedTimeSlot("Morning", LocalTime.of(10, 0), LocalTime.of(12, 30), BigDecimal.valueOf(1.0));
        TimeSlot afternoon = seedTimeSlot("Afternoon", LocalTime.of(13, 30), LocalTime.of(16, 0),
                BigDecimal.valueOf(1.1));
        TimeSlot evening = seedTimeSlot("Evening", LocalTime.of(19, 30), LocalTime.of(22, 0), BigDecimal.valueOf(1.2));
        TimeSlot late = seedTimeSlot("Late Night", LocalTime.of(22, 0), LocalTime.of(23, 59), BigDecimal.valueOf(1.3));
        return List.of(morning, afternoon, evening, late);
    }

    private TimeSlot seedTimeSlot(String name, LocalTime start, LocalTime end, BigDecimal multiplier) {
        return timeSlotRepository.findByName(name).orElseGet(() -> {
            TimeSlot slot = new TimeSlot();
            slot.setName(name);
            slot.setStartTime(start);
            slot.setEndTime(end);
            slot.setPriceMultiplier(multiplier);
            slot.setStatus("ACTIVE");
            return timeSlotRepository.save(slot);
        });
    }

    private void seedShowtimes(List<Movie> movies, List<Room> rooms, List<TimeSlot> slots) {
        LocalTime[] starts = { LocalTime.of(10, 0), LocalTime.of(13, 30), LocalTime.of(16, 0), LocalTime.of(19, 30),
                LocalTime.of(22, 0) };
        for (int dayOffset = 0; dayOffset <= 7; dayOffset++) {
            LocalDate date = LocalDate.now().plusDays(dayOffset);
            for (int i = 0; i < starts.length; i++) {
                Movie movie = movies.get((dayOffset + i) % movies.size());
                Room room = rooms.get((dayOffset + i) % rooms.size());
                TimeSlot slot = slotForStart(starts[i], slots);
                LocalDateTime start = LocalDateTime.of(date, starts[i]);
                LocalDateTime end = start.plusMinutes(movie.getDurationMinutes());

                Showtime showtime = showtimeRepository
                        .findByMovieIdAndRoomIdAndStartTime(movie.getId(), room.getId(), start)
                        .orElseGet(() -> {
                            Showtime created = new Showtime();
                            created.setMovie(movie);
                            created.setRoom(room);
                            created.setTimeSlot(slot);
                            created.setStartTime(start);
                            created.setEndTime(end);
                            created.setStatus("SCHEDULED");
                            return showtimeRepository.save(created);
                        });
                seedShowtimeSeats(showtime);
            }
        }
    }

    private TimeSlot slotForStart(LocalTime start, List<TimeSlot> slots) {
        if (start.isBefore(LocalTime.NOON))
            return slots.get(0);
        if (start.isBefore(LocalTime.of(17, 0)))
            return slots.get(1);
        if (start.isBefore(LocalTime.of(22, 0)))
            return slots.get(2);
        return slots.get(3);
    }

    private void seedShowtimeSeats(Showtime showtime) {
        List<Seat> seats = seatRepository.findByRoomIdAndStatus(showtime.getRoom().getId(), "ACTIVE");
        for (Seat seat : seats) {
            if (showtimeSeatRepository.existsByShowtimeIdAndSeatId(showtime.getId(), seat.getId())) {
                continue;
            }
            ShowtimeSeat showtimeSeat = new ShowtimeSeat();
            showtimeSeat.setShowtime(showtime);
            showtimeSeat.setSeat(seat);
            showtimeSeat.setStatus("AVAILABLE");
            showtimeSeatRepository.save(showtimeSeat);
        }
    }

    private void seedMemberships() {
        if (membershipRepository.count() > 0) return;
        membershipRepository.save(Membership.builder()
                .name("Bronze").minSpent(BigDecimal.ZERO).discountPercent(BigDecimal.ZERO).build());
        membershipRepository.save(Membership.builder()
                .name("Silver").minSpent(BigDecimal.valueOf(1_000_000)).discountPercent(BigDecimal.valueOf(5)).build());
        membershipRepository.save(Membership.builder()
                .name("Gold").minSpent(BigDecimal.valueOf(5_000_000)).discountPercent(BigDecimal.valueOf(10)).build());
        membershipRepository.save(Membership.builder()
                .name("Platinum").minSpent(BigDecimal.valueOf(15_000_000)).discountPercent(BigDecimal.valueOf(15)).build());
    }

    private record MovieSpec(
            String title,
            String description,
            String genre,
            int duration,
            String ageRating,
            String posterUrl,
            String trailerUrl,
            LocalDate releaseDate,
            Integer priority,
            Integer popularityScore) {
    }
}
