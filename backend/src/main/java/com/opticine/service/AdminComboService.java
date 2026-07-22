package com.opticine.service;

import com.opticine.dto.admin.combo.AdminComboRequest;
import com.opticine.dto.admin.combo.AdminComboResponse;
import com.opticine.entity.Combo;
import com.opticine.repository.ComboRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminComboService {
    private static final List<String> STATUSES = List.of("ACTIVE", "INACTIVE");

    private final ComboRepository comboRepository;

    @Transactional(readOnly = true)
    public List<AdminComboResponse> findCombos(String keyword, String status, String category) {
        Specification<Combo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), normalizeStatus(status)));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("category")), "%" + category.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return comboRepository.findAll(spec, Sort.by(Sort.Order.asc("category"), Sort.Order.asc("name"))).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminComboResponse getCombo(Long id) {
        return toResponse(findCombo(id));
    }

    @Transactional
    public AdminComboResponse create(AdminComboRequest request) {
        Combo combo = new Combo();
        apply(combo, request);
        return toResponse(comboRepository.save(combo));
    }

    @Transactional
    public AdminComboResponse update(Long id, AdminComboRequest request) {
        Combo combo = findCombo(id);
        apply(combo, request);
        return toResponse(comboRepository.save(combo));
    }

    @Transactional
    public AdminComboResponse updateStatus(Long id, String status) {
        Combo combo = findCombo(id);
        combo.setStatus(normalizeStatus(status));
        return toResponse(comboRepository.save(combo));
    }

    @Transactional
    public void delete(Long id) {
        Combo combo = findCombo(id);
        combo.setStatus("INACTIVE");
        comboRepository.save(combo);
    }

    private Combo findCombo(Long id) {
        return comboRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy combo."));
    }

    private void apply(Combo combo, AdminComboRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên combo là bắt buộc.");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá combo không hợp lệ.");
        }
        combo.setName(request.getName().trim());
        combo.setDescription(blankToNull(request.getDescription()));
        combo.setImageUrl(blankToNull(request.getImageUrl()));
        combo.setCategory(blankToNull(request.getCategory()));
        combo.setPrice(request.getPrice());
        combo.setStatus(normalizeStatus(request.getStatus()));
        combo.setStockQuantity(request.getStockQuantity() == null ? 0 : Math.max(0, request.getStockQuantity()));
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái combo không hợp lệ.");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AdminComboResponse toResponse(Combo combo) {
        return AdminComboResponse.builder()
                .id(combo.getId())
                .name(combo.getName())
                .description(combo.getDescription())
                .imageUrl(combo.getImageUrl())
                .category(combo.getCategory())
                .price(combo.getPrice())
                .status(combo.getStatus())
                .stockQuantity(combo.getStockQuantity())
                .createdAt(combo.getCreatedAt())
                .updatedAt(combo.getUpdatedAt())
                .build();
    }
}
