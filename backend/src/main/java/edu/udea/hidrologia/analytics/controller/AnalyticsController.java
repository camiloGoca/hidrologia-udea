package edu.udea.hidrologia.analytics.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.analytics.dto.AnalyticsSessionRequest;
import edu.udea.hidrologia.analytics.dto.PublicVisitCountResponse;
import edu.udea.hidrologia.analytics.service.AnalyticsService;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/visit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Register an anonymous site visit")
    public void recordSiteVisit(@Valid @RequestBody AnalyticsSessionRequest request) {
        analyticsService.recordSiteVisit(request.sessionId());
    }

    @PostMapping("/sections/{slug}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Register an anonymous section view")
    public void recordSectionView(
            @PathVariable String slug,
            @Valid @RequestBody AnalyticsSessionRequest request) {
        analyticsService.recordSectionView(slug, request.sessionId());
    }

    @PostMapping("/posts/{id}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Register an anonymous published post view")
    public void recordPostView(
            @PathVariable Long id,
            @Valid @RequestBody AnalyticsSessionRequest request) {
        analyticsService.recordPostView(id, request.sessionId());
    }

    @GetMapping("/visits/count")
    @Operation(summary = "Get public site visit count")
    public PublicVisitCountResponse countSiteVisits() {
        return new PublicVisitCountResponse(analyticsService.countSiteVisits());
    }
}
