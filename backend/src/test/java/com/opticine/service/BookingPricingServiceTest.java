package com.opticine.service;

import com.opticine.entity.Booking;
import com.opticine.entity.Customer;
import com.opticine.entity.Membership;
import com.opticine.entity.ShowtimeSeat;
import com.opticine.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPricingServiceTest {

    @Mock
    ComboService comboService;

    @Mock
    VietQrService vietQrService;

    @InjectMocks
    BookingPricingService bookingPricingService;

    @Test
    void calculatePrice_withoutVoucherOrMembership_returnsGrossTotal() {
        Booking booking = bookingWithSeats(null);
        when(vietQrService.calculateSeatPrice(any(Booking.class), any(ShowtimeSeat.class)))
                .thenReturn(vnd(70_000), vnd(90_000));
        when(comboService.comboTotal(booking.getComboItems())).thenReturn(vnd(40_000));

        bookingPricingService.ensurePricingSnapshot(booking, true);

        assertMoney(vnd(160_000), booking.getTicketTotal());
        assertMoney(vnd(40_000), booking.getComboTotal());
        assertMoney(vnd(200_000), booking.getGrossTotal());
        assertMoney(vnd(200_000), booking.getFinalAmount());
    }

    @Test
    void calculatePrice_withVoucher_appliesDiscount() {
        Booking booking = bookingWithOneSeat(null);
        booking.setVoucherDiscountAmount(vnd(30_000));
        when(vietQrService.calculateSeatPrice(any(Booking.class), any(ShowtimeSeat.class))).thenReturn(vnd(100_000));
        when(comboService.comboTotal(booking.getComboItems())).thenReturn(BigDecimal.ZERO);

        bookingPricingService.ensurePricingSnapshot(booking, true);

        assertMoney(vnd(70_000), booking.getFinalAmount());
        assertMoney(vnd(30_000), booking.getDiscountAmount());
    }

    @Test
    void calculatePrice_withMembership_appliesMembershipDiscount() {
        Booking booking = bookingWithOneSeat(membership("Silver", 10));
        when(vietQrService.calculateSeatPrice(any(Booking.class), any(ShowtimeSeat.class))).thenReturn(vnd(100_000));
        when(comboService.comboTotal(booking.getComboItems())).thenReturn(BigDecimal.ZERO);

        bookingPricingService.ensurePricingSnapshot(booking, true);

        assertMoney(vnd(10_000), booking.getMembershipDiscountAmount());
        assertMoney(vnd(90_000), booking.getFinalAmount());
        assertEquals("Silver", booking.getMembershipTierName());
    }

    @Test
    void calculatePrice_withVoucherAndMembership_appliesVoucherFirstThenMembership() {
        Booking booking = bookingWithOneSeat(membership("Gold", 10));
        booking.setVoucherDiscountAmount(vnd(20_000));
        when(vietQrService.calculateSeatPrice(any(Booking.class), any(ShowtimeSeat.class))).thenReturn(vnd(100_000));
        when(comboService.comboTotal(booking.getComboItems())).thenReturn(BigDecimal.ZERO);

        bookingPricingService.ensurePricingSnapshot(booking, true);

        assertMoney(vnd(8_000), booking.getMembershipDiscountAmount());
        assertMoney(vnd(28_000), booking.getDiscountAmount());
        assertMoney(vnd(72_000), booking.getFinalAmount());
    }

    @Test
    void finalAmount_neverNegative() {
        Booking booking = bookingWithOneSeat(null);
        booking.setVoucherDiscountAmount(vnd(500_000));
        when(vietQrService.calculateSeatPrice(any(Booking.class), any(ShowtimeSeat.class))).thenReturn(vnd(100_000));
        when(comboService.comboTotal(booking.getComboItems())).thenReturn(BigDecimal.ZERO);

        bookingPricingService.ensurePricingSnapshot(booking, true);

        assertMoney(BigDecimal.ZERO, booking.getFinalAmount());
        assertMoney(vnd(100_000), booking.getDiscountAmount());
    }

    private Booking bookingWithSeats(Membership membership) {
        Customer customer = Customer.builder()
                .id(1L)
                .user(User.builder().id(2L).email("customer@opticine.test").build())
                .membership(membership)
                .build();
        ShowtimeSeat seatA = ShowtimeSeat.builder().id(10L).build();
        ShowtimeSeat seatB = ShowtimeSeat.builder().id(11L).build();
        return Booking.builder()
                .customer(customer)
                .seats(Set.of(seatA, seatB))
                .build();
    }

    private Booking bookingWithOneSeat(Membership membership) {
        Customer customer = Customer.builder()
                .id(1L)
                .user(User.builder().id(2L).email("customer@opticine.test").build())
                .membership(membership)
                .build();
        ShowtimeSeat seat = ShowtimeSeat.builder().id(10L).build();
        return Booking.builder()
                .customer(customer)
                .seats(Set.of(seat))
                .build();
    }

    private Membership membership(String name, int discountPercent) {
        return Membership.builder()
                .id(1L)
                .name(name)
                .minSpent(BigDecimal.ZERO)
                .discountPercent(BigDecimal.valueOf(discountPercent))
                .build();
    }

    private BigDecimal vnd(long value) {
        return BigDecimal.valueOf(value);
    }

    private void assertMoney(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }
}
