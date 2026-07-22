package com.opticine.controller;

import com.opticine.dto.admin.staff.StaffRequest;
import com.opticine.dto.admin.staff.StaffStatusRequest;
import com.opticine.service.AdminStaffService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/staff")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminStaffController {
    private final AdminStaffService adminStaffService;

    public AdminStaffController(AdminStaffService adminStaffService) {
        this.adminStaffService = adminStaffService;
    }

    @GetMapping
    public ResponseEntity<?> search(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String position,
                                    @RequestParam(required = false) String contractType) {
        return ResponseEntity.ok(adminStaffService.search(keyword, status, position, contractType));
    }

    @GetMapping("/next-username")
    public ResponseEntity<?> getNextUsername() {
        return ResponseEntity.ok(java.util.Map.of("nextUsername", adminStaffService.getNextUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminStaffService.getById(id));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> getAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(adminStaffService.getAvailability(id));
    }

    @GetMapping("/availability")
    public ResponseEntity<?> searchAvailability(@RequestParam(required = false) String position,
                                                @RequestParam(required = false) String contractType,
                                                @RequestParam(required = false) String dayOfWeek) {
        return ResponseEntity.ok(adminStaffService.searchAvailability(position, contractType, dayOfWeek));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody StaffRequest request) {
        return ResponseEntity.ok(adminStaffService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return ResponseEntity.ok(adminStaffService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @Valid @RequestBody StaffStatusRequest request) {
        return ResponseEntity.ok(adminStaffService.updateStatus(id, request.getStatus()));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(adminStaffService.resetPassword(id));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revoke(@PathVariable Long id) {
        return ResponseEntity.ok(adminStaffService.revoke(id));
    }
}
