package com.opticine.controller;

import com.opticine.dto.customer.CustomerProfileResponse;
import com.opticine.dto.customer.CustomerProfileUpdateRequest;
import com.opticine.service.CustomerProfileService;
import com.opticine.service.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerProfileService customerProfileService;

    public CustomerController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(customerProfileService.getProfile(principal.getId()));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ResponseEntity.ok(customerProfileService.updateProfile(principal.getId(), request));
    }
}
