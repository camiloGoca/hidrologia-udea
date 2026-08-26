package edu.udea.hidrologia.post.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.post.content.PostContentDocumentService;
import edu.udea.hidrologia.post.dto.PostDetailResponse;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.dto.PostSummaryResponse;
import edu.udea.hidrologia.post.dto.SectionPostsResponse;
import edu.udea.hidrologia.post.dto.TagPostsResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.tag.dto.TagResponse;
import edu.udea.hidrologia.tag.entity.Tag;
import edu.udea.hidrologia.tag.repository.TagRepository;

@Service
public class PostQueryService {

    private final PostRepository postRepository;
    private final SectionRepository sectionRepository;
    private final TagRepository tagRepository;
    private final PostContentDocumentService postContentDocumentService;

    public PostQueryService(
            PostRepository postRepository,
            SectionRepository sectionRepository,
            TagRepository tagRepository,
            PostContentDocumentService postContentDocumentService) {
        this.postRepository = postRepository;
        this.sectionRepository = sectionRepository;
        this.tagRepository = tagRepository;
        this.postContentDocumentService = postContentDocumentService;
    }

    @Transactional(readOnly = true)
    public SectionPostsResponse findPublishedPostsBySection(String sectionSlug) {
        Section section = sectionRepository.findBySlugAndActiveTrue(sectionSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        List<PostSummaryResponse> posts = postRepository
                .findBySectionAndStatusOrderByPublishedAtDescIdDesc(section, PostStatus.PUBLISHED).stream()
                .map(this::toSummaryResponse)
                .toList();

        return new SectionPostsResponse(toSectionResponse(section), posts);
    }

    @Transactional(readOnly = true)
    public PostDetailResponse findPublishedPostById(Long id) {
        Post post = postRepository.findByIdAndStatus(id, PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        return toDetailResponse(post);
    }

    @Transactional(readOnly = true)
    public TagPostsResponse findPublishedPostsByTag(String tagSlug) {
        Tag tag = tagRepository.findBySlug(tagSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        List<PostSummaryResponse> posts = postRepository
                .findByTagSlugAndStatusOrderByPublishedAtDescIdDesc(tagSlug, PostStatus.PUBLISHED).stream()
                .map(this::toSummaryResponse)
                .toList();

        return new TagPostsResponse(toTagResponse(tag), posts);
    }

    private PostSummaryResponse toSummaryResponse(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                toSectionResponse(post.getSection()),
                toTagResponses(post),
                post.getPublishedAt());
    }

    private PostDetailResponse toDetailResponse(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                postContentDocumentService.toSerializableDocument(post.getContentDocument()),
                toSectionResponse(post.getSection()),
                toTagResponses(post),
                post.getPublishedAt());
    }

    private PostSectionResponse toSectionResponse(Section section) {
        return new PostSectionResponse(
                section.getId(),
                section.getType(),
                section.getName(),
                section.getSlug(),
                section.getDescription());
    }

    private List<TagResponse> toTagResponses(Post post) {
        return post.getTags().stream()
                .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Tag::getSlug))
                .map(this::toTagResponse)
                .toList();
    }

    private TagResponse toTagResponse(Tag tag) {
        return new TagResponse(tag.getName(), tag.getSlug());
    }
}
