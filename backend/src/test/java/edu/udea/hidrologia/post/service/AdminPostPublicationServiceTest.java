package edu.udea.hidrologia.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminPostPublicationServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Mock
    private PostRepository postRepository;

    @Mock
    private SectionRepository sectionRepository;

    private AdminPostPublicationService publicationService;

    @BeforeEach
    void setUp() {
        AdminPostService adminPostService = new AdminPostService(
                postRepository,
                sectionRepository,
                Clock.fixed(PUBLISHED_AT, ZoneOffset.UTC));
        publicationService = new AdminPostPublicationService(
                postRepository,
                adminPostService,
                Clock.fixed(PUBLISHED_AT, ZoneOffset.UTC));
    }

    @Test
    void publishesValidDraftAndMarksSourceQuestionPublished() {
        StudentQuestion question = question(StudentQuestionStatus.PENDING);
        Post draft = draftPost(question, "Título", "Contenido");
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));

        AdminPostResponse response = publicationService.publishDraft(9L);

        assertThat(draft.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(draft.getPublishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(draft.getUpdatedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(question.getStatus()).isEqualTo(StudentQuestionStatus.PUBLISHED);
        assertThat(question.getUpdatedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(response.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(response.publishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(response.title()).isEqualTo("Título");
        assertThat(response.content()).isEqualTo("Contenido");
    }

    @Test
    void publishMethodIsTransactional() throws NoSuchMethodException {
        Method method = AdminPostPublicationService.class.getMethod("publishDraft", Long.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }

    @Test
    void publishesDraftWithoutSourceQuestion() {
        Post draft = new Post(
                9L,
                section(),
                "Título",
                "Contenido",
                PostStatus.DRAFT,
                CREATED_AT,
                CREATED_AT,
                null,
                Set.of(),
                null);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));

        AdminPostResponse response = publicationService.publishDraft(9L);

        assertThat(response.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(response.sourceQuestion()).isNull();
    }

    @Test
    void archivesPublishedPostWithoutChangingPublishedAtOrSourceQuestion() {
        StudentQuestion question = question(StudentQuestionStatus.PUBLISHED);
        Post post = new Post(
                9L,
                question.getSection(),
                "Título",
                "Contenido",
                PostStatus.PUBLISHED,
                CREATED_AT,
                CREATED_AT,
                CREATED_AT,
                Set.of(),
                question);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));

        AdminPostResponse response = publicationService.archivePost(9L);

        assertThat(post.getStatus()).isEqualTo(PostStatus.ARCHIVED);
        assertThat(post.getPublishedAt()).isEqualTo(CREATED_AT);
        assertThat(post.getUpdatedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(question.getStatus()).isEqualTo(StudentQuestionStatus.PUBLISHED);
        assertThat(response.status()).isEqualTo(PostStatus.ARCHIVED);
        assertThat(response.publishedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void rejectsArchiveWhenPostIsDraftOrArchived() {
        for (PostStatus status : new PostStatus[] { PostStatus.DRAFT, PostStatus.ARCHIVED }) {
            Post post = new Post(
                    9L,
                    section(),
                    status == PostStatus.DRAFT ? "" : "Título",
                    status == PostStatus.DRAFT ? "" : "Contenido",
                    status,
                    CREATED_AT,
                    CREATED_AT,
                    status == PostStatus.ARCHIVED ? CREATED_AT : null,
                    Set.of(),
                    null);
            when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> publicationService.archivePost(9L))
                    .isInstanceOf(PostStateConflictException.class)
                    .hasMessage("Only published posts can be archived");
        }
    }

    @Test
    void throwsNotFoundWhenArchivingMissingPost() {
        when(postRepository.findAdminById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicationService.archivePost(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");
    }

    @Test
    void restoresArchivedPostWithoutChangingPublishedAtOrSourceQuestion() {
        StudentQuestion question = question(StudentQuestionStatus.PUBLISHED);
        Post post = new Post(
                9L,
                question.getSection(),
                "Título",
                "Contenido",
                PostStatus.ARCHIVED,
                CREATED_AT,
                CREATED_AT,
                CREATED_AT,
                Set.of(),
                question);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));

        AdminPostResponse response = publicationService.restorePost(9L);

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isEqualTo(CREATED_AT);
        assertThat(post.getUpdatedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(question.getStatus()).isEqualTo(StudentQuestionStatus.PUBLISHED);
        assertThat(response.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(response.publishedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void rejectsRestoreWhenPostIsDraftOrPublished() {
        for (PostStatus status : new PostStatus[] { PostStatus.DRAFT, PostStatus.PUBLISHED }) {
            Post post = new Post(
                    9L,
                    section(),
                    status == PostStatus.DRAFT ? "" : "Título",
                    status == PostStatus.DRAFT ? "" : "Contenido",
                    status,
                    CREATED_AT,
                    CREATED_AT,
                    status == PostStatus.PUBLISHED ? CREATED_AT : null,
                    Set.of(),
                    null);
            when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> publicationService.restorePost(9L))
                    .isInstanceOf(PostStateConflictException.class)
                    .hasMessage("Only archived posts can be restored");
        }
    }

    @Test
    void rejectsRestoreWhenArchivedPostHasInvalidContent() {
        Post post = new Post(
                9L,
                section(),
                "   ",
                "Contenido",
                PostStatus.DRAFT,
                CREATED_AT,
                CREATED_AT,
                null,
                Set.of(),
                null);
        post.update("   ", "Contenido", section(), CREATED_AT);
        org.springframework.test.util.ReflectionTestUtils.setField(post, "status", PostStatus.ARCHIVED);
        org.springframework.test.util.ReflectionTestUtils.setField(post, "publishedAt", CREATED_AT);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> publicationService.restorePost(9L))
                .isInstanceOf(InvalidPostPublicationException.class);
    }

    @Test
    void rejectsRestoreWhenArchivedPostSectionIsInactive() {
        Section inactiveSection = new Section(
                2L,
                SectionType.TALLER,
                "Taller inactivo",
                "taller-inactivo",
                "Inactiva",
                2,
                false,
                CREATED_AT);
        Post post = new Post(
                9L,
                inactiveSection,
                "Título",
                "Contenido",
                PostStatus.ARCHIVED,
                CREATED_AT,
                CREATED_AT,
                CREATED_AT,
                Set.of(),
                null);
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> publicationService.restorePost(9L))
                .isInstanceOf(InvalidPostPublicationException.class)
                .hasMessage("La publicación debe tener una sección activa.");
    }

    @Test
    void throwsNotFoundWhenRestoringMissingPost() {
        when(postRepository.findAdminById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicationService.restorePost(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");
    }

    @Test
    void rejectsPublishWhenTitleOrContentIsBlank() {
        StudentQuestion question = question(StudentQuestionStatus.PENDING);
        Post draft = draftPost(question, "   ", "Contenido");
        when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> publicationService.publishDraft(9L))
                .isInstanceOf(InvalidPostPublicationException.class)
                .hasMessage("La publicación necesita título y contenido antes de publicarse.");
    }

    @Test
    void rejectsPublishWhenPostIsPublishedOrArchived() {
        StudentQuestion question = question(StudentQuestionStatus.PENDING);
        for (PostStatus status : new PostStatus[] { PostStatus.PUBLISHED, PostStatus.ARCHIVED }) {
            Post post = new Post(
                    9L,
                    question.getSection(),
                    "Título",
                    "Contenido",
                    status,
                    CREATED_AT,
                    CREATED_AT,
                    status == PostStatus.PUBLISHED ? CREATED_AT : null,
                    Set.of(),
                    question);
            when(postRepository.findAdminById(9L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> publicationService.publishDraft(9L))
                    .isInstanceOf(PostStateConflictException.class);
        }
    }

    @Test
    void rejectsPublishWhenSourceQuestionIsNoLongerPending() {
        for (StudentQuestionStatus status : new StudentQuestionStatus[] {
                StudentQuestionStatus.REJECTED,
                StudentQuestionStatus.ARCHIVED,
                StudentQuestionStatus.PUBLISHED
        }) {
            StudentQuestion question = question(status);
            Post draft = draftPost(question, "Título", "Contenido");
            when(postRepository.findAdminById(9L)).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> publicationService.publishDraft(9L))
                    .isInstanceOf(PostStateConflictException.class)
                    .hasMessage("Source question must be pending before publication");
        }
    }

    @Test
    void throwsNotFoundWhenPostDoesNotExist() {
        when(postRepository.findAdminById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicationService.publishDraft(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found");

        verify(postRepository).findAdminById(404L);
    }

    private Post draftPost(StudentQuestion question, String title, String content) {
        return new Post(
                9L,
                question.getSection(),
                title,
                content,
                PostStatus.DRAFT,
                CREATED_AT,
                CREATED_AT,
                null,
                Set.of(),
                question);
    }

    private StudentQuestion question(StudentQuestionStatus status) {
        return new StudentQuestion(
                1L,
                section(),
                "Goca",
                "Pregunta original",
                status,
                CREATED_AT,
                CREATED_AT);
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
}
