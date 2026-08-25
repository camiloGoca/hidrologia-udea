package edu.udea.hidrologia.post.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostSourceQuestionResponse;
import edu.udea.hidrologia.post.dto.AdminPostSummaryResponse;
import edu.udea.hidrologia.post.dto.AdminPostsResponse;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.dto.UpdatePostRequest;
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

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final SectionRepository sectionRepository;
    private final Clock clock;

    public AdminPostService(PostRepository postRepository, SectionRepository sectionRepository, Clock clock) {
        this.postRepository = postRepository;
        this.sectionRepository = sectionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminPostsResponse findPostsByStatus(PostStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
        Page<Post> postPage = postRepository.findByStatus(status, pageRequest);

        return new AdminPostsResponse(
                postPage.getContent().stream()
                        .map(this::toSummaryResponse)
                        .toList(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminPostResponse findAdminPostById(Long id) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        return toResponse(post);
    }

    @Transactional
    public AdminPostResponse updatePost(Long id, @Valid UpdatePostRequest request) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Section section = sectionRepository.findBySlugAndActiveTrue(request.sectionSlug().strip())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        String title = normalize(request.title());
        String content = normalize(request.content());
        validateEditableContent(post, title, content);
        post.update(title, content, section, Instant.now(clock));

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

    private AdminPostSummaryResponse toSummaryResponse(Post post) {
        StudentQuestion sourceQuestion = post.getSourceQuestion();

        return new AdminPostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getStatus(),
                toSectionResponse(post.getSection()),
                sourceQuestion != null,
                sourceQuestion == null ? null : sourceQuestion.getId(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getPublishedAt());
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void validateEditableContent(Post post, String title, String content) {
        if (post.getStatus() != PostStatus.DRAFT && (title.isBlank() || content.isBlank())) {
            throw new InvalidPostPublicationException(
                    "La publicación necesita título y contenido antes de guardarse.");
        }
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
