package com.opticine.service;

import com.opticine.entity.Customer;
import com.opticine.entity.Invoice;
import com.opticine.entity.Membership;
import com.opticine.repository.CustomerRepository;
import com.opticine.repository.InvoiceRepository;
import com.opticine.repository.MembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    CustomerRepository customerRepository;

    @Mock
    InvoiceRepository invoiceRepository;

    @Mock
    MembershipRepository membershipRepository;

    @InjectMocks
    MembershipService membershipService;

    @Test
    void paidBooking_addsTotalSpentAndPoints() {
        Customer customer = Customer.builder().id(1L).totalSpent(vnd(100_000)).points(10).build();
        Invoice invoice = paidInvoice(customer, vnd(250_000), false);
        when(membershipRepository.findAllByOrderByMinSpentAsc()).thenReturn(tiers());

        membershipService.processInvoice(invoice);

        assertMoney(vnd(350_000), customer.getTotalSpent());
        assertEquals(35, customer.getPoints());
        assertTrue(invoice.getMembershipProcessed());
        verify(customerRepository).save(customer);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void upgradeTier_whenTotalSpentReachesThreshold() {
        Customer customer = Customer.builder().id(1L).totalSpent(vnd(900_000)).points(90).build();
        Invoice invoice = paidInvoice(customer, vnd(150_000), false);
        when(membershipRepository.findAllByOrderByMinSpentAsc()).thenReturn(tiers());

        membershipService.processInvoice(invoice);

        assertEquals("Silver", customer.getMembership().getName());
        assertMoney(vnd(1_050_000), customer.getTotalSpent());
    }

    @Test
    void repeatedPaymentConfirmation_doesNotDoubleAddPoints() {
        Customer customer = Customer.builder().id(1L).totalSpent(vnd(100_000)).points(10).build();
        Invoice invoice = paidInvoice(customer, vnd(250_000), true);

        membershipService.processInvoice(invoice);

        assertMoney(vnd(100_000), customer.getTotalSpent());
        assertEquals(10, customer.getPoints());
        verifyNoInteractions(customerRepository, invoiceRepository, membershipRepository);
    }

    @Test
    void guestCustomer_doesNotCrashMembershipUpdate() {
        Invoice invoice = paidInvoice(null, vnd(250_000), false);

        assertDoesNotThrow(() -> membershipService.processInvoice(invoice));
        verifyNoInteractions(customerRepository, invoiceRepository, membershipRepository);
    }

    private Invoice paidInvoice(Customer customer, BigDecimal amount, boolean processed) {
        return Invoice.builder()
                .id(10L)
                .customer(customer)
                .totalAmount(amount)
                .status("PAID")
                .membershipProcessed(processed)
                .build();
    }

    private List<Membership> tiers() {
        return List.of(
                Membership.builder().id(1L).name("Bronze").minSpent(BigDecimal.ZERO).discountPercent(BigDecimal.ZERO).build(),
                Membership.builder().id(2L).name("Silver").minSpent(vnd(1_000_000)).discountPercent(BigDecimal.valueOf(5)).build(),
                Membership.builder().id(3L).name("Gold").minSpent(vnd(5_000_000)).discountPercent(BigDecimal.valueOf(10)).build()
        );
    }

    private BigDecimal vnd(long value) {
        return BigDecimal.valueOf(value);
    }

    private void assertMoney(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }
}
