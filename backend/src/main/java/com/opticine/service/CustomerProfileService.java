package com.opticine.service;

import com.opticine.dto.customer.CustomerProfileResponse;
import com.opticine.dto.customer.CustomerProfileUpdateRequest;
import com.opticine.entity.Customer;
import com.opticine.entity.Membership;
import com.opticine.entity.User;
import com.opticine.repository.CustomerRepository;
import com.opticine.repository.MembershipRepository;
import com.opticine.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CustomerProfileService {

    private final CustomerRepository customerRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public CustomerProfileService(CustomerRepository customerRepository,
                                  MembershipRepository membershipRepository,
                                  UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public CustomerProfileResponse getProfile(Long userId) {
        Customer customer = findCustomerByUserId(userId);
        return buildResponse(customer);
    }

    @Transactional
    public CustomerProfileResponse updateProfile(Long userId, CustomerProfileUpdateRequest request) {
        Customer customer = findCustomerByUserId(userId);
        User user = customer.getUser();

        if (StringUtils.hasText(request.getFullName())) {
            customer.setFullName(request.getFullName().trim());
            if (user != null) user.setFullName(request.getFullName().trim());
        }
        if (StringUtils.hasText(request.getPhone())) {
            // Kiểm tra phone đã tồn tại chưa (không kể user hiện tại)
            userRepository.findByPhone(request.getPhone().trim())
                    .filter(u -> !u.getId().equals(userId))
                    .ifPresent(u -> { throw new IllegalArgumentException("Số điện thoại đã được sử dụng."); });
            customer.setPhone(request.getPhone().trim());
            if (user != null) user.setPhone(request.getPhone().trim());
        }

        customerRepository.save(customer);
        if (user != null) userRepository.save(user);

        return buildResponse(customer);
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private Customer findCustomerByUserId(Long userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ khách hàng."));
    }

    private CustomerProfileResponse buildResponse(Customer customer) {
        BigDecimal totalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO;
        List<Membership> tiers = membershipRepository.findAllByOrderByMinSpentAsc();

        Membership current = null;
        Membership next = null;

        // Tìm tier hiện tại và tier kế tiếp
        for (int i = 0; i < tiers.size(); i++) {
            Membership tier = tiers.get(i);
            if (totalSpent.compareTo(tier.getMinSpent()) >= 0) {
                current = tier;
                next = (i + 1 < tiers.size()) ? tiers.get(i + 1) : null;
            }
        }

        // Nếu chưa đạt tier nào (trường hợp bảng membership rỗng)
        if (current == null && !tiers.isEmpty()) {
            current = tiers.get(0);
            next = tiers.size() > 1 ? tiers.get(1) : null;
        }

        // Tính tiến độ lên hạng kế tiếp
        int progress = 100; // Platinum — đã max
        if (next != null && current != null) {
            BigDecimal range = next.getMinSpent().subtract(current.getMinSpent());
            BigDecimal done = totalSpent.subtract(current.getMinSpent());
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                progress = done.multiply(BigDecimal.valueOf(100))
                        .divide(range, 0, RoundingMode.HALF_UP)
                        .intValue();
                progress = Math.min(100, Math.max(0, progress));
            }
        }

        return CustomerProfileResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .points(customer.getPoints() != null ? customer.getPoints() : 0)
                .totalSpent(totalSpent)
                .membershipName(current != null ? current.getName() : "Bronze")
                .membershipDiscount(current != null ? current.getDiscountPercent() : BigDecimal.ZERO)
                .nextMembershipName(next != null ? next.getName() : null)
                .nextMembershipMinSpent(next != null ? next.getMinSpent() : null)
                .progressPercent(progress)
                .build();
    }
}
