package edu.udea.hidrologia.analytics.dto;

import java.util.List;

public record AdminAnalyticsSummaryResponse(
        long totalVisits,
        long visitsToday,
        long visitsThisWeek,
        long visitsThisMonth,
        List<AdminAnalyticsSectionResponse> mostViewedSections,
        AdminAnalyticsSectionResponse mostViewedWorkshop,
        AdminAnalyticsSectionResponse mostViewedExam,
        List<AdminAnalyticsPostResponse> mostViewedPosts,
        AdminAnalyticsQuestionsResponse questions,
        List<AdminAnalyticsDailyVisitResponse> dailyVisits) {
}
