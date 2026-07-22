package com.opticine.dto.booking;

import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    private List<Long> showtimeSeatIds;
    private List<ComboItemRequest> combos;
    private String promotionCode; // Optional coupon code
}

