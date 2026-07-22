package com.opticine.controller;

import com.opticine.config.jwt.JwtUtils;
import com.opticine.dto.auth.request.LoginRequest;
import com.opticine.dto.auth.request.RegisterRequest;
import com.opticine.dto.auth.response.JwtResponse;
import com.opticine.dto.auth.response.MessageResponse;
import com.opticine.dto.auth.response.CurrentUserResponse;
import com.opticine.entity.Role;
import com.opticine.entity.User;
import com.opticine.repository.RoleRepository;
import com.opticine.repository.UserRepository;
import com.opticine.service.PasswordResetService;
import com.opticine.service.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.opticine.entity.Customer;
import com.opticine.repository.CustomerRepository;
import java.math.BigDecimal;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getIdentifier(), loginRequest.getPassword()));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401)
                    .body(new MessageResponse("Email hoặc mật khẩu không đúng."));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getPhone(),
                userDetails.getFullName(),
                userDetails.getStaffPosition(),
                userDetails.getEmploymentType(),
                roles));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        if (userRepository.existsByPhone(signUpRequest.getPhone())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Phone number is already in use!"));
        }

        // Create new user's account
        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setPhone(signUpRequest.getPhone());
        user.setFullName(signUpRequest.getFullName());
        // For username, we can use email for now
        user.setUsername(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setStatus("ACTIVE");
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();

        // By default, assigning ROLE_CUSTOMER
        Role userRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> {
                    // if role doesn't exist, create it. Normally should be handled by Flyway
                    Role role = new Role();
                    role.setName("ROLE_CUSTOMER");
                    return roleRepository.save(role);
                });
        roles.add(userRole);

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // CREATE CUSTOMER RECORD
        Customer customer = new Customer();
        customer.setUser(savedUser);
        customer.setFullName(savedUser.getFullName());
        customer.setEmail(savedUser.getEmail());
        customer.setPhone(savedUser.getPhone());
        customer.setTotalSpent(BigDecimal.ZERO);
        customer.setPoints(0);
        customerRepository.save(customer);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(@AuthenticationPrincipal UserDetailsImpl principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        List<String> roles = principal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
        return ResponseEntity.ok(new CurrentUserResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getPhone(), user.getFullName(),
                user.getStaffPosition(), user.getEmploymentType(), roles, user.getAvatarUrl(), user.getProvider()));
    }

    /**
     * Bước 1: Người dùng nhập email — gửi link đặt lại mật khẩu
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email không được để trống."));
        }
        // Không tiết lộ email có tồn tại hay không — luôn trả 200
        passwordResetService.processForgotPassword(email.trim());
        return ResponseEntity.ok(new MessageResponse(
                "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu."));
    }

    /**
     * Bước 2: Người dùng đặt mật khẩu mới qua token trong link email
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Token không được để trống."));
        }
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(new MessageResponse("Mật khẩu phải có ít nhất 6 ký tự."));
        }

        try {
            passwordResetService.resetPassword(token.trim(), newPassword);
            return ResponseEntity.ok(new MessageResponse("Mật khẩu đã được đặt lại thành công!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}

