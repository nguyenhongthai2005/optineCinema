package com.opticine.service;

import com.opticine.dto.booking.ComboItemRequest;
import com.opticine.dto.booking.ComboItemResponse;
import com.opticine.dto.combo.ComboResponse;
import com.opticine.entity.Booking;
import com.opticine.entity.BookingCombo;
import com.opticine.entity.Combo;
import com.opticine.repository.BookingComboRepository;
import com.opticine.repository.ComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComboService {
    private final ComboRepository comboRepository;
    private final BookingComboRepository bookingComboRepository;

    @Transactional(readOnly = true)
    public List<ComboResponse> activeCombos() {
        return comboRepository.findByStatusIgnoreCase("ACTIVE").stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingCombo> buildBookingCombos(Booking booking, List<ComboItemRequest> requests) {
        List<ComboItemRequest> normalized = normalizeRequests(requests);
        if (normalized.isEmpty()) return List.of();

        Map<Long, Combo> combos = comboRepository.findAllById(normalized.stream().map(ComboItemRequest::getComboId).toList())
                .stream()
                .collect(Collectors.toMap(Combo::getId, combo -> combo));

        List<BookingCombo> items = new ArrayList<>();
        for (ComboItemRequest request : normalized) {
            Combo combo = combos.get(request.getComboId());
            if (combo == null) {
                throw new IllegalArgumentException("Không tìm thấy combo.");
            }
            if (!"ACTIVE".equalsIgnoreCase(combo.getStatus())) {
                throw new IllegalArgumentException("Combo không còn bán: " + combo.getName());
            }
            int quantity = request.getQuantity();
            BigDecimal unitPrice = combo.getPrice() != null ? combo.getPrice() : BigDecimal.ZERO;
            items.add(BookingCombo.builder()
                    .booking(booking)
                    .combo(combo)
                    .comboNameSnapshot(combo.getName())
                    .unitPriceSnapshot(unitPrice)
                    .quantity(quantity)
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                    .build());
        }
        return items;
    }

    public List<ComboItemRequest> normalizeRequests(List<ComboItemRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();
        return requests.stream()
                .filter(item -> item != null && item.getComboId() != null && item.getQuantity() != null && item.getQuantity() > 0)
                .collect(Collectors.groupingBy(ComboItemRequest::getComboId, Collectors.summingInt(ComboItemRequest::getQuantity)))
                .entrySet()
                .stream()
                .map(entry -> {
                    ComboItemRequest item = new ComboItemRequest();
                    item.setComboId(entry.getKey());
                    item.setQuantity(entry.getValue());
                    return item;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComboItemResponse> bookingComboResponses(Long bookingId) {
        return bookingComboRepository.findByBookingId(bookingId).stream()
                .map(this::toBookingResponse)
                .toList();
    }

    public List<ComboItemResponse> bookingComboResponses(List<BookingCombo> items) {
        if (items == null) return List.of();
        return items.stream().map(this::toBookingResponse).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal comboTotal(Long bookingId) {
        return bookingComboRepository.findByBookingId(bookingId).stream()
                .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal comboTotal(List<BookingCombo> items) {
        if (items == null) return BigDecimal.ZERO;
        return items.stream()
                .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ComboItemResponse toBookingResponse(BookingCombo item) {
        return ComboItemResponse.builder()
                .comboId(item.getCombo() != null ? item.getCombo().getId() : null)
                .comboName(item.getComboNameSnapshot())
                .unitPrice(item.getUnitPriceSnapshot())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    private ComboResponse toPublicResponse(Combo combo) {
        return ComboResponse.builder()
                .id(combo.getId())
                .name(combo.getName())
                .description(blankToEmpty(combo.getDescription()))
                .imageUrl(combo.getImageUrl())
                .category(combo.getCategory())
                .price(combo.getPrice())
                .status(combo.getStatus())
                .stockQuantity(combo.getStockQuantity())
                .build();
    }

    private String blankToEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
