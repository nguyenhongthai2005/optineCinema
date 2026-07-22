package com.opticine.dto.admin.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StaffRequest {
    @NotBlank
    @Size(max = 100)
    private String fullName;

    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 50)
    private String role;

    @Size(min = 6, max = 100)
    private String password;

    @Size(max = 50)
    private String status;

    @NotBlank
    @Size(max = 50)
    private String position;

    @NotBlank
    @Size(max = 50)
    private String contractType;

    @Size(max = 20)
    private String gender;

    private LocalDate dateOfBirth;

    @Size(max = 255)
    private String address;

    @Size(max = 50)
    @Deprecated
    private String staffPosition;

    @Size(max = 50)
    @Deprecated
    private String employmentType;
}
