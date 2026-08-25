package edu.udea.hidrologia.post.dto;

import java.time.Instant;

import edu.udea.hidrologia.question.entity.StudentQuestionStatus;

public record AdminPostSourceQuestionResponse(
        Long id,
        String nickname,
        String question,
        StudentQuestionStatus status,
        Instant createdAt,
        boolean hasAttachment) {
}
