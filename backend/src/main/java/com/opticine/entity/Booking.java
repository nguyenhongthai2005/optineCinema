package com.opticine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Column(length = 50)
    private String status;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "ticket_email_sent")
    @Builder.Default
    private Boolean ticketEmailSent = false;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "ticket_total", precision = 15, scale = 2)
    private BigDecimal ticketTotal;

    @Column(name = "combo_total", precision = 15, scale = 2)
    private BigDecimal comboTotal;

    @Column(name = "gross_total", precision = 15, scale = 2)
    private BigDecimal grossTotal;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "voucher_discount_amount", precision = 15, scale = 2)
    private BigDecimal voucherDiscountAmount;

    @Column(name = "membership_tier_name", length = 50)
    private String membershipTierName;

    @Column(name = "membership_discount_percent", precision = 5, scale = 2)
    private BigDecimal membershipDiscountPercent;

    @Column(name = "membership_discount_amount", precision = 15, scale = 2)
    private BigDecimal membershipDiscountAmount;

    @Column(name = "final_amount", precision = 15, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "promotion_code", length = 50)
    private String promotionCode;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "booking_seats",
        joinColumns = @JoinColumn(name = "booking_id"),
        inverseJoinColumns = @JoinColumn(name = "showtime_seat_id")
    )
    @Builder.Default
    private java.util.Set<ShowtimeSeat> seats = new java.util.HashSet<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingCombo> comboItems = new ArrayList<>();
}
