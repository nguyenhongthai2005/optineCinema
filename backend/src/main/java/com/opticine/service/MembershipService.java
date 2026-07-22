package com.opticine.service;

import com.opticine.entity.Customer;
import com.opticine.entity.Invoice;
import com.opticine.entity.Membership;
import com.opticine.repository.CustomerRepository;
import com.opticine.repository.InvoiceRepository;
import com.opticine.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Xử lý tích điểm và cập nhật hạng membership sau khi thanh toán thành công.
 *
 * Quy tắc:
 * - 1 điểm = 10.000₫ chi tiêu (làm tròn xuống)
 * - Hạng dựa trên totalSpent: Bronze → Silver (1tr) → Gold (5tr) → Platinum (15tr)
 * - Chỉ xử lý nếu invoice.membershipProcessed == false (tránh double-count)
 * - Booking hủy/thất bại không gọi service này
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

    private static final BigDecimal POINTS_PER_VND = BigDecimal.valueOf(10_000);

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final MembershipRepository membershipRepository;

    /**
     * Cộng điểm và cập nhật hạng membership cho khách hàng sau khi invoice được PAID.
     * Chạy trong transaction hiện tại để booking, invoice và membership luôn nhất quán.
     */
    @Transactional
    public void processInvoice(Invoice invoice) {
        // Bảo vệ: chỉ xử lý invoice PAID và chưa được cộng điểm
        if (invoice == null) return;
        if (Boolean.TRUE.equals(invoice.getMembershipProcessed())) {
            log.debug("Invoice {} already processed for membership", invoice.getId());
            return;
        }
        if (!"PAID".equals(invoice.getStatus())) {
            log.debug("Invoice {} not PAID, skipping membership", invoice.getId());
            return;
        }
        if (invoice.getCustomer() == null) {
            log.debug("Invoice {} has no customer, skipping membership", invoice.getId());
            return;
        }

        Customer customer = invoice.getCustomer();

        // 1. Tính điểm từ tổng tiền thanh toán
        BigDecimal amount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        int earnedPoints = amount.divide(POINTS_PER_VND, 0, RoundingMode.FLOOR).intValue();

        // 2. Cộng điểm và totalSpent vào customer
        int currentPoints = customer.getPoints() != null ? customer.getPoints() : 0;
        BigDecimal currentSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;

        customer.setPoints(currentPoints + earnedPoints);
        customer.setTotalSpent(currentSpent.add(amount));

        // 3. Xác định hạng membership phù hợp theo totalSpent mới
        BigDecimal newTotalSpent = customer.getTotalSpent();
        List<Membership> tiers = membershipRepository.findAllByOrderByMinSpentAsc();

        Membership newMembership = null;
        for (Membership tier : tiers) {
            if (newTotalSpent.compareTo(tier.getMinSpent()) >= 0) {
                newMembership = tier;
            }
        }
        if (newMembership != null) {
            Membership oldMembership = customer.getMembership();
            customer.setMembership(newMembership);
            if (oldMembership == null || !newMembership.getId().equals(oldMembership.getId())) {
                log.info("Customer {} upgraded to {} (spent={})",
                        customer.getId(), newMembership.getName(), newTotalSpent);
            }
        }

        // 4. Lưu customer
        customerRepository.save(customer);

        // 5. Đánh dấu invoice đã xử lý để tránh double-count
        invoice.setMembershipProcessed(true);
        invoiceRepository.save(invoice);

        log.info("Membership processed: invoiceId={}, customerId={}, +{}pts, totalSpent={}",
                invoice.getId(), customer.getId(), earnedPoints, newTotalSpent);
    }

    @Transactional
    public void setCustomerSpending(Customer customer, BigDecimal totalSpent) {
        if (customer == null) return;
        BigDecimal safeTotal = totalSpent != null ? totalSpent : BigDecimal.ZERO;
        customer.setTotalSpent(safeTotal);
        customer.setPoints(safeTotal.divide(POINTS_PER_VND, 0, RoundingMode.FLOOR).intValue());

        Membership newMembership = null;
        for (Membership tier : membershipRepository.findAllByOrderByMinSpentAsc()) {
            if (safeTotal.compareTo(tier.getMinSpent()) >= 0) {
                newMembership = tier;
            }
        }
        customer.setMembership(newMembership);
        customerRepository.save(customer);
    }
}
