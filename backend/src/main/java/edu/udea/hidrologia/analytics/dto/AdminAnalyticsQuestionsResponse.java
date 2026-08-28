package edu.udea.hidrologia.analytics.dto;

public record AdminAnalyticsQuestionsResponse(
        long total,
        long pending,
        long published) {
}
