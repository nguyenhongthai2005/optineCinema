package com.opticine.controller;

import com.opticine.config.jwt.JwtUtils;
import com.opticine.dto.auth.request.LoginRequest;
import com.opticine.dto.auth.request.RegisterRequest;
import com.opticine.dto.auth.response.JwtResponse;
import com.opticine.dto.auth.response.MessageResponse;
import com.opticine.entity.Customer;
import com.opticine.entity.Role;
import com.opticine.entity.User;
import com.opticine.repository.CustomerRepository;
import com.opticine.repository.RoleRepository;
import com.opticine.repository.UserRepository;
import com.opticine.service.PasswordResetService;
import com.opticine.service.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    CustomerRepository customerRepository;

    @Mock
    PasswordEncoder encoder;

    @Mock
    JwtUtils jwtUtils;

    @Mock
    PasswordResetService passwordResetService;

    @Mock
    Authentication authentication;

    @InjectMocks
    AuthController authController;

    @Test
    void login_success_returns_user_without_password() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("customer@opticine.test");
        request.setPassword("password123");
        UserDetailsImpl principal = new UserDetailsImpl(
                7L,
                "customer@opticine.test",
                "customer@opticine.test",
                "0900000000",
                "Khach hang Demo",
                null,
                null,
                true,
                "ACTIVE",
                "encoded-secret",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-demo-token");

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(JwtResponse.class, response.getBody());
        JwtResponse body = (JwtResponse) response.getBody();
        assertEquals("jwt-demo-token", body.getToken());
        assertEquals("customer@opticine.test", body.getEmail());
        assertTrue(body.getRoles().contains("ROLE_CUSTOMER"));
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("customer@opticine.test");
        request.setPassword("wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertEquals(401, response.getStatusCode().value());
        assertInstanceOf(MessageResponse.class, response.getBody());
        MessageResponse body = (MessageResponse) response.getBody();
        assertEquals("Email hoặc mật khẩu không đúng.", body.getMessage());
        assertFalse(response.getBody().toString().toLowerCase().contains("token"));
    }

    @Test
    void registerCustomer_success_encodesPassword() {
        RegisterRequest request = registerRequest();
        Role customerRole = Role.builder().id(1L).name("ROLE_CUSTOMER").build();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(encoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return user;
        });

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(MessageResponse.class, response.getBody());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        assertEquals("ACTIVE", userCaptor.getValue().getStatus());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(400, response.getStatusCode().value());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(customerRepository);
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Khach hang Demo");
        request.setEmail("customer@opticine.test");
        request.setPhone("0900000000");
        request.setPassword("password123");
        return request;
    }
}
