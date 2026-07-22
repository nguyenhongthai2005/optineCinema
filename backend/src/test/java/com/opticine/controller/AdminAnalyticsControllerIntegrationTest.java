package com.opticine.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAnalyticsControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithUserDetails("admin")
    void adminCanReadOverview() throws Exception {
        mockMvc.perform(get("/admin/analytics/overview")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").isNumber())
                .andExpect(jsonPath("$.orderRevenue").isNumber())
                .andExpect(jsonPath("$.actualPaidRevenue").isNumber())
                .andExpect(jsonPath("$.demoDifference").isNumber())
                .andExpect(jsonPath("$.totalBookings").isNumber())
                .andExpect(jsonPath("$.timeline", hasSize(31)))
                .andExpect(jsonPath("$.timeline[0].actualPaidRevenue").isNumber())
                .andExpect(jsonPath("$.topMovies").isArray())
                .andExpect(jsonPath("$.paymentMethods").isArray());
    }

    @Test
    @WithUserDetails("admin")
    void adminCanExportUtf8Csv() throws Exception {
        mockMvc.perform(get("/admin/reports/bookings/export")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"opticine-bookings-2026-07-01_2026-07-31.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(result -> assertArrayEquals(
                        new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                        java.util.Arrays.copyOf(result.getResponse().getContentAsByteArray(), 3)));
    }

    @Test
    @WithUserDetails("admin")
    void revenueExportIncludesOrderRevenueAndActualPaid() throws Exception {
        mockMvc.perform(get("/admin/reports/revenue/export")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Order revenue")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Actual paid")));
    }

    @Test
    @WithUserDetails("user")
    void customerCannotReadAdminAnalytics() throws Exception {
        mockMvc.perform(get("/admin/analytics/overview"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này."));
    }

    @Test
    @WithUserDetails("admin")
    void invalidDateRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/analytics/overview")
                        .param("from", "2026-08-01")
                        .param("to", "2026-07-01"))
                .andExpect(status().isBadRequest());
    }
}
