package com.opticine.dto.auth.response;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        String phone,
        String fullName,
        String position,
        String contractType,
        List<String> roles,
        String avatarUrl,
        String provider) {
}
