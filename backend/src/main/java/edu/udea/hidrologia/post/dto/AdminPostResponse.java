package edu.udea.hidrologia.post.dto;

import java.time.Instant;
import java.util.List;

import edu.udea.hidrologia.post.entity.PostStatus;

public record AdminPostResponse(
        Long id,
        String title,
        String content,
        PostStatus status,
        Long sourceQuestionId,
        PostSectionResponse section,
        AdminPostSourceQuestionResponse sourceQuestion,
        List<AdminPostTagResponse> tags,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt) {
}
