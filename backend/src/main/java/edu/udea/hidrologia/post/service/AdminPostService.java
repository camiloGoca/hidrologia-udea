package edu.udea.hidrologia.post.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import edu.udea.hidrologia.post.content.PostContentDocumentService;
import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostImageResponse;
import edu.udea.hidrologia.post.dto.AdminPostSourceQuestionResponse;
import edu.udea.hidrologia.post.dto.AdminPostSummaryResponse;
import edu.udea.hidrologia.post.dto.AdminPostTagResponse;
import edu.udea.hidrologia.post.dto.AdminPostsResponse;
import edu.udea.hidrologia.post.dto.CreatePostRequest;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.dto.UpdatePostRequest;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostImage;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostImageRepository;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.tag.entity.Tag;
import edu.udea.hidrologia.tag.repository.TagRepository;
import jakarta.validation.Valid;

@Service
@Validated
public class AdminPostService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final SectionRepository sectionRepository;
    private final TagRepository tagRepository;
    private final PostContentDocumentService postContentDocumentService;
    private final Clock clock;

    public AdminPostService(
            PostRepository postRepository,
            PostImageRepository postImageRepository,
            SectionRepository sectionRepository,
            TagRepository tagRepository,
            PostContentDocumentService postContentDocumentService,
            Clock clock) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.sectionRepository = sectionRepository;
        this.tagRepository = tagRepository;
        this.postContentDocumentService = postContentDocumentService;
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
    public AdminPostResponse createManualDraft(@Valid CreatePostRequest request) {
        Section section = sectionRepository.findBySlugAndActiveTrue(request.sectionSlug().strip())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        Instant now = Instant.now(clock);
        Post draft = postRepository.saveAndFlush(Post.createManualDraft(section, now));

        return toResponse(draft);
    }

    @Transactional
    public AdminPostResponse updatePost(Long id, @Valid UpdatePostRequest request) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Section section = sectionRepository.findBySlugAndActiveTrue(request.sectionSlug().strip())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        String title = normalize(request.title());
        Map<String, Object> contentDocument = postContentDocumentService.validate(request.contentDocument());
        String content = postContentDocumentService.extractPlainText(contentDocument);
        validateEditableContent(post, title, content);
        List<Tag> tags = resolveTags(request.tagIds());
        Instant now = Instant.now(clock);
        post.update(title, content, contentDocument, section, now);
        if (request.tagIds() != null) {
            post.replaceTags(tags, now);
        }

        return toResponse(post);
    }

    @Transactional
    public void discardManualDraft(Long id) {
        Post post = postRepository.findAdminById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (post.getStatus() != PostStatus.DRAFT || post.getSourceQuestion() != null) {
            throw new PostStateConflictException("Only manual draft posts can be discarded here");
        }
        if (postImageRepository.existsByPostId(id)) {
            throw new PostStateConflictException("Remove post images before discarding this draft");
        }

        postRepository.delete(post);
    }

    public AdminPostResponse toResponse(Post post) {
        StudentQuestion sourceQuestion = post.getSourceQuestion();

        return new AdminPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                postContentDocumentService.toSerializableDocument(post.getContentDocument()),
                post.getStatus(),
                sourceQuestion == null ? null : sourceQuestion.getId(),
                toSectionResponse(post.getSection()),
                sourceQuestion == null ? null : toSourceQuestionResponse(sourceQuestion),
                toTagResponses(post),
                toImageResponses(post),
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

    private List<Tag> resolveTags(List<Long> tagIds) {
        if (tagIds == null) {
            return List.of();
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(tagIds);
        if (uniqueIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new InvalidPostPublicationException("Uno o más hashtags seleccionados no existen.");
        }
        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = tagRepository.findByIdIn(uniqueIds);
        if (tags.size() != uniqueIds.size()) {
            throw new InvalidPostPublicationException("Uno o más hashtags seleccionados no existen.");
        }

        return tags;
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

    private List<AdminPostTagResponse> toTagResponses(Post post) {
        return post.getTags().stream()
                .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Tag::getSlug))
                .map(tag -> new AdminPostTagResponse(tag.getId(), tag.getName(), tag.getSlug()))
                .toList();
    }

    private List<AdminPostImageResponse> toImageResponses(Post post) {
        if (post.getId() == null) {
            return List.of();
        }

        List<PostImage> images = postImageRepository.findByPostIdOrderById(post.getId());
        if (images == null) {
            return List.of();
        }

        return images.stream()
                .map(image -> new AdminPostImageResponse(
                        image.getId(),
                        image.getSecureUrl(),
                        image.getFormat(),
                        image.getWidth(),
                        image.getHeight(),
                        image.getBytes(),
                        image.getAltText(),
                        image.getCreatedAt()))
                .toList();
    }
}
