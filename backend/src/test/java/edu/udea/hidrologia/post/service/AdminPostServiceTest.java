package edu.udea.hidrologia.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostsResponse;
import edu.udea.hidrologia.post.dto.UpdatePostRequest;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.entity.QuestionAttachment;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.tag.entity.Tag;

@ExtendWith(MockitoExtension.class)
class AdminPostServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Mock
    private PostRepository postRepository;

    @Mock
    private SectionRepository sectionRepository;

    private AdminPostService adminPostService;

    @BeforeEach
    void setUp() {
        adminPostService = new AdminPostService(
                postRepository,
                sectionRepository,
                Clock.fixed(UPDATED_AT, ZoneOffset.UTC));
    }

    @Test
    void returnsPostsByStatusWithPaginationAndUpdatedAtOrder() {
        StudentQuestion question = question();
        Post draft = draftPost(question);
        PageRequest pageRequest = pageRequest(0, 20);
        when(postRepository.findByStatus(PostStatus.DRAFT, pageRequest))
                .thenReturn(new PageImpl<>(List.of(draft), pageRequest, 1));

        AdminPostsResponse response = adminPostService.findPostsByStatus(PostStatus.DRAFT, 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).id()).isEqualTo(9L);
        assertThat(response.items().get(0).title()).isEmpty();
        assertThat(response.items().get(0).status()).isEqualTo(PostStatus.DRAFT);
        assertThat(response.items().get(0).section().slug()).isEqualTo("taller-1");
        assertThat(response.items().get(0).hasSourceQuestion()).isTrue();
        assertThat(response.items().get(0).sourceQuestionId()).isEqualTo(1L);
        assertThat(response.items().get(0).createdAt()).isEqualTo(NOW);
        assertThat(response.items().get(0).updatedAt()).isEqualTo(NOW);
        assertThat(response.items().get(0).publishedAt()).isNull();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findByStatus(eq(PostStatus.DRAFT), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort().getOrderFor("updatedAt").isDescending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    void supportsPublishedAndArchivedAdminPostLists() {
        PageRequest pageRequest = pageRequest(0, 20);
        when(postRepository.findByStatus(eq(PostStatus.PUBLISHED), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));
        when(postRepository.findByStatus(eq(PostStatus.ARCHIVED), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        adminPostService.findPostsByStatus(PostStatus.PUBLISHED, 0, 20);
        adminPostService.findPostsByStatus(PostStatus.ARCHIVED, 0, 20);

        verify(postRepository).findByStatus(PostStatus.PUBLISHED, pageRequest);
        verify(postRepository).findByStatus(PostStatus.ARCHIVED, pageRequest);
    }

    @Test
    void clampsInvalidPostListBounds() {
        PageRequest pageRequest = pageRequest(0, 50);
        when(postRepository.findByStatus(PostStatus.PUBLISHED, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AdminPostsResponse response = adminPostService.findPostsByStatus(PostStatus.PUBLISHED, -2, 100);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(50);
        verify(postRepository).findByStatus(PostStatus.PUBLISHED, pageRequest);
    }

    @Test
    void usesDefaultSizeWhenPostListSizeIsInvalid() {
        PageRequest pageRequest = pageRequest(1, 20);
        when(postRepository.findByStatus(PostStatus.ARCHIVED, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AdminPostsResponse response = adminPostService.findPostsByStatus(PostStatus.ARCHIVED, 1, 0);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        verify(postRepository).findByStatus(PostStatus.ARCHIVED, pageRequest);
    }

    @Test
    void listSupportsSourceQuestionNullable() {
        Post post = new Post(
                11L,
                section(1L, SectionType.TALLER, "Taller 1", "taller-1"),
                "Título",
                "Contenido",
                PostStatus.PUBLISHED,
                NOW,
                UPDATED_AT,
                NOW,
                Set.of(),
                null);
        PageRequest pageRequest = pageRequest(0, 20);
        when(postRepository.findByStatus(PostStatus.PUBLISHED, pageRequest))
                .thenReturn(new PageImpl<>(List.of(post), pageRequest, 1));

        AdminPostsResponse response = adminPostService.findPostsByStatus(PostStatus.PUBLISHED, 0, 20);

        assertThat(response.items().get(0).hasSourceQuestion()).isFalse();
        assertThat(response.items().get(0).sourceQuestionId()).isNull();
    }

    @Test
    void returnsAdminPostWithSourceQuestionReference() {
        StudentQuestion question = withAttachment(question());
        Post draft = draftPost(question);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));

        AdminPostResponse response = adminPostService.findAdminPostById(9L);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.status()).isEqualTo(PostStatus.DRAFT);
        assertThat(response.title()).isEmpty();
        assertThat(response.content()).isEmpty();
        assertThat(response.sourceQuestionId()).isEqualTo(1L);
        assertThat(response.sourceQuestion().question()).isEqualTo("Pregunta original");
        assertThat(response.sourceQuestion().hasAttachment()).isTrue();
        assertThat(response.section().slug()).isEqualTo("taller-1");
        assertThat(response.publishedAt()).isNull();
    }

    @Test
    void updatesDraftTitleContentAndSection() {
        StudentQuestion question = question();
        Post draft = draftPost(question);
        Section newSection = section(2L, SectionType.PARCIAL, "Parcial 1", "parcial-1");
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));
        when(sectionRepository.findBySlugAndActiveTrue("parcial-1")).thenReturn(Optional.of(newSection));

        AdminPostResponse response = adminPostService.updatePost(
                9L,
                new UpdatePostRequest("  Título  ", "  Línea 1\nLínea 2  ", "parcial-1"));

        assertThat(response.title()).isEqualTo("Título");
        assertThat(response.content()).isEqualTo("Línea 1\nLínea 2");
        assertThat(response.section().slug()).isEqualTo("parcial-1");
        assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void updatingPostSectionDoesNotChangeSourceQuestionSection() {
        StudentQuestion question = question();
        Post draft = draftPost(question);
        Section newSection = section(2L, SectionType.PARCIAL, "Parcial 1", "parcial-1");
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));
        when(sectionRepository.findBySlugAndActiveTrue("parcial-1")).thenReturn(Optional.of(newSection));

        adminPostService.updatePost(9L, new UpdatePostRequest("Título", "Contenido", "parcial-1"));

        assertThat(draft.getSection().getSlug()).isEqualTo("parcial-1");
        assertThat(question.getSection().getSlug()).isEqualTo("taller-1");
    }

    @Test
    void allowsEmptyTitleAndContentDuringDraftUpdate() {
        StudentQuestion question = question();
        Post draft = draftPost(question);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(question.getSection()));

        AdminPostResponse response = adminPostService.updatePost(
                9L,
                new UpdatePostRequest("   ", "   ", "taller-1"));

        assertThat(response.title()).isEmpty();
        assertThat(response.content()).isEmpty();
    }

    @Test
    void keepsExistingTagsWhenDraftIsUpdated() {
        StudentQuestion question = question();
        Tag tag = new Tag(1L, "Cuencas", "cuencas", NOW);
        Post draft = new Post(
                9L,
                question.getSection(),
                "",
                "",
                PostStatus.DRAFT,
                NOW,
                NOW,
                null,
                new LinkedHashSet<>(Set.of(tag)),
                question);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(question.getSection()));

        adminPostService.updatePost(9L, new UpdatePostRequest("Título", "Contenido", "taller-1"));

        assertThat(draft.getTags()).containsExactly(tag);
    }

    @Test
    void updatesPublishedPostAndPreservesStatusPublishedAtAndQuestion() {
        StudentQuestion question = question();
        question.transitionTo(StudentQuestionStatus.PUBLISHED, NOW);
        Post post = new Post(
                9L,
                question.getSection(),
                "Título",
                "Contenido",
                PostStatus.PUBLISHED,
                NOW,
                NOW,
                NOW,
                Set.of(),
                question);
        Section newSection = section(2L, SectionType.PARCIAL, "Parcial 1", "parcial-1");
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));
        when(sectionRepository.findBySlugAndActiveTrue("parcial-1")).thenReturn(Optional.of(newSection));

        AdminPostResponse response = adminPostService.updatePost(
                9L,
                new UpdatePostRequest("  Nuevo título  ", "  Línea 1\nLínea 2  ", "parcial-1"));

        assertThat(response.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(response.publishedAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(response.content()).isEqualTo("Línea 1\nLínea 2");
        assertThat(question.getStatus()).isEqualTo(StudentQuestionStatus.PUBLISHED);
        assertThat(question.getSection().getSlug()).isEqualTo("taller-1");
    }

    @Test
    void rejectsBlankPublishedPostUpdate() {
        StudentQuestion question = question();
        Post post = new Post(
                9L,
                question.getSection(),
                "Título",
                "Contenido",
                PostStatus.PUBLISHED,
                NOW,
                NOW,
                NOW,
                Set.of(),
                question);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(question.getSection()));

        assertThatThrownBy(() -> adminPostService.updatePost(
                9L,
                new UpdatePostRequest("  ", "Contenido", "taller-1")))
                .isInstanceOf(InvalidPostPublicationException.class);
    }

    @Test
    void updatesArchivedPostAndPreservesStatusAndPublishedAt() {
        StudentQuestion question = question();
        Post post = new Post(
                9L,
                question.getSection(),
                "Título",
                "Contenido",
                PostStatus.ARCHIVED,
                NOW,
                NOW,
                NOW,
                Set.of(),
                question);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(question.getSection()));

        AdminPostResponse response = adminPostService.updatePost(
                9L,
                new UpdatePostRequest("Archivada", "Contenido", "taller-1"));

        assertThat(response.status()).isEqualTo(PostStatus.ARCHIVED);
        assertThat(response.publishedAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void rejectsBlankArchivedPostUpdate() {
        StudentQuestion question = question();
        Post post = new Post(
                9L,
                question.getSection(),
                "Título",
                "Contenido",
                PostStatus.ARCHIVED,
                NOW,
                NOW,
                NOW,
                Set.of(),
                question);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(question.getSection()));

        assertThatThrownBy(() -> adminPostService.updatePost(
                9L,
                new UpdatePostRequest("Título", "  ", "taller-1")))
                .isInstanceOf(InvalidPostPublicationException.class);
    }

    @Test
    void throwsNotFoundWhenDraftUpdatePostDoesNotExist() {
        when(postRepository.findAdminById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPostService.updatePost(
                404L,
                new UpdatePostRequest("Título", "Contenido", "taller-1")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");
    }

    @Test
    void throwsNotFoundWhenDraftUpdateSectionDoesNotExistOrIsInactive() {
        StudentQuestion question = question();
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draftPost(question)));
        when(sectionRepository.findBySlugAndActiveTrue("inactiva")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPostService.updatePost(
                9L,
                new UpdatePostRequest("Título", "Contenido", "inactiva")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Section not found");
    }

    @Test
    void throwsNotFoundWhenAdminPostDoesNotExist() {
        when(postRepository.findAdminById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPostService.findAdminPostById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");
    }

    @Test
    void allowsDraftToExistWithBlankTitleAndContentButRejectsPublishedBlankContent() {
        StudentQuestion question = question();

        Post draft = Post.createQuestionDraft(question, NOW);

        assertThat(draft.getTitle()).isEmpty();
        assertThat(draft.getContent()).isEmpty();
        assertThatThrownBy(() -> new Post(
                9L,
                question.getSection(),
                "",
                "",
                PostStatus.PUBLISHED,
                NOW,
                NOW,
                NOW,
                Set.of(),
                question))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Post draftPost(StudentQuestion question) {
        return new Post(
                9L,
                question.getSection(),
                "",
                "",
                PostStatus.DRAFT,
                NOW,
                NOW,
                null,
                Set.of(),
                question);
    }

    private StudentQuestion withAttachment(StudentQuestion question) {
        QuestionAttachment attachment = new QuestionAttachment(
                1L,
                question,
                "hidrologia-udea/questions/private-id",
                "https://res.cloudinary.com/demo/image/upload/question.png",
                "png",
                640,
                480,
                1000L,
                NOW);
        ReflectionTestUtils.setField(question, "attachment", attachment);

        return question;
    }

    private StudentQuestion question() {
        return new StudentQuestion(
                1L,
                section(1L, SectionType.TALLER, "Taller 1", "taller-1"),
                null,
                "Pregunta original",
                StudentQuestionStatus.PENDING,
                NOW,
                NOW);
    }

    private Section section(Long id, SectionType type, String name, String slug) {
        return new Section(
                id,
                type,
                name,
                slug,
                "Morfometria de cuencas",
                id.intValue(),
                true,
                NOW);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                page,
                size,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("updatedAt"),
                        org.springframework.data.domain.Sort.Order.desc("id")));
    }
}
