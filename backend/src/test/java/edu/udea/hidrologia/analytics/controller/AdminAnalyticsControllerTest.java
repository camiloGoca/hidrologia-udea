package edu.udea.hidrologia.analytics.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.analytics.dto.AdminAnalyticsDailyVisitResponse;
import edu.udea.hidrologia.analytics.dto.AdminAnalyticsPostResponse;
import edu.udea.hidrologia.analytics.dto.AdminAnalyticsQuestionsResponse;
import edu.udea.hidrologia.analytics.dto.AdminAnalyticsSectionResponse;
import edu.udea.hidrologia.analytics.dto.AdminAnalyticsSummaryResponse;
import edu.udea.hidrologia.analytics.service.AnalyticsService;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.section.entity.SectionType;

class AdminAnalyticsControllerTest {

    private AnalyticsService analyticsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        analyticsService = Mockito.mock(AnalyticsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminAnalyticsController(analyticsService))
                .build();
    }

    @Test
    void returnsAdminSummaryWithoutPrivateVisitorData() throws Exception {
        when(analyticsService.getAdminSummary()).thenReturn(new AdminAnalyticsSummaryResponse(
                10,
                2,
                5,
                8,
                List.of(new AdminAnalyticsSectionResponse(1L, SectionType.TALLER, "Taller 1", "taller-1", 4)),
                new AdminAnalyticsSectionResponse(1L, SectionType.TALLER, "Taller 1", "taller-1", 4),
                null,
                List.of(new AdminAnalyticsPostResponse(
                        7L,
                        "Balance",
                        new PostSectionResponse(1L, SectionType.TALLER, "Taller 1", "taller-1", null),
                        6)),
                new AdminAnalyticsQuestionsResponse(12, 4, 5),
                List.of(new AdminAnalyticsDailyVisitResponse(LocalDate.parse("2026-08-27"), 2))));

        mockMvc.perform(get("/api/v1/admin/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits", is(10)))
                .andExpect(jsonPath("$.visitsToday", is(2)))
                .andExpect(jsonPath("$.visitsThisWeek", is(5)))
                .andExpect(jsonPath("$.visitsThisMonth", is(8)))
                .andExpect(jsonPath("$.mostViewedSections", hasSize(1)))
                .andExpect(jsonPath("$.mostViewedWorkshop.slug", is("taller-1")))
                .andExpect(jsonPath("$.mostViewedExam").doesNotExist())
                .andExpect(jsonPath("$.mostViewedPosts[0].title", is("Balance")))
                .andExpect(jsonPath("$.questions.pending", is(4)))
                .andExpect(jsonPath("$.dailyVisits[0].date", is("2026-08-27")))
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andExpect(jsonPath("$.ip").doesNotExist())
                .andExpect(jsonPath("$.userAgent").doesNotExist());
    }
}
