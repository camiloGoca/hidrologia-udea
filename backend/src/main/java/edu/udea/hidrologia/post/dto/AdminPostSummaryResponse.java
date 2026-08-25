package edu.udea.hidrologia.post.dto;

import java.time.Instant;

import edu.udea.hidrologia.post.entity.PostStatus;

public record AdminPostSummaryResponse(
        Long id,
        String title,
        PostStatus status,
        PostSectionResponse section,
        boolean hasSourceQuestion,
        Long sourceQuestionId,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt) {
}
