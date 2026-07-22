package com.opticine.service;

import com.opticine.dto.payment.VietQrPaymentInfoResponse;
import com.opticine.entity.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VietQrServiceTest {

    @Mock
    ComboService comboService;

    @InjectMocks
    VietQrService vietQrService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(vietQrService, "enabled", true);
        ReflectionTestUtils.setField(vietQrService, "autoConfirmEnabled", true);
        ReflectionTestUtils.setField(vietQrService, "demoAmountEnabled", true);
        ReflectionTestUtils.setField(vietQrService, "demoAmountRate", new BigDecimal("0.1"));
        ReflectionTestUtils.setField(vietQrService, "demoRoundTo", BigDecimal.valueOf(1000));
        ReflectionTestUtils.setField(vietQrService, "demoMinAmount", BigDecimal.valueOf(1000));
        ReflectionTestUtils.setField(vietQrService, "bankId", "MB");
        ReflectionTestUtils.setField(vietQrService, "accountNo", "0000000000");
        ReflectionTestUtils.setField(vietQrService, "accountName", "OPTICINE DEMO");
        ReflectionTestUtils.setField(vietQrService, "template", "compact2");
    }

    @Test
    void createVietQrPayment_demoAmount_enabled_setsOriginalAndPayableAmount() {
        Booking booking = Booking.builder()
                .id(123L)
                .finalAmount(BigDecimal.valueOf(155_000))
                .grossTotal(BigDecimal.valueOf(180_000))
                .discountAmount(BigDecimal.valueOf(25_000))
                .paymentReference("OPTI123")
                .build();

        VietQrPaymentInfoResponse response = vietQrService.buildPaymentInfo(booking);

        assertMoney(BigDecimal.valueOf(155_000), response.getOriginalAmount());
        assertMoney(BigDecimal.valueOf(16_000), response.getPayableAmount());
        assertTrue(response.isDemoMode());
        assertEquals("OPTI123", response.getTransferContent());
        assertNotNull(response.getQrImageUrl());
    }

    @Test
    void calculateVietQrPayableAmount_demoNeverBelowMinimum() {
        BigDecimal payable = vietQrService.calculateVietQrPayableAmount(BigDecimal.valueOf(5_000));

        assertMoney(BigDecimal.valueOf(1000), payable);
    }

    private void assertMoney(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }
}
