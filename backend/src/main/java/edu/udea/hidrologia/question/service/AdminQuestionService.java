package edu.udea.hidrologia.question.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.question.dto.AdminQuestionAttachmentResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionDetailResponse;
import edu.udea.hidrologia.question.dto.AdminLinkedPostResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionSectionResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionStatusUpdateResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionSummaryResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionsResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.repository.PostRepository;
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
    private static final Set<StudentQuestionStatus> LISTABLE_STATUSES = EnumSet.of(
            StudentQuestionStatus.PENDING,
            StudentQuestionStatus.ARCHIVED,
            StudentQuestionStatus.REJECTED,
            StudentQuestionStatus.PUBLISHED);

    private final StudentQuestionRepository studentQuestionRepository;
    private final PostRepository postRepository;
    private final Clock clock;

    public AdminQuestionService(
            StudentQuestionRepository studentQuestionRepository,
            PostRepository postRepository,
            Clock clock) {
        this.studentQuestionRepository = studentQuestionRepository;
        this.postRepository = postRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminQuestionsResponse findQuestionsByStatus(StudentQuestionStatus status, int page, int size) {
        if (!LISTABLE_STATUSES.contains(status)) {
            throw new UnsupportedQuestionStatusFilterException();
        }

        int safePage = Math.max(page, 0);
        int safeSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<StudentQuestion> questionPage = studentQuestionRepository.findByStatus(status, pageRequest);
        Map<Long, Post> linkedPostsByQuestionId = findLinkedPostsByQuestionId(questionPage.getContent());

        return new AdminQuestionsResponse(
                questionPage.getContent().stream()
                        .map(question -> toSummaryResponse(question, linkedPostsByQuestionId.containsKey(question.getId())))
                        .toList(),
                questionPage.getNumber(),
                questionPage.getSize(),
                questionPage.getTotalElements(),
                questionPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminQuestionsResponse findPendingQuestions(int page, int size) {
        return findQuestionsByStatus(StudentQuestionStatus.PENDING, page, size);
    }

    @Transactional(readOnly = true)
    public AdminQuestionDetailResponse findQuestionById(Long id) {
        StudentQuestion question = studentQuestionRepository.findByIdWithSectionAndAttachment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        AdminLinkedPostResponse linkedPost = postRepository.findBySourceQuestionId(id)
                .map(this::toLinkedPostResponse)
                .orElse(null);

        return toDetailResponse(question, linkedPost);
    }

    @Transactional
    public AdminQuestionStatusUpdateResponse rejectQuestion(Long id) {
        return transitionQuestion(id, StudentQuestionStatus.PENDING, StudentQuestionStatus.REJECTED);
    }

    @Transactional
    public AdminQuestionStatusUpdateResponse archiveQuestion(Long id) {
        return transitionQuestion(id, StudentQuestionStatus.PENDING, StudentQuestionStatus.ARCHIVED);
    }

    @Transactional
    public AdminQuestionStatusUpdateResponse reopenQuestion(Long id) {
        StudentQuestion question = studentQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (question.getStatus() != StudentQuestionStatus.REJECTED
                && question.getStatus() != StudentQuestionStatus.ARCHIVED) {
            throw new InvalidQuestionStatusTransitionException();
        }

        return updateStatus(question, StudentQuestionStatus.PENDING);
    }

    private AdminQuestionStatusUpdateResponse transitionQuestion(
            Long id,
            StudentQuestionStatus expectedStatus,
            StudentQuestionStatus targetStatus) {
        StudentQuestion question = studentQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (question.getStatus() != expectedStatus) {
            throw new InvalidQuestionStatusTransitionException();
        }

        if ((targetStatus == StudentQuestionStatus.REJECTED || targetStatus == StudentQuestionStatus.ARCHIVED)
                && postRepository.existsBySourceQuestionId(question.getId())) {
            throw new QuestionDraftConflictException("This question has a linked post draft");
        }

        return updateStatus(question, targetStatus);
    }

    private AdminQuestionStatusUpdateResponse updateStatus(
            StudentQuestion question,
            StudentQuestionStatus targetStatus) {
        Instant updatedAt = Instant.now(clock);
        question.transitionTo(targetStatus, updatedAt);
        StudentQuestion savedQuestion = studentQuestionRepository.save(question);

        return new AdminQuestionStatusUpdateResponse(
                savedQuestion.getId(),
                savedQuestion.getStatus(),
                savedQuestion.getUpdatedAt());
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Map<Long, Post> findLinkedPostsByQuestionId(java.util.List<StudentQuestion> questions) {
        Set<Long> questionIds = questions.stream()
                .map(StudentQuestion::getId)
                .collect(Collectors.toSet());

        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return postRepository.findBySourceQuestionIdIn(questionIds).stream()
                .collect(Collectors.toMap(post -> post.getSourceQuestion().getId(), Function.identity()));
    }

    private AdminQuestionSummaryResponse toSummaryResponse(StudentQuestion question, boolean hasLinkedPost) {
        return new AdminQuestionSummaryResponse(
                question.getId(),
                question.getNickname(),
                toSectionResponse(question.getSection()),
                question.getStatus(),
                toPreview(question.getQuestion()),
                question.getAttachment() != null,
                hasLinkedPost,
                question.getCreatedAt());
    }

    private AdminQuestionDetailResponse toDetailResponse(StudentQuestion question, AdminLinkedPostResponse linkedPost) {
        return new AdminQuestionDetailResponse(
                question.getId(),
                question.getNickname(),
                question.getQuestion(),
                question.getStatus(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                toSectionResponse(question.getSection()),
                toAttachmentResponse(question.getAttachment()),
                linkedPost);
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

    private AdminLinkedPostResponse toLinkedPostResponse(Post post) {
        return new AdminLinkedPostResponse(
                post.getId(),
                post.getStatus(),
                post.getTitle());
    }

    private String toPreview(String question) {
        String trimmedQuestion = question.trim();

        if (trimmedQuestion.length() <= PREVIEW_LIMIT) {
            return trimmedQuestion;
        }

        return trimmedQuestion.substring(0, PREVIEW_LIMIT).stripTrailing() + ELLIPSIS;
    }
}
