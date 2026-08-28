package edu.udea.hidrologia.analytics.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.analytics.service.AnalyticsService;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

class AnalyticsControllerTest {

    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    private AnalyticsService analyticsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        analyticsService = Mockito.mock(AnalyticsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalyticsController(analyticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void recordsSiteVisit() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/visit")
                .contentType("application/json")
                .content(sessionJson()))
                .andExpect(status().isNoContent());

        verify(analyticsService).recordSiteVisit(SESSION_ID);
    }

    @Test
    void rejectsInvalidUuid() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/visit")
                .contentType("application/json")
                .content("""
                        {
                          "sessionId": "not-a-uuid"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordsSectionView() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/sections/taller-1/view")
                .contentType("application/json")
                .content(sessionJson()))
                .andExpect(status().isNoContent());

        verify(analyticsService).recordSectionView("taller-1", SESSION_ID);
    }

    @Test
    void returnsNotFoundForMissingSection() throws Exception {
        Mockito.doThrow(new ResourceNotFoundException("Section not found"))
                .when(analyticsService)
                .recordSectionView("no-existe", SESSION_ID);

        mockMvc.perform(post("/api/v1/analytics/sections/no-existe/view")
                .contentType("application/json")
                .content(sessionJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordsPostView() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/posts/9/view")
                .contentType("application/json")
                .content(sessionJson()))
                .andExpect(status().isNoContent());

        verify(analyticsService).recordPostView(9L, SESSION_ID);
    }

    @Test
    void returnsNotFoundForUnavailablePost() throws Exception {
        Mockito.doThrow(new ResourceNotFoundException("Post not found"))
                .when(analyticsService)
                .recordPostView(9L, SESSION_ID);

        mockMvc.perform(post("/api/v1/analytics/posts/9/view")
                .contentType("application/json")
                .content(sessionJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsPublicVisitCount() throws Exception {
        when(analyticsService.countSiteVisits()).thenReturn(42L);

        mockMvc.perform(get("/api/v1/analytics/visits/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visits", is(42)));
    }

    private String sessionJson() {
        return """
                {
                  "sessionId": "11111111-1111-4111-8111-111111111111"
                }
                """;
    }
}
