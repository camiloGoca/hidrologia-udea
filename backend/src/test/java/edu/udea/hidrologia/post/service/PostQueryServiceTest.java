package edu.udea.hidrologia.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import edu.udea.hidrologia.post.content.PostContentDocumentService;
import edu.udea.hidrologia.post.dto.PostDetailResponse;
import edu.udea.hidrologia.post.dto.PostSummaryResponse;
import edu.udea.hidrologia.post.dto.SectionPostsResponse;
import edu.udea.hidrologia.post.dto.TagPostsResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostImage;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostImageRepository;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.tag.entity.Tag;
import edu.udea.hidrologia.tag.repository.TagRepository;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private TagRepository tagRepository;

    private PostQueryService postQueryService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        postQueryService = new PostQueryService(
                postRepository,
                postImageRepository,
                sectionRepository,
                tagRepository,
                new PostContentDocumentService(JsonMapper.builder().build()));
    }

    @Test
    void returnsSectionMetadataAndEmptyPostsWhenSectionHasNoPublishedPosts() {
        Section section = section();
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(postRepository.findBySectionAndStatusOrderByPublishedAtDescIdDesc(section, PostStatus.PUBLISHED))
                .thenReturn(List.of());

        SectionPostsResponse response = postQueryService.findPublishedPostsBySection("taller-1");

        assertThat(response.section().slug()).isEqualTo("taller-1");
        assertThat(response.posts()).isEmpty();
        verify(postRepository).findBySectionAndStatusOrderByPublishedAtDescIdDesc(section, PostStatus.PUBLISHED);
    }

    @Test
    void mapsPublishedPostsBySectionToSummaryDtos() {
        Section section = section();
        Tag tag = tag();
        Post post = publishedPost(section, Set.of(tag));
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(postRepository.findBySectionAndStatusOrderByPublishedAtDescIdDesc(section, PostStatus.PUBLISHED))
                .thenReturn(List.of(post));

        SectionPostsResponse response = postQueryService.findPublishedPostsBySection("taller-1");

        assertThat(response.posts())
                .extracting(PostSummaryResponse::title)
                .containsExactly("Pregunta publicada");
        assertThat(response.posts().get(0).tags().get(0).slug()).isEqualTo("morfometria");
        assertThat(response.posts().get(0).section().name()).isEqualTo("Taller 1");
    }

    @Test
    void throwsNotFoundWhenSectionDoesNotExist() {
        when(sectionRepository.findBySlugAndActiveTrue("desconocida")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postQueryService.findPublishedPostsBySection("desconocida"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Section not found");
    }

    @Test
    void returnsPublishedPostDetail() {
        Post post = publishedPost(section(), Set.of(tag()));
        when(postRepository.findByIdAndStatus(1L, PostStatus.PUBLISHED)).thenReturn(Optional.of(post));

        PostDetailResponse response = postQueryService.findPublishedPostById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Pregunta publicada");
        assertThat(response.content()).isEqualTo("Contenido de texto seguro.");
        assertThat(response.tags()).hasSize(1);
        assertThat(response.images()).isEmpty();
        verify(postRepository).findByIdAndStatus(1L, PostStatus.PUBLISHED);
    }

    @Test
    void returnsOnlyReferencedPublicImagesForPostDetail() {
        Section section = section();
        Post post = new Post(
                1L,
                section,
                "Pregunta publicada",
                "Figura de validacion",
                imageDocument(7L),
                PostStatus.PUBLISHED,
                CREATED_AT,
                CREATED_AT,
                PUBLISHED_AT,
                Set.of(),
                null);
        PostImage referenced = new PostImage(
                7L,
                post,
                "hidrologia-udea/posts/1/post-1-image-7",
                "https://res.cloudinary.com/demo/image/upload/post-1-image-7.jpg",
                "jpg",
                900,
                600,
                1200L,
                "Grafica de prueba",
                CREATED_AT);
        when(postRepository.findByIdAndStatus(1L, PostStatus.PUBLISHED)).thenReturn(Optional.of(post));
        when(postImageRepository.findByPostIdAndIdInOrderById(1L, Set.of(7L))).thenReturn(List.of(referenced));

        PostDetailResponse response = postQueryService.findPublishedPostById(1L);

        assertThat(response.images()).hasSize(1);
        assertThat(response.images().get(0).id()).isEqualTo(7L);
        assertThat(response.images().get(0).secureUrl())
                .isEqualTo("https://res.cloudinary.com/demo/image/upload/post-1-image-7.jpg");
        assertThat(response.images().get(0).altText()).isEqualTo("Grafica de prueba");
        verify(postImageRepository).findByPostIdAndIdInOrderById(1L, Set.of(7L));
    }

    @Test
    void returnsNotFoundWhenDraftPostIsRequestedPublicly() {
        when(postRepository.findByIdAndStatus(99L, PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postQueryService.findPublishedPostById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");

        verify(postRepository).findByIdAndStatus(99L, PostStatus.PUBLISHED);
    }

    @Test
    void returnsNotFoundWhenArchivedPostIsRequestedPublicly() {
        when(postRepository.findByIdAndStatus(100L, PostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postQueryService.findPublishedPostById(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");

        verify(postRepository).findByIdAndStatus(100L, PostStatus.PUBLISHED);
    }

    @Test
    void returnsTagMetadataAndEmptyPostsWhenTagHasNoPublishedPosts() {
        Tag tag = tag();
        when(tagRepository.findBySlug("morfometria")).thenReturn(Optional.of(tag));
        when(postRepository.findByTagSlugAndStatusOrderByPublishedAtDescIdDesc("morfometria", PostStatus.PUBLISHED))
                .thenReturn(List.of());

        TagPostsResponse response = postQueryService.findPublishedPostsByTag("morfometria");

        assertThat(response.tag().name()).isEqualTo("Morfometria");
        assertThat(response.posts()).isEmpty();
    }

    @Test
    void mapsPublishedPostsByTagToSummaryDtos() {
        Section section = section();
        Tag tag = tag();
        Post post = publishedPost(section, Set.of(tag));
        when(tagRepository.findBySlug("morfometria")).thenReturn(Optional.of(tag));
        when(postRepository.findByTagSlugAndStatusOrderByPublishedAtDescIdDesc("morfometria", PostStatus.PUBLISHED))
                .thenReturn(List.of(post));

        TagPostsResponse response = postQueryService.findPublishedPostsByTag("morfometria");

        assertThat(response.posts()).hasSize(1);
        assertThat(response.posts().get(0).publishedAt()).isEqualTo(PUBLISHED_AT);
    }

    @Test
    void returnsRenamedTagMetadataThroughStableSlug() {
        Tag tag = new Tag(1L, "Morfometría de cuencas", "morfometria", CREATED_AT);
        when(tagRepository.findBySlug("morfometria")).thenReturn(Optional.of(tag));
        when(postRepository.findByTagSlugAndStatusOrderByPublishedAtDescIdDesc("morfometria", PostStatus.PUBLISHED))
                .thenReturn(List.of());

        TagPostsResponse response = postQueryService.findPublishedPostsByTag("morfometria");

        assertThat(response.tag().name()).isEqualTo("Morfometría de cuencas");
        assertThat(response.tag().slug()).isEqualTo("morfometria");
    }

    @Test
    void draftAndArchivedPostsWithTagAreNotReturnedPublicly() {
        Tag tag = tag();
        when(tagRepository.findBySlug("morfometria")).thenReturn(Optional.of(tag));
        when(postRepository.findByTagSlugAndStatusOrderByPublishedAtDescIdDesc("morfometria", PostStatus.PUBLISHED))
                .thenReturn(List.of());

        TagPostsResponse response = postQueryService.findPublishedPostsByTag("morfometria");

        assertThat(response.posts()).isEmpty();
        verify(postRepository).findByTagSlugAndStatusOrderByPublishedAtDescIdDesc("morfometria", PostStatus.PUBLISHED);
    }

    @Test
    void sortsTagsDeterministicallyByNameThenSlug() {
        Section section = section();
        Tag balance = new Tag(2L, "Balance", "balance", CREATED_AT);
        Tag cuencas = new Tag(3L, "Cuencas", "cuencas", CREATED_AT);
        Tag balanceAlt = new Tag(4L, "balance", "balance-alt", CREATED_AT);
        Post post = publishedPost(section, Set.of(cuencas, balanceAlt, balance));
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(postRepository.findBySectionAndStatusOrderByPublishedAtDescIdDesc(section, PostStatus.PUBLISHED))
                .thenReturn(List.of(post));

        SectionPostsResponse response = postQueryService.findPublishedPostsBySection("taller-1");

        assertThat(response.posts().get(0).tags())
                .extracting("slug")
                .containsExactly("balance", "balance-alt", "cuencas");
    }

    @Test
    void throwsNotFoundWhenTagDoesNotExist() {
        when(tagRepository.findBySlug("desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postQueryService.findPublishedPostsByTag("desconocido"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tag not found");
    }

    private Section section() {
        return new Section(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas",
                1,
                true,
                CREATED_AT);
    }

    private Tag tag() {
        return new Tag(1L, "Morfometria", "morfometria", CREATED_AT);
    }

    private Post publishedPost(Section section, Set<Tag> tags) {
        return new Post(
                1L,
                section,
                "Pregunta publicada",
                "Contenido de texto seguro.",
                PostStatus.PUBLISHED,
                CREATED_AT,
                CREATED_AT,
                PUBLISHED_AT,
                new LinkedHashSet<>(tags));
    }

    private Map<String, Object> imageDocument(Long postImageId) {
        return Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "image",
                        "attrs", Map.of(
                                "postImageId", postImageId,
                                "caption", "Figura de validacion"))));
    }
}
