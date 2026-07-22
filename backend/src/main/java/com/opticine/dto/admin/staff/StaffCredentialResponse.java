package com.opticine.dto.admin.staff;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffCredentialResponse {
    private Long id;
    private String username;
    private String temporaryPassword;
    private String fullName;
    private String email;
    private String phone;
    private String status;
    private String position;
    private String positionLabel;
    private String contractType;
    private String contractTypeLabel;
    private String message;
}
