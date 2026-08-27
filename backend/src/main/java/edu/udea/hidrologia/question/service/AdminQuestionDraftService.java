package edu.udea.hidrologia.question.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.post.service.AdminPostService;
import edu.udea.hidrologia.post.service.PostImageCleanupService;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@Service
public class AdminQuestionDraftService {

    private final StudentQuestionRepository studentQuestionRepository;
    private final PostRepository postRepository;
    private final AdminPostService adminPostService;
    private final PostImageCleanupService postImageCleanupService;
    private final Clock clock;

    public AdminQuestionDraftService(
            StudentQuestionRepository studentQuestionRepository,
            PostRepository postRepository,
            AdminPostService adminPostService,
            PostImageCleanupService postImageCleanupService,
            Clock clock) {
        this.studentQuestionRepository = studentQuestionRepository;
        this.postRepository = postRepository;
        this.adminPostService = adminPostService;
        this.postImageCleanupService = postImageCleanupService;
        this.clock = clock;
    }

    @Transactional
    public AdminPostResponse createDraft(Long questionId) {
        StudentQuestion question = studentQuestionRepository.findByIdWithSectionAndAttachment(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (question.getStatus() != StudentQuestionStatus.PENDING) {
            throw new InvalidQuestionStatusTransitionException();
        }

        if (postRepository.existsBySourceQuestionId(question.getId())) {
            throw new QuestionDraftConflictException("This question already has a linked post");
        }

        try {
            Post draft = Post.createQuestionDraft(question, Instant.now(clock));
            Post savedDraft = postRepository.saveAndFlush(draft);

            return adminPostService.toResponse(savedDraft);
        } catch (DataIntegrityViolationException exception) {
            throw new QuestionDraftConflictException("This question already has a linked post");
        }
    }

    @Transactional
    public void discardDraft(Long questionId) {
        studentQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        Post draft = postRepository.findBySourceQuestionId(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question draft not found"));

        if (draft.getStatus() != PostStatus.DRAFT) {
            throw new QuestionDraftConflictException("Only draft posts can be discarded");
        }

        postImageCleanupService.deleteAllForPost(draft.getId());
        postRepository.delete(draft);
    }
}
