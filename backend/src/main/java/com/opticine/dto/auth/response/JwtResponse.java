package com.opticine.dto.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String fullName;
    private String position;
    private String contractType;
    private List<String> roles;

    public JwtResponse(String token, Long id, String username, String email, String phone, String fullName, List<String> roles) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.fullName = fullName;
        this.roles = roles;
    }

    public JwtResponse(String token, Long id, String username, String email, String phone, String fullName, String position, String contractType, List<String> roles) {
        this(token, id, username, email, phone, fullName, roles);
        this.position = position;
        this.contractType = contractType;
    }
}

