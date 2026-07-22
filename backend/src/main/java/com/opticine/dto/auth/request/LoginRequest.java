package com.opticine.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Email or Phone cannot be blank")
    private String identifier; // can be email or phone

    @NotBlank(message = "Password cannot be blank")
    private String password;
}

