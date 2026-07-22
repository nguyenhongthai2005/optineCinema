package com.opticine.service;

import com.opticine.dto.admin.timeslot.TimeSlotRequest;
import com.opticine.dto.admin.timeslot.TimeSlotResponse;
import com.opticine.entity.TimeSlot;
import com.opticine.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminTimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    /** Lấy tất cả giờ chiếu. */
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAll() {
        return timeSlotRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /** Lấy chi tiết giờ chiếu. */
    @Transactional(readOnly = true)
    public TimeSlotResponse getById(Long id) {
        return toResponse(findSlot(id));
    }

    /** Tạo giờ chiếu mới. */
    @Transactional
    public TimeSlotResponse create(TimeSlotRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime())
                || request.getEndTime().equals(request.getStartTime())) {
            throw new IllegalArgumentException("endTime phải sau startTime");
        }

        TimeSlot slot = TimeSlot.builder()
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .priceMultiplier(request.getPriceMultiplier() != null
                        ? request.getPriceMultiplier() : BigDecimal.ONE)
                .status(normalizeStatus(request.getStatus()))
                .build();

        return toResponse(timeSlotRepository.save(slot));
    }

    /** Cập nhật giờ chiếu. */
    @Transactional
    public TimeSlotResponse update(Long id, TimeSlotRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime())
                || request.getEndTime().equals(request.getStartTime())) {
            throw new IllegalArgumentException("endTime phải sau startTime");
        }

        TimeSlot slot = findSlot(id);
        slot.setName(request.getName());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        if (request.getPriceMultiplier() != null) {
            slot.setPriceMultiplier(request.getPriceMultiplier());
        }
        if (StringUtils.hasText(request.getStatus())) {
            slot.setStatus(normalizeStatus(request.getStatus()));
        }

        return toResponse(timeSlotRepository.save(slot));
    }

    /** Cập nhật trạng thái giờ chiếu (ACTIVE / INACTIVE). */
    @Transactional
    public TimeSlotResponse updateStatus(Long id, String status) {
        TimeSlot slot = findSlot(id);
        slot.setStatus(normalizeStatus(status));
        return toResponse(timeSlotRepository.save(slot));
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private TimeSlot findSlot(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giờ chiếu id=" + id));
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "ACTIVE";
    }

    private TimeSlotResponse toResponse(TimeSlot slot) {
        return TimeSlotResponse.builder()
                .id(slot.getId())
                .name(slot.getName())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .priceMultiplier(slot.getPriceMultiplier())
                .status(slot.getStatus())
                .build();
    }
}
