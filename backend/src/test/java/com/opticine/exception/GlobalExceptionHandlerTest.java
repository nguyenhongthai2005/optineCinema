package com.opticine.exception;

import com.opticine.dto.auth.response.MessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiException_usesDeclaredStatusAndMessage() {
        ResponseEntity<MessageResponse> response =
                handler.handleApiException(new ResourceNotFoundException("Không tìm thấy phim."));

        assertResponse(response, HttpStatus.NOT_FOUND, "Không tìm thấy phim.");
    }

    @Test
    void illegalArgument_remainsBadRequestForBackwardCompatibility() {
        ResponseEntity<MessageResponse> response =
                handler.handleInvalidOperation(new IllegalArgumentException("Ngày không hợp lệ."));

        assertResponse(response, HttpStatus.BAD_REQUEST, "Ngày không hợp lệ.");
    }

    @Test
    void responseStatus_preservesStatusAndReason() {
        ResponseEntity<MessageResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền."));

        assertResponse(response, HttpStatus.FORBIDDEN, "Bạn không có quyền.");
    }

    @Test
    void unexpectedException_hidesInternalDetails() {
        ResponseEntity<MessageResponse> response =
                handler.handleUnexpected(new RuntimeException("database password leaked"));

        assertResponse(response, HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau.");
    }

    private void assertResponse(ResponseEntity<MessageResponse> response,
                                HttpStatus expectedStatus,
                                String expectedMessage) {
        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedMessage, response.getBody().getMessage());
    }
}
