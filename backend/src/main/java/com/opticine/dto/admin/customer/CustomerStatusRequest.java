package com.opticine.dto.admin.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerStatusRequest {
    @NotBlank
    private String status;
}
