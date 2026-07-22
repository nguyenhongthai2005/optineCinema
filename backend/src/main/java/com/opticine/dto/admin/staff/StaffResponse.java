package com.opticine.dto.admin.staff;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class StaffResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String address;
    private String position;
    private String positionLabel;
    private String contractType;
    private String contractTypeLabel;
    private String role;
    private String status;
    private Boolean enabled;
    @Deprecated
    private String staffPosition;
    @Deprecated
    private String employmentType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
