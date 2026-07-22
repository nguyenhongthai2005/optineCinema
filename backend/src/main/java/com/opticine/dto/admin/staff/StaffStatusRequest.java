package com.opticine.dto.admin.staff;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffStatusRequest {
    @NotBlank
    private String status;
}
