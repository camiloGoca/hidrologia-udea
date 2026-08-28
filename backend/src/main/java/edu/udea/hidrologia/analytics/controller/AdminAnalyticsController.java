package edu.udea.hidrologia.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.analytics.dto.AdminAnalyticsSummaryResponse;
import edu.udea.hidrologia.analytics.service.AnalyticsService;

@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get private analytics summary")
    public AdminAnalyticsSummaryResponse getSummary() {
        return analyticsService.getAdminSummary();
    }
}
