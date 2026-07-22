package com.opticine.service;

import com.opticine.dto.admin.room.RoomRequest;
import com.opticine.dto.admin.room.RoomResponse;
import com.opticine.entity.Room;
import com.opticine.repository.RoomRepository;
import com.opticine.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminRoomService {

    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final AdminSeatService adminSeatService;

    /** Lấy tất cả phòng chiếu, có thể lọc theo keyword (tên). */
    @Transactional(readOnly = true)
    public List<RoomResponse> search(String keyword) {
        List<Room> rooms = roomRepository.findAll();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase(Locale.ROOT);
            rooms = rooms.stream()
                    .filter(r -> r.getName().toLowerCase(Locale.ROOT).contains(kw))
                    .toList();
        }
        return rooms.stream().map(this::toResponse).toList();
    }

    /** Lấy chi tiết một phòng. */
    @Transactional(readOnly = true)
    public RoomResponse getById(Long id) {
        return toResponse(findRoom(id));
    }

    /** Tạo phòng mới. */
    @Transactional
    public RoomResponse create(RoomRequest request) {
        validateRequest(request);
        ensureNameAvailable(request.getName().trim(), null);
        Room room = Room.builder()
                .name(request.getName().trim())
                .totalRows(rowCount(request))
                .totalColumns(columnCount(request))
                .status(normalizeStatus(request.getStatus()))
                .priceMultiplier(request.getPriceMultiplier() != null
                        ? request.getPriceMultiplier() : BigDecimal.ONE)
                .screenType(request.getScreenType())
                .build();
        Room saved = roomRepository.save(room);
        adminSeatService.generateSeatsForRoom(saved, saved.getTotalRows(), saved.getTotalColumns());
        return toResponse(saved);
    }

    /** Cập nhật thông tin phòng. */
    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        validateRequest(request);
        ensureNameAvailable(request.getName().trim(), id);
        Room room = findRoom(id);
        int newRows = rowCount(request);
        int newColumns = columnCount(request);
        boolean gridChanged = !room.getTotalRows().equals(newRows)
                || !room.getTotalColumns().equals(newColumns);
        room.setName(request.getName().trim());
        room.setTotalRows(newRows);
        room.setTotalColumns(newColumns);
        if (StringUtils.hasText(request.getStatus())) {
            room.setStatus(normalizeStatus(request.getStatus()));
        }
        if (request.getPriceMultiplier() != null) {
            room.setPriceMultiplier(request.getPriceMultiplier());
        }
        if (request.getScreenType() != null) {
            room.setScreenType(request.getScreenType());
        }
        Room saved = roomRepository.save(room);
        if (gridChanged) {
            adminSeatService.resizeSeatsForRoom(saved, saved.getTotalRows(), saved.getTotalColumns());
        }
        return toResponse(saved);
    }

    @Transactional
    public List<com.opticine.dto.admin.seat.SeatResponse> generateSeats(Long roomId, int rowCount, int columnCount) {
        Room room = findRoom(roomId);
        room.setTotalRows(rowCount);
        room.setTotalColumns(columnCount);
        roomRepository.save(room);
        return adminSeatService.generateSeatsForRoom(room, rowCount, columnCount);
    }

    /** Cập nhật trạng thái phòng (ACTIVE / INACTIVE / MAINTENANCE). */
    @Transactional
    public RoomResponse updateStatus(Long id, String status) {
        Room room = findRoom(id);
        room.setStatus(normalizeStatus(status));
        return toResponse(roomRepository.save(room));
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu id=" + id));
    }

    private void validateRequest(RoomRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Tên phòng không được để trống.");
        }
        if (rowCount(request) <= 0) {
            throw new IllegalArgumentException("Số hàng ghế phải lớn hơn 0.");
        }
        if (columnCount(request) <= 0) {
            throw new IllegalArgumentException("Số cột ghế phải lớn hơn 0.");
        }
        if (rowCount(request) > 26 || columnCount(request) > 30) {
            throw new IllegalArgumentException("Không thể tạo ghế cho phòng này.");
        }
    }

    private void ensureNameAvailable(String name, Long currentRoomId) {
        roomRepository.findByName(name).ifPresent(existing -> {
            if (currentRoomId == null || !existing.getId().equals(currentRoomId)) {
                throw new IllegalArgumentException("Tên phòng đã tồn tại.");
            }
        });
    }

    private int rowCount(RoomRequest request) {
        return request.getTotalRows() != null ? request.getTotalRows() : request.getRowCount() != null ? request.getRowCount() : 0;
    }

    private int columnCount(RoomRequest request) {
        return request.getTotalColumns() != null ? request.getTotalColumns() : request.getColumnCount() != null ? request.getColumnCount() : 0;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "ACTIVE";
    }

    private RoomResponse toResponse(Room room) {
        int totalSeats = seatRepository.findByRoomId(room.getId()).size();
        int capacity = room.getTotalRows() * room.getTotalColumns();
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .roomType(room.getScreenType())
                .totalRows(room.getTotalRows())
                .totalColumns(room.getTotalColumns())
                .rowCount(room.getTotalRows())
                .columnCount(room.getTotalColumns())
                .capacity(capacity)
                .totalSeats(totalSeats)
                .seatCount(totalSeats)
                .status(room.getStatus())
                .priceMultiplier(room.getPriceMultiplier())
                .screenType(room.getScreenType())
                .build();
    }
}
