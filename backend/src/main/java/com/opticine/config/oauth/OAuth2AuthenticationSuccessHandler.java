package com.opticine.config.oauth;

import com.opticine.config.jwt.JwtUtils;
import com.opticine.entity.Customer;
import com.opticine.entity.Role;
import com.opticine.entity.User;
import com.opticine.repository.CustomerRepository;
import com.opticine.repository.RoleRepository;
import com.opticine.repository.UserRepository;
import com.opticine.service.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository,
                                               RoleRepository roleRepository,
                                               CustomerRepository customerRepository,
                                               PasswordEncoder passwordEncoder,
                                               JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    @Transactional
    public synchronized void onAuthenticationSuccess(HttpServletRequest request,
                                                     HttpServletResponse response,
                                                     Authentication authentication) throws IOException {
        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();
        String email = normalized(googleUser.getAttribute("email"));
        if (email == null) {
            response.sendRedirect(errorUrl("google_email_missing"));
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> createGoogleUser(
                email,
                googleUser.getAttribute("name"),
                googleUser.getAttribute("sub"),
                googleUser.getAttribute("picture")));

        if (!isActive(user)) {
            response.sendRedirect(errorUrl("account_inactive"));
            return;
        }

        user.setProvider("GOOGLE");
        user.setProviderId(googleUser.getAttribute("sub"));
        user.setAvatarUrl(googleUser.getAttribute("picture"));
        user.setEmailVerified(Boolean.TRUE);
        if ((user.getFullName() == null || user.getFullName().isBlank()) && googleUser.getAttribute("name") != null) {
            user.setFullName(googleUser.getAttribute("name"));
        }
        user = userRepository.save(user);

        String jwt = jwtUtils.generateJwtToken(UserDetailsImpl.build(user));
        String target = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/oauth2/success")
                .queryParam("token", jwt)
                .build().encode().toUriString();
        response.sendRedirect(target);
    }

    private User createGoogleUser(String email, String name, String subject, String picture) {
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_CUSTOMER");
            return roleRepository.save(role);
        });
        byte[] randomPassword = new byte[32];
        secureRandom.nextBytes(randomPassword);

        User user = new User();
        user.setEmail(email);
        user.setUsername(availableUsername(email));
        user.setFullName(name == null || name.isBlank() ? email : name);
        user.setPassword(passwordEncoder.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(randomPassword)));
        user.setStatus("ACTIVE");
        user.setEnabled(true);
        user.setProvider("GOOGLE");
        user.setProviderId(subject);
        user.setAvatarUrl(picture);
        user.setEmailVerified(Boolean.TRUE);
        user.setRoles(new HashSet<>());
        user.getRoles().add(customerRole);
        User saved = userRepository.save(user);

        Customer customer = new Customer();
        customer.setUser(saved);
        customer.setFullName(saved.getFullName());
        customer.setEmail(saved.getEmail());
        customer.setTotalSpent(BigDecimal.ZERO);
        customer.setPoints(0);
        customerRepository.save(customer);
        return saved;
    }

    private boolean isActive(User user) {
        return (user.getEnabled() == null || user.getEnabled())
                && (user.getStatus() == null || "ACTIVE".equalsIgnoreCase(user.getStatus()));
    }

    private String availableUsername(String email) {
        String base = email.length() <= 50 ? email : email.substring(0, 50);
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        do {
            byte[] suffixBytes = new byte[6];
            secureRandom.nextBytes(suffixBytes);
            String suffix = Base64.getUrlEncoder().withoutPadding().encodeToString(suffixBytes);
            base = email.substring(0, Math.min(email.length(), 41)) + "_" + suffix;
        } while (userRepository.existsByUsername(base));
        return base;
    }

    private String errorUrl(String error) {
        return UriComponentsBuilder.fromUriString(frontendUrl).path("/login")
                .queryParam("oauthError", error).build().encode().toUriString();
    }

    private String normalized(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
