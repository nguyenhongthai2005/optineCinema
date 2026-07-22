package com.opticine.controller;

import com.opticine.dto.ticket.TicketResponse;
import com.opticine.entity.Booking;
import com.opticine.entity.ShowtimeSeat;
import com.opticine.entity.Ticket;
import com.opticine.repository.BookingRepository;
import com.opticine.repository.TicketRepository;
import com.opticine.service.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/bookings")
public class TicketController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @GetMapping("/{bookingId}/tickets")
    public ResponseEntity<?> getTicketsByBooking(@PathVariable Long bookingId,
                                                 @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            if (!canAccessBookingTickets(booking, currentUser)) {
                return ResponseEntity.status(403).body("Bạn không có quyền truy cập vé này.");
            }

            List<TicketResponse> responses = new ArrayList<>();
            List<Ticket> tickets = ticketRepository.findByShowtimeSeatIn(booking.getSeats());
            
            for (Ticket ticket : tickets) {
                ShowtimeSeat sts = ticket.getShowtimeSeat();
                TicketResponse res = new TicketResponse();
                res.setId(ticket.getId());
                
                // Boc null an toan cho Movie (vi chua co phim tren main)
                if (sts.getShowtime() != null && sts.getShowtime().getMovie() != null) {
                    res.setMovieTitle(sts.getShowtime().getMovie().getTitle());
                } else {
                    res.setMovieTitle("Đang cập nhật");
                }
                
                if (sts.getShowtime() != null && sts.getShowtime().getRoom() != null) {
                    res.setRoomName(sts.getShowtime().getRoom().getName());
                } else {
                    res.setRoomName("Phòng chưa xác định");
                }

                // Lay ghe va thoi gian
                if (sts.getSeat() != null) {
                    res.setSeatLabel(sts.getSeat().getRowLabel() + sts.getSeat().getColumnNumber());
                }
                if (sts.getShowtime() != null) {
                    res.setStartTime(sts.getShowtime().getStartTime());
                    res.setEndTime(sts.getShowtime().getEndTime());
                }
                
                res.setPrice(ticket.getPrice());
                res.setQrCode(ticket.getQrCode());
                res.setStatus(ticket.getStatus());
                res.setTicketTotal(valueOrZero(booking.getTicketTotal()));
                res.setComboTotal(valueOrZero(booking.getComboTotal()));
                res.setGrossTotal(valueOrZero(booking.getGrossTotal()));
                res.setPromotionCode(booking.getPromotionCode());
                BigDecimal voucherDiscountAmount = booking.getVoucherDiscountAmount() != null
                        ? booking.getVoucherDiscountAmount()
                        : booking.getPromotionCode() != null ? valueOrZero(booking.getDiscountAmount()) : BigDecimal.ZERO;
                res.setVoucherDiscountAmount(voucherDiscountAmount);
                res.setMembershipTierName(booking.getMembershipTierName());
                res.setMembershipDiscountPercent(valueOrZero(booking.getMembershipDiscountPercent()));
                res.setMembershipDiscountAmount(valueOrZero(booking.getMembershipDiscountAmount()));
                res.setDiscountAmount(valueOrZero(booking.getDiscountAmount()));
                res.setFinalAmount(valueOrZero(booking.getFinalAmount()));
                res.setPaymentMethod(booking.getPaymentMethod());
                res.setPaymentStatus(booking.getPaymentStatus());
                responses.add(res);
            }
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    private boolean canAccessBookingTickets(Booking booking, UserDetailsImpl currentUser) {
        if (currentUser == null) return false;
        boolean canManage = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER"));
        if (canManage) return true;
        return booking.getCustomer() != null
                && booking.getCustomer().getUser() != null
                && booking.getCustomer().getUser().getId().equals(currentUser.getId());
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
