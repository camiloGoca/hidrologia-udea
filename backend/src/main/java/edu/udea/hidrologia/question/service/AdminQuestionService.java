package edu.udea.hidrologia.question.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.question.dto.AdminPendingQuestionsResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionAttachmentResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionDetailResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionSectionResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionSummaryResponse;
import edu.udea.hidrologia.question.entity.QuestionAttachment;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@Service
public class AdminQuestionService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int PREVIEW_LIMIT = 200;
    private static final String ELLIPSIS = "\u2026";

    private final StudentQuestionRepository studentQuestionRepository;

    public AdminQuestionService(StudentQuestionRepository studentQuestionRepository) {
        this.studentQuestionRepository = studentQuestionRepository;
    }

    @Transactional(readOnly = true)
    public AdminPendingQuestionsResponse findPendingQuestions(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<StudentQuestion> questionPage =
                studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest);

        return new AdminPendingQuestionsResponse(
                questionPage.getContent().stream().map(this::toSummaryResponse).toList(),
                questionPage.getNumber(),
                questionPage.getSize(),
                questionPage.getTotalElements(),
                questionPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminQuestionDetailResponse findQuestionById(Long id) {
        StudentQuestion question = studentQuestionRepository.findByIdWithSectionAndAttachment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        return toDetailResponse(question);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private AdminQuestionSummaryResponse toSummaryResponse(StudentQuestion question) {
        return new AdminQuestionSummaryResponse(
                question.getId(),
                question.getNickname(),
                toSectionResponse(question.getSection()),
                toPreview(question.getQuestion()),
                question.getAttachment() != null,
                question.getCreatedAt());
    }

    private AdminQuestionDetailResponse toDetailResponse(StudentQuestion question) {
        return new AdminQuestionDetailResponse(
                question.getId(),
                question.getNickname(),
                question.getQuestion(),
                question.getStatus(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                toSectionResponse(question.getSection()),
                toAttachmentResponse(question.getAttachment()));
    }

    private AdminQuestionSectionResponse toSectionResponse(Section section) {
        return new AdminQuestionSectionResponse(
                section.getId(),
                section.getType(),
                section.getName(),
                section.getSlug(),
                section.getDescription());
    }

    private AdminQuestionAttachmentResponse toAttachmentResponse(QuestionAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return new AdminQuestionAttachmentResponse(
                attachment.getSecureUrl(),
                attachment.getFormat(),
                attachment.getWidth(),
                attachment.getHeight(),
                attachment.getBytes());
    }

    private String toPreview(String question) {
        String trimmedQuestion = question.trim();

        if (trimmedQuestion.length() <= PREVIEW_LIMIT) {
            return trimmedQuestion;
        }

        return trimmedQuestion.substring(0, PREVIEW_LIMIT).stripTrailing() + ELLIPSIS;
    }
}
