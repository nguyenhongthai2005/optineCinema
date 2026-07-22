package com.opticine.controller;

import com.opticine.dto.admin.customer.CustomerStatusRequest;
import com.opticine.service.AdminCustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {
    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerService adminCustomerService) {
        this.adminCustomerService = adminCustomerService;
    }

    @GetMapping
    public ResponseEntity<?> search(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(adminCustomerService.search(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminCustomerService.getDetail(id));
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<?> getBookings(@PathVariable Long id) {
        return ResponseEntity.ok(adminCustomerService.getBookings(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @Valid @RequestBody CustomerStatusRequest request) {
        return ResponseEntity.ok(adminCustomerService.updateStatus(id, request.getStatus()));
    }

    @PostMapping("/recalculate-spending")
    public ResponseEntity<?> recalculateSpending() {
        return ResponseEntity.ok(adminCustomerService.recalculateSpending());
    }
}
