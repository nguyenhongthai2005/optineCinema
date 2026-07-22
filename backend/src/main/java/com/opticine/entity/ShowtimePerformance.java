package com.opticine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "showtime_performance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimePerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "business_date")
    private LocalDate businessDate;

    @Column(name = "tickets_sold")
    private Integer ticketsSold;

    @Column(name = "occupancy_rate", precision = 5, scale = 2)
    private BigDecimal occupancyRate;

    @Column(name = "ticket_revenue", precision = 15, scale = 2)
    private BigDecimal ticketRevenue;

    @Column(name = "combo_revenue", precision = 15, scale = 2)
    private BigDecimal comboRevenue;

    @Column(name = "total_revenue", precision = 15, scale = 2)
    private BigDecimal totalRevenue;
}
