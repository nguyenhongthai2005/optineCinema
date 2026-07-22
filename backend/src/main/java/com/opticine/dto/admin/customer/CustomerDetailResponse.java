package com.opticine.dto.admin.customer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerDetailResponse {
    private CustomerResponse profile;
    private String membershipName;
}
