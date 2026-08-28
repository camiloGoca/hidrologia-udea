package edu.udea.hidrologia.analytics.dto;

import edu.udea.hidrologia.section.entity.SectionType;

public record AdminAnalyticsSectionResponse(
        Long id,
        SectionType type,
        String name,
        String slug,
        long views) {
}
