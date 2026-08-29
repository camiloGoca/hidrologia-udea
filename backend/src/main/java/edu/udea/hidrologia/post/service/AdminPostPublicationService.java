package edu.udea.hidrologia.post.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@Service
public class AdminPostPublicationService {

    private final PostRepository postRepository;
    private final AdminPostService adminPostService;
    private final Clock clock;

    public AdminPostPublicationService(
            PostRepository postRepository,
            AdminPostService adminPostService,
            Clock clock) {
        this.postRepository = postRepository;
        this.adminPostService = adminPostService;
        this.clock = clock;
    }

    @Transactional
    public AdminPostResponse publishDraft(Long id) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getStatus() != PostStatus.DRAFT) {
            throw new PostStateConflictException("Only draft posts can be published");
        }

        validatePublishable(post);

        Instant now = Instant.now(clock);
        StudentQuestion sourceQuestion = post.getSourceQuestion();
        if (sourceQuestion != null) {
            if (sourceQuestion.getStatus() != StudentQuestionStatus.PENDING) {
                throw new PostStateConflictException("Source question must be pending before publication");
            }

            sourceQuestion.transitionTo(StudentQuestionStatus.PUBLISHED, now);
        }
        post.publish(now);

        return adminPostService.toResponse(post);
    }

    @Transactional
    public AdminPostResponse archivePost(Long id) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new PostStateConflictException("Only published posts can be archived");
        }

        Instant now = Instant.now(clock);
        StudentQuestion sourceQuestion = post.getSourceQuestion();
        if (sourceQuestion != null) {
            if (sourceQuestion.getStatus() != StudentQuestionStatus.PUBLISHED) {
                throw new PostStateConflictException("Source question must be published before archiving the post");
            }

            sourceQuestion.transitionTo(StudentQuestionStatus.ARCHIVED, now);
        }
        post.archive(now);

        return adminPostService.toResponse(post);
    }

    @Transactional
    public AdminPostResponse restorePost(Long id) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getStatus() != PostStatus.ARCHIVED) {
            throw new PostStateConflictException("Only archived posts can be restored");
        }

        validatePublishable(post);
        Instant now = Instant.now(clock);
        StudentQuestion sourceQuestion = post.getSourceQuestion();
        if (sourceQuestion != null) {
            if (sourceQuestion.getStatus() != StudentQuestionStatus.ARCHIVED) {
                throw new PostStateConflictException("Source question must be archived before restoring the post");
            }

            sourceQuestion.transitionTo(StudentQuestionStatus.PUBLISHED, now);
        }
        post.restore(now);

        return adminPostService.toResponse(post);
    }

    private void validatePublishable(Post post) {
        if (post.getTitle() == null || post.getTitle().isBlank() || post.getContent() == null
                || post.getContent().isBlank()) {
            throw new InvalidPostPublicationException(
                    "La publicación necesita título y contenido antes de publicarse.");
        }

        if (post.getSection() == null || !post.getSection().isActive()) {
            throw new InvalidPostPublicationException("La publicación debe tener una sección activa.");
        }
    }
}
