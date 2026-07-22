package com.opticine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticine.entity.*;
import com.opticine.config.VNPayConfig;
import com.opticine.dto.booking.BookingRequest;
import com.opticine.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BookingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    private List<Long> availableSeatIds = new ArrayList<>();
    private Long showtimeId;
    private Long userId;

    @BeforeEach
    void setup() {
        availableSeatIds.clear();
        userId = userRepository.findByUsername("user").orElseThrow().getId();
        ShowtimeSeat firstSeat = showtimeSeatRepository.findAll().stream()
                .filter(seat -> seat.getShowtime() != null
                        && seat.getShowtime().getStartTime() != null
                        && seat.getShowtime().getStartTime().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No future showtime seats found"));
        showtimeId = firstSeat.getShowtime().getId();

        // Reset seats from one showtime for booking-flow tests.
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeId(showtimeId);
        for (ShowtimeSeat seat : seats) {
            seat.setStatus("LOCKED");
            seat.setLockedBy(userId);
            seat.setLockedAt(java.time.LocalDateTime.now());
            showtimeSeatRepository.save(seat);
            availableSeatIds.add(seat.getId());
        }
    }

    // 1. Tạo Booking thành công
    @Test
    @WithUserDetails("user")
    void test1_CreateBooking_Success() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setShowtimeSeatIds(availableSeatIds);

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    // 2. Lỗi do ghế đã có người đặt
    @Test
    @WithUserDetails("user")
    void test2_CreateBooking_SeatAlreadyLocked() throws Exception {
        // Lock seats first
        for (Long id : availableSeatIds) {
            ShowtimeSeat sts = showtimeSeatRepository.findById(id).get();
            sts.setStatus("LOCKED");
            sts.setLockedBy(99999L);
            sts.setLockedAt(java.time.LocalDateTime.now());
            showtimeSeatRepository.save(sts);
        }

        BookingRequest request = new BookingRequest();
        request.setShowtimeSeatIds(availableSeatIds);

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ghế đang được giữ bởi người khác: A1"));
    }

    // 3. Tạo Booking nhưng không truyền token (Lỗi 401)
    @Test
    void test3_CreateBooking_Unauthorized() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setShowtimeSeatIds(availableSeatIds);

        mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."));
    }

    // 4. Tạo Payment URL thành công
    @Test
    @WithUserDetails("user")
    void test4_CreatePaymentUrl_Success() throws Exception {
        // Create booking manually
        BookingRequest req = new BookingRequest();
        req.setShowtimeSeatIds(availableSeatIds);
        MvcResult res = mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        String content = res.getResponse().getContentAsString();
        Long bookingId = objectMapper.readTree(content).get("id").asLong();

        mockMvc.perform(get("/payment/vnpay/create-url")
                .param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentUrl").exists());
    }

    // 5. Tạo Payment URL với Booking rỗng
    @Test
    @WithUserDetails("user")
    void test5_CreatePaymentUrl_NotFound() throws Exception {
        mockMvc.perform(get("/payment/vnpay/create-url")
                .param("bookingId", "9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy đơn đặt vé."));
    }

    // 6. Callback VNPay Thành công -> Tạo vé, tạo hóa đơn, đổi trạng thái
    @Test
    @WithUserDetails("user")
    void test6_PaymentReturn_Success() throws Exception {
        // Tạo booking
        BookingRequest req = new BookingRequest();
        req.setShowtimeSeatIds(availableSeatIds);
        MvcResult res = mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        Long bookingId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        Booking createdBooking = bookingRepository.findById(bookingId).orElseThrow();
        String vnpAmount = createdBooking.getFinalAmount().toBigInteger().multiply(java.math.BigInteger.valueOf(100)).toString();

        // Giả lập callback thành công từ VNPay
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", vnpAmount);
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_BankTranNo", "123456");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan don hang");
        params.put("vnp_PayDate", "20260610120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "CGXZLS0Z");
        params.put("vnp_TransactionNo", "13579");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", bookingId + "_123456789");

        // Hash data
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
            if (itr.hasNext()) hashData.append('&');
        }
        String secureHash = VNPayConfig.hmacSHA512(hashSecret, hashData.toString());

        mockMvc.perform(get("/payment/vnpay/return")
                .param("vnp_Amount", vnpAmount)
                .param("vnp_BankCode", "NCB")
                .param("vnp_BankTranNo", "123456")
                .param("vnp_CardType", "ATM")
                .param("vnp_OrderInfo", "Thanh toan don hang")
                .param("vnp_PayDate", "20260610120000")
                .param("vnp_ResponseCode", "00")
                .param("vnp_TmnCode", "CGXZLS0Z")
                .param("vnp_TransactionNo", "13579")
                .param("vnp_TransactionStatus", "00")
                .param("vnp_TxnRef", bookingId + "_123456789")
                .param("vnp_SecureHash", secureHash))
                .andExpect(status().isFound());

        Booking b = bookingRepository.findById(bookingId).get();
        assertEquals("CONFIRMED", b.getStatus());
        assertTrue(ticketRepository.count() > 0);
        assertTrue(invoiceRepository.count() > 0);
    }

    // 7. Callback VNPay Thất bại -> Hủy Booking, nhả ghế
    @Test
    @WithUserDetails("user")
    void test7_PaymentReturn_Failed() throws Exception {
        BookingRequest req = new BookingRequest();
        req.setShowtimeSeatIds(availableSeatIds);
        MvcResult res = mockMvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        Long bookingId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        Booking createdBooking = bookingRepository.findById(bookingId).orElseThrow();
        String vnpAmount = createdBooking.getFinalAmount().toBigInteger().multiply(java.math.BigInteger.valueOf(100)).toString();

        // Giả lập callback thất bại từ VNPay (TransactionStatus = 02)
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", vnpAmount);
        params.put("vnp_TransactionStatus", "02"); // Giao dịch thất bại
        params.put("vnp_TxnRef", bookingId + "_123");
        
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
            if (itr.hasNext()) hashData.append('&');
        }
        String secureHash = VNPayConfig.hmacSHA512(hashSecret, hashData.toString());

        mockMvc.perform(get("/payment/vnpay/return")
                .param("vnp_Amount", vnpAmount)
                .param("vnp_TransactionStatus", "02")
                .param("vnp_TxnRef", bookingId + "_123")
                .param("vnp_SecureHash", secureHash))
                .andExpect(status().isFound());

        Booking b = bookingRepository.findById(bookingId).get();
        assertEquals("CANCELLED", b.getStatus());
        for (ShowtimeSeat seat : b.getSeats()) {
            assertEquals("AVAILABLE", seat.getStatus());
        }
    }

    // 8. Callback VNPay Sai Chữ Ký -> Lỗi 400
    @Test
    void test8_PaymentReturn_InvalidSignature() throws Exception {
        mockMvc.perform(get("/payment/vnpay/return")
                .param("vnp_Amount", "10000")
                .param("vnp_TxnRef", "1_123")
                .param("vnp_SecureHash", "wronghash123"))
                .andExpect(status().isBadRequest());
    }

    // 9. Lấy danh sách vé thành công
    @Test
    @WithUserDetails("user")
    void test9_GetTickets_Success() throws Exception {
        // Run test 6 to create tickets
        test6_PaymentReturn_Success();

        // Lấy booking ID vừa tạo
        List<Booking> bookings = bookingRepository.findAll();
        Long bookingId = bookings.get(bookings.size() - 1).getId();

        mockMvc.perform(get("/bookings/" + bookingId + "/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].qrCode").exists());
    }

    // 10. Lấy danh sách vé cho Booking không tồn tại
    @Test
    @WithUserDetails("user")
    void test10_GetTickets_NotFound() throws Exception {
        mockMvc.perform(get("/bookings/9999/tickets"))
                .andExpect(status().isBadRequest());
    }
}
