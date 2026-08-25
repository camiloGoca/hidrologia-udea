package edu.udea.hidrologia.question.dto;

import java.time.Instant;

import edu.udea.hidrologia.question.entity.StudentQuestionStatus;

public record AdminQuestionSummaryResponse(
        Long id,
        String nickname,
        AdminQuestionSectionResponse section,
        StudentQuestionStatus status,
        String questionPreview,
        boolean hasAttachment,
        boolean hasLinkedPost,
        Instant createdAt) {
}
