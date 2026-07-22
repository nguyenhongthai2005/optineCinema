package com.opticine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schedule_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(length = 50)
    private String mode;

    @Column(length = 50)
    private String status;

    @Column(name = "estimated_revenue", precision = 15, scale = 2)
    private BigDecimal estimatedRevenue;

    @Column(name = "estimated_tickets")
    private Integer estimatedTickets;

    @Column(name = "room_utilization", precision = 5, scale = 2)
    private BigDecimal roomUtilization;

    @Column(name = "opening_time", length = 10)
    private String openingTime;

    @Column(name = "closing_time", length = 10)
    private String closingTime;

    @Column(name = "cleaning_minutes")
    private Integer cleaningMinutes;

    @Column(name = "overwrite_existing")
    private Boolean overwriteExisting;

    @Column(name = "window_minutes")
    private Integer windowMinutes;

    @ManyToOne
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchedulePlanItem> items = new ArrayList<>();
}
