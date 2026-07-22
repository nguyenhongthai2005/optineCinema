package com.opticine.dto.customer;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerProfileUpdateRequest {

    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ (VD: 0901234567)")
    private String phone;
}
