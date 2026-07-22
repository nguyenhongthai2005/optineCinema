package com.opticine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticine.dto.auth.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_invalidEmail_withoutToken_returnsValidationError() throws Exception {
        RegisterRequest request = invalidEmailRegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                .contextPath("/api")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Email không hợp lệ")));
    }

    @Test
    void register_invalidEmail_withInvalidBearerStillReturnsValidationError() throws Exception {
        RegisterRequest request = invalidEmailRegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                .contextPath("/api")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Email không hợp lệ")));
    }

    private RegisterRequest invalidEmailRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Invalid Email");
        request.setEmail("not-an-email");
        request.setPhone("0900000001");
        request.setPassword("Demo123456");
        return request;
    }
}
