package edu.udea.hidrologia.question.dto;

import java.time.Instant;

public record AdminQuestionSummaryResponse(
        Long id,
        String nickname,
        AdminQuestionSectionResponse section,
        String questionPreview,
        boolean hasAttachment,
        Instant createdAt) {
}
