package edu.udea.hidrologia.analytics.dto;

import edu.udea.hidrologia.post.dto.PostSectionResponse;

public record AdminAnalyticsPostResponse(
        Long id,
        String title,
        PostSectionResponse section,
        long views) {
}
