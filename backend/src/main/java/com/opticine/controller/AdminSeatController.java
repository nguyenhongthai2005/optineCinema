package com.opticine.controller;

import com.opticine.dto.admin.seat.SeatRequest;
import com.opticine.dto.admin.seat.SeatStatusRequest;
import com.opticine.service.AdminSeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class AdminSeatController {

    private final AdminSeatService adminSeatService;

    /** GET /api/admin/rooms/{roomId}/seats */
    @GetMapping("/admin/rooms/{roomId}/seats")
    public ResponseEntity<?> getByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(adminSeatService.getByRoom(roomId));
    }

    /** GET /api/admin/seats/{id} */
    @GetMapping("/admin/seats/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminSeatService.getById(id));
    }

    /** POST /api/admin/rooms/{roomId}/seats */
    @PostMapping("/admin/rooms/{roomId}/seats")
    public ResponseEntity<?> create(@PathVariable Long roomId,
                                    @Valid @RequestBody SeatRequest request) {
        return ResponseEntity.ok(adminSeatService.create(roomId, request));
    }

    /** PUT /api/admin/seats/{id} */
    @PutMapping("/admin/seats/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody SeatRequest request) {
        return ResponseEntity.ok(adminSeatService.update(id, request));
    }

    /** PATCH /api/admin/seats/{id}/status */
    @PatchMapping("/admin/seats/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody SeatStatusRequest request) {
        return ResponseEntity.ok(adminSeatService.updateStatus(id, request.getStatus()));
    }

    @PatchMapping("/admin/seats/{id}/type")
    public ResponseEntity<?> updateType(@PathVariable Long id,
                                        @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(adminSeatService.updateType(id, String.valueOf(request.get("seatType"))));
    }

    @PatchMapping("/admin/seats/bulk/type")
    public ResponseEntity<?> updateTypes(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) request.get("seatIds");
        List<Long> seatIds = rawIds == null ? List.of() : rawIds.stream().map(Number::longValue).toList();
        return ResponseEntity.ok(adminSeatService.updateTypes(seatIds, String.valueOf(request.get("seatType"))));
    }

    @PatchMapping("/admin/seats/{id}/maintenance")
    public ResponseEntity<?> updateMaintenance(@PathVariable Long id,
                                               @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(adminSeatService.updateMaintenance(id, Boolean.TRUE.equals(request.get("maintenance"))));
    }

    @PatchMapping("/admin/seats/bulk/maintenance")
    public ResponseEntity<?> updateMaintenanceBulk(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) request.get("seatIds");
        List<Long> seatIds = rawIds == null ? List.of() : rawIds.stream().map(Number::longValue).toList();
        return ResponseEntity.ok(adminSeatService.updateMaintenanceBulk(seatIds, Boolean.TRUE.equals(request.get("maintenance"))));
    }

    /** DELETE /api/admin/seats/{id} */
    @DeleteMapping("/admin/seats/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        adminSeatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
