package com.opticine.service;

import com.opticine.dto.admin.seat.SeatRequest;
import com.opticine.dto.admin.seat.SeatResponse;
import com.opticine.entity.Room;
import com.opticine.entity.Seat;
import com.opticine.repository.RoomRepository;
import com.opticine.repository.SeatRepository;
import com.opticine.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;

    /** Lấy tất cả ghế theo phòng. */
    @Transactional(readOnly = true)
    public List<SeatResponse> getByRoom(Long roomId) {
        findRoom(roomId); // validate phòng tồn tại
        return seatRepository.findByRoomId(roomId).stream()
                .sorted(java.util.Comparator.comparing(Seat::getRowLabel).thenComparing(Seat::getColumnNumber))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<SeatResponse> generateSeatsForRoom(Room room, int rowCount, int columnCount) {
        validateGrid(rowCount, columnCount);
        addMissingSeats(room, rowCount, columnCount);
        return getByRoom(room.getId());
    }

    @Transactional
    public List<SeatResponse> resizeSeatsForRoom(Room room, int rowCount, int columnCount) {
        validateGrid(rowCount, columnCount);
        addMissingSeats(room, rowCount, columnCount);

        List<Seat> seats = seatRepository.findByRoomId(room.getId());
        List<Seat> outsideGrid = seats.stream()
                .filter(seat -> rowIndex(seat.getRowLabel()) > rowCount || seat.getColumnNumber() > columnCount)
                .toList();
        boolean hasDependencies = outsideGrid.stream().anyMatch(seat -> showtimeSeatRepository.existsBySeatId(seat.getId()));
        if (hasDependencies) {
            throw new IllegalStateException("Không thể giảm kích thước phòng vì một số ghế đã có lịch chiếu hoặc vé đã bán.");
        }
        outsideGrid.forEach(seat -> seat.setStatus("INACTIVE"));
        seatRepository.saveAll(outsideGrid);
        return getByRoom(room.getId());
    }

    /** Lấy chi tiết ghế. */
    @Transactional(readOnly = true)
    public SeatResponse getById(Long id) {
        return toResponse(findSeat(id));
    }

    /** Thêm một ghế vào phòng. */
    @Transactional
    public SeatResponse create(Long roomId, SeatRequest request) {
        Room room = findRoom(roomId);

        // Kiểm tra trùng vị trí
        boolean exists = seatRepository.findByRoomId(roomId).stream()
                .anyMatch(s -> s.getRowLabel().equalsIgnoreCase(request.getRowLabel())
                        && s.getColumnNumber().equals(request.getColumnNumber()));
        if (exists) {
            throw new IllegalArgumentException(
                    "Ghế " + request.getRowLabel() + request.getColumnNumber() + " đã tồn tại trong phòng này");
        }

        Seat pairedSeat = null;
        if (request.getPairedSeatId() != null) {
            pairedSeat = findSeat(request.getPairedSeatId());
        }

        Seat seat = Seat.builder()
                .room(room)
                .rowLabel(request.getRowLabel().toUpperCase(Locale.ROOT))
                .columnNumber(request.getColumnNumber())
                .seatType(request.getSeatType())
                .basePrice(request.getBasePrice())
                .status(normalizeStatus(request.getStatus()))
                .pairedSeat(pairedSeat)
                .build();

        return toResponse(seatRepository.save(seat));
    }

    /** Cập nhật thông tin ghế. */
    @Transactional
    public SeatResponse update(Long id, SeatRequest request) {
        Seat seat = findSeat(id);

        seat.setRowLabel(request.getRowLabel().toUpperCase(Locale.ROOT));
        seat.setColumnNumber(request.getColumnNumber());
        if (request.getSeatType() != null) {
            seat.setSeatType(request.getSeatType());
        }
        seat.setBasePrice(request.getBasePrice());
        if (StringUtils.hasText(request.getStatus())) {
            seat.setStatus(normalizeStatus(request.getStatus()));
        }
        if (request.getPairedSeatId() != null) {
            seat.setPairedSeat(findSeat(request.getPairedSeatId()));
        } else {
            seat.setPairedSeat(null);
        }

        return toResponse(seatRepository.save(seat));
    }

    /** Cập nhật trạng thái ghế (ACTIVE / INACTIVE / MAINTENANCE). */
    @Transactional
    public SeatResponse updateStatus(Long id, String status) {
        Seat seat = findSeat(id);
        seat.setStatus(normalizeStatus(status));
        return toResponse(seatRepository.save(seat));
    }

    @Transactional
    public SeatResponse updateType(Long id, String seatType) {
        Seat seat = findSeat(id);
        applySeatType(seat, seatType);
        return toResponse(seatRepository.save(seat));
    }

    @Transactional
    public List<SeatResponse> updateTypes(List<Long> seatIds, String seatType) {
        List<Seat> seats = loadSeats(seatIds);
        seats.forEach(seat -> applySeatType(seat, seatType));
        return seatRepository.saveAll(seats).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SeatResponse updateMaintenance(Long id, boolean maintenance) {
        Seat seat = findSeat(id);
        applyMaintenance(seat, maintenance);
        return toResponse(seatRepository.save(seat));
    }

    @Transactional
    public List<SeatResponse> updateMaintenanceBulk(List<Long> seatIds, boolean maintenance) {
        List<Seat> seats = loadSeats(seatIds);
        seats.forEach(seat -> applyMaintenance(seat, maintenance));
        return seatRepository.saveAll(seats).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Xóa ghế (chỉ khi không có booking). */
    @Transactional
    public void delete(Long id) {
        Seat seat = findSeat(id);
        seatRepository.delete(seat);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private Seat findSeat(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế id=" + id));
    }

    private Room findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu id=" + roomId));
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "ACTIVE";
    }

    private List<Seat> loadSeats(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một ghế.");
        }
        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("Có ghế không hợp lệ.");
        }
        return new ArrayList<>(seats);
    }

    private void applySeatType(Seat seat, String seatType) {
        String normalized = normalizeSeatType(seatType);
        seat.setSeatType(normalized);
        seat.setBasePrice(defaultBasePrice(normalized));
    }

    private String normalizeSeatType(String seatType) {
        String normalized = StringUtils.hasText(seatType) ? seatType.trim().toUpperCase(Locale.ROOT) : "NORMAL";
        if ("STANDARD".equals(normalized)) return "NORMAL";
        if (!List.of("NORMAL", "VIP", "COUPLE").contains(normalized)) {
            throw new IllegalArgumentException("Loại ghế không hợp lệ.");
        }
        return normalized;
    }

    private BigDecimal defaultBasePrice(String seatType) {
        if ("VIP".equals(seatType)) return BigDecimal.valueOf(90_000);
        if ("COUPLE".equals(seatType)) return BigDecimal.valueOf(140_000);
        return BigDecimal.valueOf(70_000);
    }

    private void applyMaintenance(Seat seat, boolean maintenance) {
        if (maintenance) {
            if (showtimeSeatRepository.existsLockedBySeatId(seat.getId())) {
                throw new IllegalStateException("Ghế đang được khách giữ, vui lòng thử lại sau.");
            }
            if (showtimeSeatRepository.existsFutureBookedBySeatId(seat.getId(), LocalDateTime.now())) {
                throw new IllegalStateException("Ghế đang có vé đã bán cho suất chiếu sắp tới, không thể chuyển sang bảo trì.");
            }
            seat.setStatus("MAINTENANCE");
        } else {
            seat.setStatus("ACTIVE");
        }
    }

    private void validateGrid(int rowCount, int columnCount) {
        if (rowCount <= 0) throw new IllegalArgumentException("Số hàng ghế phải lớn hơn 0.");
        if (columnCount <= 0) throw new IllegalArgumentException("Số cột ghế phải lớn hơn 0.");
        if (rowCount > 26 || columnCount > 30) {
            throw new IllegalArgumentException("Không thể tạo ghế cho phòng này.");
        }
    }

    private void addMissingSeats(Room room, int rowCount, int columnCount) {
        Map<String, Seat> existing = seatRepository.findByRoomId(room.getId()).stream()
                .collect(Collectors.toMap(seat -> seat.getRowLabel().toUpperCase(Locale.ROOT) + "-" + seat.getColumnNumber(), seat -> seat, (a, b) -> a));
        for (int row = 1; row <= rowCount; row++) {
            String rowLabel = rowLabel(row);
            for (int col = 1; col <= columnCount; col++) {
                String key = rowLabel + "-" + col;
                Seat existingSeat = existing.get(key);
                if (existingSeat != null) {
                    if ("INACTIVE".equalsIgnoreCase(existingSeat.getStatus())) {
                        existingSeat.setStatus("ACTIVE");
                        seatRepository.save(existingSeat);
                    }
                    continue;
                }
                Seat seat = Seat.builder()
                        .room(room)
                        .rowLabel(rowLabel)
                        .columnNumber(col)
                        .seatType("NORMAL")
                        .basePrice(BigDecimal.valueOf(70_000))
                        .status("ACTIVE")
                        .build();
                seatRepository.save(seat);
            }
        }
    }

    private String rowLabel(int rowNumber) {
        return String.valueOf((char) ('A' + rowNumber - 1));
    }

    private int rowIndex(String rowLabel) {
        if (!StringUtils.hasText(rowLabel)) return Integer.MAX_VALUE;
        return rowLabel.trim().toUpperCase(Locale.ROOT).charAt(0) - 'A' + 1;
    }

    private SeatResponse toResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .roomId(seat.getRoom().getId())
                .roomName(seat.getRoom().getName())
                .rowLabel(seat.getRowLabel())
                .columnNumber(seat.getColumnNumber())
                .seatLabel(seat.getRowLabel() + seat.getColumnNumber())
                .seatType(seat.getSeatType())
                .seatTypeLabel(seatTypeLabel(seat.getSeatType()))
                .basePrice(seat.getBasePrice())
                .status(seat.getStatus())
                .statusLabel(statusLabel(seat.getStatus()))
                .pairedSeatId(seat.getPairedSeat() != null ? seat.getPairedSeat().getId() : null)
                .build();
    }

    private String seatTypeLabel(String seatType) {
        if ("VIP".equalsIgnoreCase(seatType)) return "VIP";
        if ("COUPLE".equalsIgnoreCase(seatType)) return "Đôi";
        return "Thường";
    }

    private String statusLabel(String status) {
        if ("MAINTENANCE".equalsIgnoreCase(status)) return "Bảo trì";
        if ("INACTIVE".equalsIgnoreCase(status)) return "Ngừng hoạt động";
        return "Hoạt động";
    }
}
