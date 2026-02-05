package com.ticketer.controllers;

import com.ticketer.api.ApiResponse;
import com.ticketer.models.AnalysisReport;
import com.ticketer.services.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Autowired
    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping
    public ApiResponse<AnalysisReport> getAnalysis(@RequestParam String startDate, @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        AnalysisReport report = analysisService.generateReport(start, end);
        return ApiResponse.success(report);
    }
}
