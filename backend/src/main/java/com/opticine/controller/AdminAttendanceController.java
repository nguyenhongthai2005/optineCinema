package com.opticine.controller;

import com.opticine.dto.admin.attendance.AttendanceRequest;
import com.opticine.service.AdminAttendanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/attendance")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminAttendanceController {
    private final AdminAttendanceService adminAttendanceService;

    public AdminAttendanceController(AdminAttendanceService adminAttendanceService) {
        this.adminAttendanceService = adminAttendanceService;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(adminAttendanceService.search(staffId, fromDate, toDate, status));
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<?> monthlySummary(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam(required = false) Long staffId
    ) {
        return ResponseEntity.ok(adminAttendanceService.monthlySummary(month, year, staffId));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(adminAttendanceService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(adminAttendanceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        adminAttendanceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
