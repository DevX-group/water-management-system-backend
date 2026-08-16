package com.backend.water_management_system.controller;

import com.backend.water_management_system.config.SecurityConfig;
import com.backend.water_management_system.dto.BillsSummaryDTO;
import com.backend.water_management_system.entity.BillReport;
import com.backend.water_management_system.service.BillReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillReportController.class)
@Import(SecurityConfig.class)
class BillReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillReportService billService;

    @Test
    void getAllBills_returnsList() throws Exception {
        // Arrange
        BillReport bill = new BillReport("1", "C001", "John Doe", 500.0, LocalDate.now(), LocalDate.now(), "UNPAID");
        when(billService.getAllBills()).thenReturn(List.of(bill));

        // Act & Assert
        mockMvc.perform(get("/api/bills_report")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("1")))
                .andExpect(jsonPath("$[0].customerId", is("C001")))
                .andExpect(jsonPath("$[0].amount", is(500.0)));

        verify(billService, times(1)).getAllBills();
    }

    @Test
    void getByCustomer_returnsList() throws Exception {
        // Arrange
        String customerId = "C001";
        BillReport bill = new BillReport("1", customerId, "John Doe", 500.0, LocalDate.now(), LocalDate.now(), "UNPAID");
        when(billService.getBillsByCustomer(customerId)).thenReturn(List.of(bill));

        // Act & Assert
        mockMvc.perform(get("/api/bills_report/{customerId}", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerId", is(customerId)));

        verify(billService, times(1)).getBillsByCustomer(customerId);
    }

    @Test
    void getSummary_returnsSummaryDTO() throws Exception {
        // Arrange
        String customerId = "C001";
        BillsSummaryDTO summary = new BillsSummaryDTO(customerId, "John Doe", 500.0, LocalDate.of(2026, 5, 29), 1L);
        when(billService.getSummary(customerId)).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/api/bills_report/summary/{customerId}", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId", is(customerId)))
                .andExpect(jsonPath("$.customerName", is("John Doe")))
                .andExpect(jsonPath("$.totalAmount", is(500.0)))
                .andExpect(jsonPath("$.lastBillDate", is("2026-05-29")))
                .andExpect(jsonPath("$.unpaidCount", is(1)));

        verify(billService, times(1)).getSummary(customerId);
    }

    @Test
    void getOverdueBills_returnsList() throws Exception {
        // Arrange
        BillReport bill = new BillReport("1", "C001", "John Doe", 500.0, LocalDate.now().minusDays(5), LocalDate.now().minusDays(10), "UNPAID");
        when(billService.getOverdueBills()).thenReturn(List.of(bill));

        // Act & Assert
        mockMvc.perform(get("/api/bills_report/overdue")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("UNPAID")));

        verify(billService, times(1)).getOverdueBills();
    }
}
