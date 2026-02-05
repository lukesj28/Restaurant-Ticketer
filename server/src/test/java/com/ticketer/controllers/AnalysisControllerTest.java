package com.ticketer.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketer.models.AnalysisReport;
import com.ticketer.services.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(AnalysisController.class)
public class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalysisService analysisService;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void testGetAnalysis() throws Exception {
        AnalysisReport report = new AnalysisReport();
        report.setTotalTicketCount(5);
        report.setTotalTotalCents(5000);
        report.setTotalSubtotalCents(4500);
        report.setHourlyTraffic(Collections.emptyMap());
        report.setItemRankings(Collections.emptyList());
        report.setSideRankings(Collections.emptyMap());
        report.setDayRankings(Collections.emptyList());

        when(analysisService.generateReport(any(LocalDate.class), any(LocalDate.class))).thenReturn(report);

        mockMvc.perform(get("/api/analysis")
                .param("startDate", "2023-01-01")
                .param("endDate", "2023-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.payload.totalTicketCount").value(5))
                .andExpect(jsonPath("$.payload.totalTotalCents").value(5000))
                .andExpect(jsonPath("$.payload.totalSubtotalCents").value(4500));
    }

    @Test
    public void testGetAnalysisInvalidDate() throws Exception {
        mockMvc.perform(get("/api/analysis")
                .param("startDate", "invalid-date")
                .param("endDate", "2023-01-02"))
                .andExpect(status().isBadRequest());
    }
}
