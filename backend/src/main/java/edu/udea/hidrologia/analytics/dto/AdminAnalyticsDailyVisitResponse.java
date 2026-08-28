package edu.udea.hidrologia.analytics.dto;

import java.time.LocalDate;

public record AdminAnalyticsDailyVisitResponse(
        LocalDate date,
        long visits) {
}
