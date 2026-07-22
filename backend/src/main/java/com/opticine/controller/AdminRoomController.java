package com.opticine.controller;

import com.opticine.dto.admin.room.RoomRequest;
import com.opticine.dto.admin.room.RoomStatusRequest;
import com.opticine.dto.admin.seat.SeatGridRequest;
import com.opticine.service.AdminRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/rooms")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    /** GET /api/admin/rooms?keyword=... */
    @GetMapping
    public ResponseEntity<?> search(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(adminRoomService.search(keyword));
    }

    /** GET /api/admin/rooms/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminRoomService.getById(id));
    }

    /** POST /api/admin/rooms */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(adminRoomService.create(request));
    }

    /** PUT /api/admin/rooms/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(adminRoomService.update(id, request));
    }

    /** PATCH /api/admin/rooms/{id}/status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody RoomStatusRequest request) {
        return ResponseEntity.ok(adminRoomService.updateStatus(id, request.getStatus()));
    }

    @PostMapping("/{id}/generate-seats")
    public ResponseEntity<?> generateSeats(@PathVariable Long id,
                                           @Valid @RequestBody SeatGridRequest request) {
        return ResponseEntity.ok(adminRoomService.generateSeats(id, request.getRowCount(), request.getColumnCount()));
    }
}
