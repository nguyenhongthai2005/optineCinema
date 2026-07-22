package com.opticine.controller;

import com.opticine.dto.admin.staff.StaffAssignmentRequest;
import com.opticine.service.StaffSchedulingService;
import com.opticine.service.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/admin/staff-assignments")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class AdminStaffAssignmentController {
    private final StaffSchedulingService schedulingService;

    @GetMapping
    public ResponseEntity<?> search(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(required = false) String position,
                                    @RequestParam(required = false) Long staffId,
                                    @RequestParam(required = false) String status) {
        return ResponseEntity.ok(schedulingService.searchAssignments(date, position, staffId, status));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<?> suggestions(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                         @RequestParam String assignmentType,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        return ResponseEntity.ok(schedulingService.suggestions(date, assignmentType, startTime, endTime));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody StaffAssignmentRequest request,
                                    @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(schedulingService.createAssignment(request, currentUser.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody StaffAssignmentRequest request) {
        return ResponseEntity.ok(schedulingService.updateAssignment(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(schedulingService.cancelAssignment(id));
    }
}
