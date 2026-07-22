package com.opticine.controller;

import com.opticine.dto.showtime.request.LockSeatRequest;
import com.opticine.dto.showtime.response.LockSeatResponse;
import com.opticine.service.StaffService;
import com.opticine.service.ShowtimeSeatService;
import com.opticine.service.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/showtimes")
@RequiredArgsConstructor
public class ShowtimeSeatController {

    private final ShowtimeSeatService seatService;
    private final StaffService staffService;

    /**
     * POST /api/showtimes/{id}/seats/lock
     * Body: { "seatIds": [1, 2] }
     * Yêu cầu đăng nhập (JWT trong header Authorization)
     */
    @PostMapping("/{id}/seats/lock")
    public ResponseEntity<LockSeatResponse> lockSeats(
            @PathVariable("id") Long id,
            @Valid @RequestBody LockSeatRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        staffService.denyTicketCheckerSalesAccess(currentUser);
        return ResponseEntity.ok(seatService.lockSeats(id, request, currentUser.getId()));
    }

    /**
     * DELETE /api/showtimes/{id}/seats/lock
     * User chủ động huỷ ghế đang giữ
     */
    @DeleteMapping("/{id}/seats/lock")
    public ResponseEntity<Void> releaseSeats(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        seatService.releaseSeats(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
