package com.opticine.controller;

import com.opticine.dto.admin.schedule.GenerateSchedulePlanRequest;
import com.opticine.service.AutoScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/schedule-plans")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class AdminSchedulePlanController {

    private final AutoScheduleService autoScheduleService;

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody GenerateSchedulePlanRequest request) {
        return ResponseEntity.ok(autoScheduleService.generate(request));
    }

    @GetMapping
    public ResponseEntity<?> recentPlans() {
        return ResponseEntity.ok(autoScheduleService.recentPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(autoScheduleService.getPlan(id));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<?> apply(@PathVariable Long id) {
        return ResponseEntity.ok(autoScheduleService.apply(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(autoScheduleService.cancel(id));
    }
}
