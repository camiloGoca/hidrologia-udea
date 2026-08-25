package edu.udea.hidrologia.post.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostSourceQuestionResponse;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.dto.UpdatePostDraftRequest;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import jakarta.validation.Valid;

@Service
@Validated
public class AdminPostService {

    private final PostRepository postRepository;
    private final SectionRepository sectionRepository;
    private final Clock clock;

    public AdminPostService(PostRepository postRepository, SectionRepository sectionRepository, Clock clock) {
        this.postRepository = postRepository;
        this.sectionRepository = sectionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminPostResponse findAdminPostById(Long id) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        return toResponse(post);
    }

    @Transactional
    public AdminPostResponse updateDraft(Long id, @Valid UpdatePostDraftRequest request) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getStatus() != PostStatus.DRAFT) {
            throw new PostStateConflictException("Only draft posts can be edited");
        }

        Section section = sectionRepository.findBySlugAndActiveTrue(request.sectionSlug().strip())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        post.updateDraft(
                normalize(request.title()),
                normalize(request.content()),
                section,
                Instant.now(clock));

        return toResponse(post);
    }

    public AdminPostResponse toResponse(Post post) {
        StudentQuestion sourceQuestion = post.getSourceQuestion();

        return new AdminPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                sourceQuestion == null ? null : sourceQuestion.getId(),
                toSectionResponse(post.getSection()),
                sourceQuestion == null ? null : toSourceQuestionResponse(sourceQuestion),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getPublishedAt());
    }

    private String normalize(String value) {
        return value.strip();
    }

    private AdminPostSourceQuestionResponse toSourceQuestionResponse(StudentQuestion question) {
        return new AdminPostSourceQuestionResponse(
                question.getId(),
                question.getNickname(),
                question.getQuestion(),
                question.getStatus(),
                question.getCreatedAt(),
                question.getAttachment() != null);
    }

    private PostSectionResponse toSectionResponse(Section section) {
        return new PostSectionResponse(
                section.getId(),
                section.getType(),
                section.getName(),
                section.getSlug(),
                section.getDescription());
    }
}
