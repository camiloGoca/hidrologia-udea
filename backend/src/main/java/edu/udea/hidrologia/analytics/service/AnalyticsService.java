package edu.udea.hidrologia.analytics.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.analytics.dto.AdminAnalyticsDailyVisitResponse;
import edu.udea.hidrologia.analytics.dto.AdminAnalyticsPostResponse;
import edu.udea.hidrologia.analytics.dto.AdminAnalyticsQuestionsResponse;
import edu.udea.hidrologia.analytics.dto.AdminAnalyticsSectionResponse;
import edu.udea.hidrologia.analytics.dto.AdminAnalyticsSummaryResponse;
import edu.udea.hidrologia.analytics.repository.AnalyticsRepository;
import edu.udea.hidrologia.analytics.repository.AnalyticsSummaryClock;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@Service
public class AnalyticsService {

    private static final int MOST_VIEWED_POSTS_LIMIT = 5;
    private static final int DAILY_VISITS_DAYS = 30;

    private final AnalyticsRepository analyticsRepository;
    private final Clock clock;

    public AnalyticsService(AnalyticsRepository analyticsRepository, Clock clock) {
        this.analyticsRepository = analyticsRepository;
        this.clock = clock;
    }

    @Transactional
    public void recordSiteVisit(UUID sessionId) {
        analyticsRepository.insertSiteVisit(sessionId, now());
    }

    @Transactional
    public void recordSectionView(String slug, UUID sessionId) {
        Long sectionId = analyticsRepository.findActiveSectionIdBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        analyticsRepository.insertSectionView(sessionId, sectionId, now());
    }

    @Transactional
    public void recordPostView(Long postId, UUID sessionId) {
        Long publishedPostId = analyticsRepository.findPublishedPostId(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        analyticsRepository.insertPostView(sessionId, publishedPostId, now());
    }

    @Transactional(readOnly = true)
    public long countSiteVisits() {
        return analyticsRepository.countSiteVisits();
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsSummaryResponse getAdminSummary() {
        Instant currentInstant = now();
        LocalDate today = LocalDate.ofInstant(currentInstant, AnalyticsSummaryClock.BOGOTA_ZONE);
        Instant todayStart = today.atStartOfDay(AnalyticsSummaryClock.BOGOTA_ZONE).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(AnalyticsSummaryClock.BOGOTA_ZONE).toInstant();
        LocalDate weekStartDate = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        Instant weekStart = weekStartDate.atStartOfDay(AnalyticsSummaryClock.BOGOTA_ZONE).toInstant();
        Instant nextWeekStart = weekStartDate.plusWeeks(1).atStartOfDay(AnalyticsSummaryClock.BOGOTA_ZONE).toInstant();
        LocalDate monthStartDate = today.withDayOfMonth(1);
        Instant monthStart = monthStartDate.atStartOfDay(AnalyticsSummaryClock.BOGOTA_ZONE).toInstant();
        Instant nextMonthStart = monthStartDate.plusMonths(1).atStartOfDay(AnalyticsSummaryClock.BOGOTA_ZONE).toInstant();

        List<AdminAnalyticsSectionResponse> sections = analyticsRepository.findSectionViewsRanking().stream()
                .map(this::toSectionResponse)
                .toList();
        List<AdminAnalyticsPostResponse> posts = analyticsRepository.findMostViewedPublishedPosts(MOST_VIEWED_POSTS_LIMIT).stream()
                .map(this::toPostResponse)
                .toList();
        AnalyticsRepository.QuestionCountsRow questionCounts = analyticsRepository.findQuestionCounts();

        return new AdminAnalyticsSummaryResponse(
                analyticsRepository.countSiteVisits(),
                analyticsRepository.countSiteVisitsBetween(todayStart, tomorrowStart),
                analyticsRepository.countSiteVisitsBetween(weekStart, nextWeekStart),
                analyticsRepository.countSiteVisitsBetween(monthStart, nextMonthStart),
                sections,
                mostViewedSectionByType(sections, SectionType.TALLER),
                mostViewedSectionByType(sections, SectionType.PARCIAL),
                posts,
                new AdminAnalyticsQuestionsResponse(
                        questionCounts.total(),
                        questionCounts.pending(),
                        questionCounts.published()),
                dailyVisits(today));
    }

    private List<AdminAnalyticsDailyVisitResponse> dailyVisits(LocalDate today) {
        LocalDate startDate = today.minusDays(DAILY_VISITS_DAYS - 1L);
        Map<LocalDate, Long> visitsByDate = analyticsRepository.findDailyVisitsBetween(startDate, today).stream()
                .collect(Collectors.toMap(
                        AnalyticsRepository.DailyVisitsRow::date,
                        AnalyticsRepository.DailyVisitsRow::visits,
                        (left, right) -> left));

        return startDate.datesUntil(today.plusDays(1))
                .map(date -> new AdminAnalyticsDailyVisitResponse(date, visitsByDate.getOrDefault(date, 0L)))
                .toList();
    }

    private AdminAnalyticsSectionResponse mostViewedSectionByType(
            List<AdminAnalyticsSectionResponse> sections,
            SectionType type) {
        return sections.stream()
                .filter(section -> section.type() == type)
                .filter(section -> section.views() > 0)
                .findFirst()
                .orElse(null);
    }

    private AdminAnalyticsSectionResponse toSectionResponse(AnalyticsRepository.SectionViewsRow row) {
        return new AdminAnalyticsSectionResponse(
                row.id(),
                row.type(),
                row.name(),
                row.slug(),
                row.views());
    }

    private AdminAnalyticsPostResponse toPostResponse(AnalyticsRepository.PostViewsRow row) {
        return new AdminAnalyticsPostResponse(
                row.id(),
                row.title(),
                new PostSectionResponse(
                        row.sectionId(),
                        row.sectionType(),
                        row.sectionName(),
                        row.sectionSlug(),
                        row.sectionDescription()),
                row.views());
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
