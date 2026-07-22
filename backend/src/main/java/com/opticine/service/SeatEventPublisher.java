package com.opticine.service;

import com.opticine.dto.showtime.response.SeatEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Tách logic broadcast WebSocket ra khỏi business service.
 * Tham khảo: AstraCine SeatEventPublisher.
 */
@Service
@RequiredArgsConstructor
public class SeatEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Long showtimeId, SeatEventDto event) {
        String topic = "/topic/showtimes/" + showtimeId + "/seats";
        messagingTemplate.convertAndSend(topic, event);
    }
}
