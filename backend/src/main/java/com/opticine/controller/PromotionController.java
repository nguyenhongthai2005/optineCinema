package com.opticine.controller;

import com.opticine.dto.promotion.PromotionValidateRequest;
import com.opticine.dto.promotion.PromotionValidateResponse;
import com.opticine.service.PromotionService;
import com.opticine.service.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /**
     * Customer validates a coupon code before creating a booking.
     * Returns discount amount without consuming the coupon.
     */
    @PostMapping("/validate")
    public ResponseEntity<PromotionValidateResponse> validate(@RequestBody PromotionValidateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            PromotionValidateResponse response = promotionService.validate(userDetails.getId(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(
                PromotionValidateResponse.builder()
                    .valid(false)
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    @GetMapping("/available")
    public ResponseEntity<?> available(@RequestParam(required = false) java.math.BigDecimal ticketAmount,
                                       @RequestParam(required = false) java.math.BigDecimal comboAmount) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(promotionService.available(userDetails.getId(), ticketAmount, comboAmount));
    }
}
