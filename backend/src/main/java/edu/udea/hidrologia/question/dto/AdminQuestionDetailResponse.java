package edu.udea.hidrologia.question.dto;

import java.time.Instant;

import edu.udea.hidrologia.question.entity.StudentQuestionStatus;

public record AdminQuestionDetailResponse(
        Long id,
        String nickname,
        String question,
        StudentQuestionStatus status,
        Instant createdAt,
        Instant updatedAt,
        AdminQuestionSectionResponse section,
        AdminQuestionAttachmentResponse attachment,
        AdminLinkedPostResponse linkedPost) {
}
