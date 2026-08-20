package edu.udea.hidrologia.question.dto;

import java.time.Instant;

import edu.udea.hidrologia.question.entity.StudentQuestionStatus;

public record CreateStudentQuestionResponse(
        Long id,
        StudentQuestionStatus status,
        Instant createdAt) {
}
