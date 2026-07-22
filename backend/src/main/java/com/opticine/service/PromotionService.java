package com.opticine.service;

import com.opticine.dto.promotion.PromotionDTO;
import com.opticine.dto.promotion.AvailablePromotionResponse;
import com.opticine.dto.promotion.PromotionValidateRequest;
import com.opticine.dto.promotion.PromotionValidateResponse;
import com.opticine.entity.Booking;
import com.opticine.entity.Customer;
import com.opticine.entity.Promotion;
import com.opticine.entity.PromotionUsage;
import com.opticine.repository.CustomerRepository;
import com.opticine.repository.PromotionRepository;
import com.opticine.repository.PromotionUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final CustomerRepository customerRepository;

    // ─────────────────────────────────────────────────────────
    // Public: validate coupon without consuming it
    // ─────────────────────────────────────────────────────────

    public PromotionValidateResponse validate(Long userId, PromotionValidateRequest request) {
        String code = request.getCode();
        if (code == null || code.isBlank()) {
            return invalid("Mã khuyến mãi không được để trống.");
        }

        Promotion promo = promotionRepository.findByCode(code.trim().toUpperCase())
                .orElse(null);
        if (promo == null) {
            return invalid("Mã khuyến mãi không tồn tại.");
        }
        if (!"ACTIVE".equals(promo.getStatus())) {
            return invalid("Mã khuyến mãi không còn hiệu lực.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartDate() != null && now.isBefore(promo.getStartDate())) {
            return invalid("Mã khuyến mãi chưa được kích hoạt.");
        }
        if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
            return invalid("Mã khuyến mãi đã hết hạn.");
        }

        if (promo.getMaxUsage() != null && promo.getCurrentUsage() != null
                && promo.getCurrentUsage() >= promo.getMaxUsage()) {
            return invalid("Mã khuyến mãi đã hết lượt sử dụng.");
        }

        // Per-user limit check
        if (promo.getMaxUsagePerUser() != null && promo.getMaxUsagePerUser() > 0) {
            Customer customer = customerRepository.findByUserId(userId).orElse(null);
            if (customer != null) {
                long usedCount = promotionUsageRepository.countByPromotionIdAndCustomerId(
                        promo.getId(), customer.getId());
                if (usedCount >= promo.getMaxUsagePerUser()) {
                    return invalid("Bạn đã sử dụng hết lượt cho mã này.");
                }
            }
        }

        // Compute applicable base amount
        BigDecimal ticketAmount = request.getTicketAmount() != null ? request.getTicketAmount() : BigDecimal.ZERO;
        BigDecimal comboAmount = request.getComboAmount() != null ? request.getComboAmount() : BigDecimal.ZERO;
        BigDecimal baseAmount = computeBase(promo.getApplicableTo(), ticketAmount, comboAmount);
        BigDecimal discountAmount = computeDiscount(promo, baseAmount);

        return PromotionValidateResponse.builder()
                .valid(true)
                .message("Áp dụng mã thành công!")
                .code(promo.getCode())
                .discountType(promo.getDiscountType())
                .discountValue(promo.getDiscountValue())
                .discountAmount(discountAmount)
                .maxDiscountAmount(promo.getMaxDiscountAmount())
                .applicableTo(promo.getApplicableTo())
                .build();
    }

    public List<AvailablePromotionResponse> available(Long userId, BigDecimal ticketAmount, BigDecimal comboAmount) {
        return promotionRepository.findAll().stream()
                .map(promo -> toAvailableResponse(userId, promo, ticketAmount, comboAmount))
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // Called after payment CONFIRMED: consume the coupon
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void applyPromotion(String code, Long userId, Booking booking) {
        if (code == null || code.isBlank()) return;

        Promotion promo = promotionRepository.findByCode(code.trim().toUpperCase()).orElse(null);
        if (promo == null) return;

        Customer customer = customerRepository.findByUserId(userId).orElse(null);
        if (customer == null) return;

        // Increase global usage counter
        int current = promo.getCurrentUsage() != null ? promo.getCurrentUsage() : 0;
        promo.setCurrentUsage(current + 1);
        promotionRepository.save(promo);

        // Record per-user usage
        PromotionUsage usage = PromotionUsage.builder()
                .promotion(promo)
                .customer(customer)
                .booking(booking)
                .usedAt(LocalDateTime.now())
                .build();
        promotionUsageRepository.save(usage);
    }

    // ─────────────────────────────────────────────────────────
    // Release (rollback) if booking cancelled before payment
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void releasePromotion(String code, Long bookingId) {
        if (code == null || code.isBlank()) return;
        Promotion promo = promotionRepository.findByCode(code.trim().toUpperCase()).orElse(null);
        if (promo == null) return;

        boolean used = promotionUsageRepository.existsByPromotionIdAndBookingId(promo.getId(), bookingId);
        if (used) {
            promotionUsageRepository.deleteByBookingId(bookingId);
            int current = promo.getCurrentUsage() != null ? promo.getCurrentUsage() : 1;
            promo.setCurrentUsage(Math.max(0, current - 1));
            promotionRepository.save(promo);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Compute discount amount
    // ─────────────────────────────────────────────────────────

    public BigDecimal computeDiscountAmount(String code, BigDecimal ticketAmount, BigDecimal comboAmount) {
        if (code == null || code.isBlank()) return BigDecimal.ZERO;
        Promotion promo = promotionRepository.findByCode(code.trim().toUpperCase()).orElse(null);
        if (promo == null) return BigDecimal.ZERO;
        BigDecimal base = computeBase(promo.getApplicableTo(), ticketAmount, comboAmount);
        return computeDiscount(promo, base);
    }

    // ─────────────────────────────────────────────────────────
    // Admin CRUD
    // ─────────────────────────────────────────────────────────

    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PromotionDTO getById(Long id) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found: " + id));
        return toDTO(promo);
    }

    @Transactional
    public PromotionDTO create(PromotionDTO dto) {
        if (promotionRepository.existsByCode(dto.getCode().trim().toUpperCase())) {
            throw new RuntimeException("Mã khuyến mãi đã tồn tại: " + dto.getCode());
        }
        Promotion promo = fromDTO(dto);
        promo.setCode(promo.getCode().trim().toUpperCase());
        promo.setCurrentUsage(0);
        if (promo.getStatus() == null) promo.setStatus("ACTIVE");
        return toDTO(promotionRepository.save(promo));
    }

    @Transactional
    public PromotionDTO update(Long id, PromotionDTO dto) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found: " + id));
        // Allow code change only if not conflict
        String newCode = dto.getCode().trim().toUpperCase();
        if (!newCode.equals(promo.getCode()) && promotionRepository.existsByCode(newCode)) {
            throw new RuntimeException("Mã khuyến mãi đã tồn tại: " + newCode);
        }
        promo.setCode(newCode);
        promo.setDiscountType(dto.getDiscountType());
        promo.setDiscountValue(dto.getDiscountValue());
        promo.setStartDate(dto.getStartDate());
        promo.setEndDate(dto.getEndDate());
        promo.setStatus(dto.getStatus());
        promo.setMaxUsage(dto.getMaxUsage());
        promo.setApplicableTo(dto.getApplicableTo());
        promo.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        promo.setMaxUsagePerUser(dto.getMaxUsagePerUser());
        return toDTO(promotionRepository.save(promo));
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found: " + id));
        promo.setStatus(status);
        promotionRepository.save(promo);
    }

    @Transactional
    public void delete(Long id) {
        promotionRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private BigDecimal computeBase(String applicableTo, BigDecimal ticket, BigDecimal combo) {
        if ("TICKET".equals(applicableTo)) return ticket;
        if ("COMBO".equals(applicableTo)) return combo;
        return ticket.add(combo); // ALL
    }

    private BigDecimal computeDiscount(Promotion promo, BigDecimal base) {
        BigDecimal discount;
        if ("PERCENT".equals(promo.getDiscountType())) {
            discount = base.multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
            if (promo.getMaxDiscountAmount() != null && promo.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(promo.getMaxDiscountAmount());
            }
        } else {
            // FIXED
            discount = promo.getDiscountValue() != null ? promo.getDiscountValue() : BigDecimal.ZERO;
        }
        return discount.min(base).max(BigDecimal.ZERO);
    }

    private PromotionDTO toDTO(Promotion p) {
        PromotionDTO dto = new PromotionDTO();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setDiscountType(p.getDiscountType());
        dto.setDiscountValue(p.getDiscountValue());
        dto.setStartDate(p.getStartDate());
        dto.setEndDate(p.getEndDate());
        dto.setStatus(p.getStatus());
        dto.setMaxUsage(p.getMaxUsage());
        dto.setCurrentUsage(p.getCurrentUsage());
        dto.setApplicableTo(p.getApplicableTo());
        dto.setMaxDiscountAmount(p.getMaxDiscountAmount());
        dto.setMaxUsagePerUser(p.getMaxUsagePerUser());
        return dto;
    }

    private AvailablePromotionResponse toAvailableResponse(Long userId, Promotion promo, BigDecimal ticketAmount, BigDecimal comboAmount) {
        PromotionValidateRequest request = new PromotionValidateRequest();
        request.setCode(promo.getCode());
        request.setTicketAmount(ticketAmount != null ? ticketAmount : BigDecimal.ZERO);
        request.setComboAmount(comboAmount != null ? comboAmount : BigDecimal.ZERO);
        PromotionValidateResponse validation = validate(userId, request);
        Integer remainingUses = promo.getMaxUsage() != null
                ? Math.max(0, promo.getMaxUsage() - (promo.getCurrentUsage() != null ? promo.getCurrentUsage() : 0))
                : null;
        return AvailablePromotionResponse.builder()
                .id(promo.getId())
                .code(promo.getCode())
                .name(promo.getCode())
                .description(discountDescription(promo))
                .type(promo.getDiscountType())
                .typeLabel(discountTypeLabel(promo.getDiscountType()))
                .discountValue(promo.getDiscountValue())
                .maxDiscountAmount(promo.getMaxDiscountAmount())
                .startDate(promo.getStartDate())
                .endDate(promo.getEndDate())
                .remainingUses(remainingUses)
                .isApplicable(validation.isValid())
                .unavailableReason(validation.isValid() ? null : validation.getMessage())
                .estimatedDiscountAmount(validation.getDiscountAmount() != null ? validation.getDiscountAmount() : BigDecimal.ZERO)
                .applicableTo(promo.getApplicableTo())
                .build();
    }

    private String discountDescription(Promotion promo) {
        if ("PERCENT".equalsIgnoreCase(promo.getDiscountType())) {
            String base = "Giảm " + promo.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
            if (promo.getMaxDiscountAmount() != null && promo.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                base += ", tối đa " + promo.getMaxDiscountAmount().stripTrailingZeros().toPlainString() + "đ";
            }
            return base;
        }
        return "Giảm " + (promo.getDiscountValue() != null ? promo.getDiscountValue().stripTrailingZeros().toPlainString() : "0") + "đ";
    }

    private String discountTypeLabel(String type) {
        if ("PERCENT".equalsIgnoreCase(type)) return "Giảm theo phần trăm";
        if ("FIXED".equalsIgnoreCase(type)) return "Giảm số tiền cố định";
        return type;
    }

    private Promotion fromDTO(PromotionDTO dto) {
        return Promotion.builder()
                .code(dto.getCode())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus())
                .maxUsage(dto.getMaxUsage())
                .currentUsage(dto.getCurrentUsage() != null ? dto.getCurrentUsage() : 0)
                .applicableTo(dto.getApplicableTo())
                .maxDiscountAmount(dto.getMaxDiscountAmount())
                .maxUsagePerUser(dto.getMaxUsagePerUser())
                .build();
    }

    private PromotionValidateResponse invalid(String message) {
        return PromotionValidateResponse.builder()
                .valid(false)
                .message(message)
                .discountAmount(BigDecimal.ZERO)
                .build();
    }
}
