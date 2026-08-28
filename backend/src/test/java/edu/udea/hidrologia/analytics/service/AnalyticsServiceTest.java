package edu.udea.hidrologia.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import edu.udea.hidrologia.analytics.dto.AdminAnalyticsSummaryResponse;
import edu.udea.hidrologia.analytics.repository.AnalyticsRepository;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

class AnalyticsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-27T15:30:00Z");
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    private AnalyticsRepository analyticsRepository;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsRepository = Mockito.mock(AnalyticsRepository.class);
        analyticsService = new AnalyticsService(
                analyticsRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsFirstSiteVisitBySession() {
        analyticsService.recordSiteVisit(SESSION_ID);

        verify(analyticsRepository).insertSiteVisit(SESSION_ID, NOW);
    }

    @Test
    void countsPublicSiteVisits() {
        when(analyticsRepository.countSiteVisits()).thenReturn(12L);

        assertThat(analyticsService.countSiteVisits()).isEqualTo(12L);
    }

    @Test
    void recordsActiveSectionViewBySlug() {
        when(analyticsRepository.findActiveSectionIdBySlug("taller-1")).thenReturn(Optional.of(1L));

        analyticsService.recordSectionView("taller-1", SESSION_ID);

        verify(analyticsRepository).insertSectionView(SESSION_ID, 1L, NOW);
    }

    @Test
    void rejectsMissingSectionView() {
        when(analyticsRepository.findActiveSectionIdBySlug("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.recordSectionView("no-existe", SESSION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Section not found");
        verify(analyticsRepository, never()).insertSectionView(Mockito.any(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    void recordsPublishedPostViewOnly() {
        when(analyticsRepository.findPublishedPostId(9L)).thenReturn(Optional.of(9L));

        analyticsService.recordPostView(9L, SESSION_ID);

        verify(analyticsRepository).insertPostView(SESSION_ID, 9L, NOW);
    }

    @Test
    void rejectsDraftOrArchivedPostViewAsNotFound() {
        when(analyticsRepository.findPublishedPostId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.recordPostView(9L, SESSION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");
        verify(analyticsRepository, never()).insertPostView(Mockito.any(), Mockito.anyLong(), Mockito.any());
    }

    @Test
    void buildsAdminSummaryWithBogotaCalendarPeriodsRankingsAndQuestionCounts() {
        when(analyticsRepository.countSiteVisits()).thenReturn(10L);
        when(analyticsRepository.countSiteVisitsBetween(
                Instant.parse("2026-08-27T05:00:00Z"),
                Instant.parse("2026-08-28T05:00:00Z"))).thenReturn(2L);
        when(analyticsRepository.countSiteVisitsBetween(
                Instant.parse("2026-08-24T05:00:00Z"),
                Instant.parse("2026-08-31T05:00:00Z"))).thenReturn(5L);
        when(analyticsRepository.countSiteVisitsBetween(
                Instant.parse("2026-08-01T05:00:00Z"),
                Instant.parse("2026-09-01T05:00:00Z"))).thenReturn(8L);
        when(analyticsRepository.findSectionViewsRanking()).thenReturn(List.of(
                new AnalyticsRepository.SectionViewsRow(1L, SectionType.TALLER, "Taller 1", "taller-1", 4),
                new AnalyticsRepository.SectionViewsRow(4L, SectionType.PARCIAL, "Parcial 1", "parcial-1", 3),
                new AnalyticsRepository.SectionViewsRow(2L, SectionType.TALLER, "Taller 2", "taller-2", 0)));
        when(analyticsRepository.findMostViewedPublishedPosts(5)).thenReturn(List.of(
                new AnalyticsRepository.PostViewsRow(
                        7L,
                        "Balance hidrico",
                        1L,
                        SectionType.TALLER,
                        "Taller 1",
                        "taller-1",
                        "Morfometria",
                        6)));
        when(analyticsRepository.findQuestionCounts()).thenReturn(new AnalyticsRepository.QuestionCountsRow(12, 4, 5));
        when(analyticsRepository.findDailyVisitsBetween(LocalDate.parse("2026-07-29"), LocalDate.parse("2026-08-27")))
                .thenReturn(List.of(new AnalyticsRepository.DailyVisitsRow(LocalDate.parse("2026-08-27"), 2)));

        AdminAnalyticsSummaryResponse summary = analyticsService.getAdminSummary();

        assertThat(summary.totalVisits()).isEqualTo(10);
        assertThat(summary.visitsToday()).isEqualTo(2);
        assertThat(summary.visitsThisWeek()).isEqualTo(5);
        assertThat(summary.visitsThisMonth()).isEqualTo(8);
        assertThat(summary.mostViewedSections()).extracting("slug").containsExactly("taller-1", "parcial-1", "taller-2");
        assertThat(summary.mostViewedWorkshop().slug()).isEqualTo("taller-1");
        assertThat(summary.mostViewedExam().slug()).isEqualTo("parcial-1");
        assertThat(summary.mostViewedPosts()).hasSize(1);
        assertThat(summary.mostViewedPosts().get(0).views()).isEqualTo(6);
        assertThat(summary.questions().total()).isEqualTo(12);
        assertThat(summary.questions().pending()).isEqualTo(4);
        assertThat(summary.questions().published()).isEqualTo(5);
        assertThat(summary.dailyVisits()).hasSize(30);
        assertThat(summary.dailyVisits().get(29).date()).isEqualTo(LocalDate.parse("2026-08-27"));
        assertThat(summary.dailyVisits().get(29).visits()).isEqualTo(2);
    }

    @Test
    void returnsNullMostViewedWorkshopAndExamWhenNoViewsExist() {
        when(analyticsRepository.findSectionViewsRanking()).thenReturn(List.of(
                new AnalyticsRepository.SectionViewsRow(1L, SectionType.TALLER, "Taller 1", "taller-1", 0),
                new AnalyticsRepository.SectionViewsRow(4L, SectionType.PARCIAL, "Parcial 1", "parcial-1", 0)));
        when(analyticsRepository.findMostViewedPublishedPosts(5)).thenReturn(List.of());
        when(analyticsRepository.findQuestionCounts()).thenReturn(new AnalyticsRepository.QuestionCountsRow(0, 0, 0));
        when(analyticsRepository.findDailyVisitsBetween(LocalDate.parse("2026-07-29"), LocalDate.parse("2026-08-27")))
                .thenReturn(List.of());

        AdminAnalyticsSummaryResponse summary = analyticsService.getAdminSummary();

        assertThat(summary.mostViewedWorkshop()).isNull();
        assertThat(summary.mostViewedExam()).isNull();
        assertThat(summary.dailyVisits()).extracting("visits").containsOnly(0L);
    }
}
