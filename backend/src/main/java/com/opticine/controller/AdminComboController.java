package com.opticine.controller;

import com.opticine.dto.admin.combo.AdminComboRequest;
import com.opticine.dto.admin.combo.AdminComboResponse;
import com.opticine.dto.admin.combo.AdminComboStatusRequest;
import com.opticine.service.AdminComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/combos")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class AdminComboController {
    private final AdminComboService adminComboService;

    @GetMapping
    public ResponseEntity<List<AdminComboResponse>> getCombos(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(adminComboService.findCombos(keyword, status, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminComboResponse> getCombo(@PathVariable Long id) {
        return ResponseEntity.ok(adminComboService.getCombo(id));
    }

    @PostMapping
    public ResponseEntity<AdminComboResponse> createCombo(@RequestBody AdminComboRequest request) {
        return ResponseEntity.ok(adminComboService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminComboResponse> updateCombo(@PathVariable Long id, @RequestBody AdminComboRequest request) {
        return ResponseEntity.ok(adminComboService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminComboResponse> updateStatus(@PathVariable Long id, @RequestBody AdminComboStatusRequest request) {
        return ResponseEntity.ok(adminComboService.updateStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCombo(@PathVariable Long id) {
        adminComboService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
