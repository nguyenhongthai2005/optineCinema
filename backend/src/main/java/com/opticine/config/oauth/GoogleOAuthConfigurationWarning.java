package com.opticine.config.oauth;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthConfigurationWarning {
    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthConfigurationWarning.class);

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String clientId;

    @PostConstruct
    void warnWhenMissing() {
        if (clientId.isBlank() || clientId.contains("not-configured")) {
            log.warn("Google OAuth chưa được cấu hình. Hãy đặt GOOGLE_CLIENT_ID và GOOGLE_CLIENT_SECRET trước khi đăng nhập Google.");
        }
    }
}
