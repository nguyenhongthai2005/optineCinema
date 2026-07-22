package com.opticine.controller;

import com.opticine.dto.admin.timeslot.TimeSlotRequest;
import com.opticine.dto.admin.timeslot.TimeSlotStatusRequest;
import com.opticine.service.AdminTimeSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/timeslots")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTimeSlotController {

    private final AdminTimeSlotService adminTimeSlotService;

    /** GET /api/admin/timeslots */
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(adminTimeSlotService.getAll());
    }

    /** GET /api/admin/timeslots/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminTimeSlotService.getById(id));
    }

    /** POST /api/admin/timeslots */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TimeSlotRequest request) {
        return ResponseEntity.ok(adminTimeSlotService.create(request));
    }

    /** PUT /api/admin/timeslots/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody TimeSlotRequest request) {
        return ResponseEntity.ok(adminTimeSlotService.update(id, request));
    }

    /** PATCH /api/admin/timeslots/{id}/status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody TimeSlotStatusRequest request) {
        return ResponseEntity.ok(adminTimeSlotService.updateStatus(id, request.getStatus()));
    }
}
