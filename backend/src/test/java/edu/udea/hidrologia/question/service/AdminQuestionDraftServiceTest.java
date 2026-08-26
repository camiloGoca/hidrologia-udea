package edu.udea.hidrologia.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.post.service.AdminPostService;
import edu.udea.hidrologia.question.entity.QuestionAttachment;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminQuestionDraftServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-02T00:00:00Z");
    @Mock
    private StudentQuestionRepository studentQuestionRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private AdminPostService adminPostService;

    private AdminQuestionDraftService adminQuestionDraftService;

    @BeforeEach
    void setUp() {
        adminQuestionDraftService = new AdminQuestionDraftService(
                studentQuestionRepository,
                postRepository,
                adminPostService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsDraftFromPendingQuestionWithoutPublishingIt() {
        StudentQuestion question = withAttachment(question(StudentQuestionStatus.PENDING));
        AdminPostResponse draftResponse = draftResponse();
        when(studentQuestionRepository.findByIdWithSectionAndAttachment(1L)).thenReturn(Optional.of(question));
        when(postRepository.existsBySourceQuestionId(1L)).thenReturn(false);
        when(postRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Post.class)))
                .thenAnswer(invocation -> {
                    Post draft = invocation.getArgument(0);
                    ReflectionTestUtils.setField(draft, "id", 9L);
                    return draft;
                });
        when(adminPostService.toResponse(org.mockito.ArgumentMatchers.any(Post.class))).thenReturn(draftResponse);

        AdminPostResponse response = adminQuestionDraftService.createDraft(1L);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).saveAndFlush(postCaptor.capture());
        Post savedPost = postCaptor.getValue();
        assertThat(savedPost.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(savedPost.getSourceQuestion()).isSameAs(question);
        assertThat(savedPost.getSection()).isSameAs(question.getSection());
        assertThat(savedPost.getTitle()).isEmpty();
        assertThat(savedPost.getContent()).isEmpty();
        assertThat(savedPost.getPublishedAt()).isNull();
        assertThat(question.getStatus()).isEqualTo(StudentQuestionStatus.PENDING);
        assertThat(question.getAttachment()).isNotNull();
        assertThat(response).isSameAs(draftResponse);
    }

    @Test
    void rejectsDraftCreationForNonPendingQuestions() {
        for (StudentQuestionStatus status : new StudentQuestionStatus[] {
                StudentQuestionStatus.ARCHIVED,
                StudentQuestionStatus.REJECTED,
                StudentQuestionStatus.PUBLISHED }) {
            StudentQuestion question = question(status);
            when(studentQuestionRepository.findByIdWithSectionAndAttachment(1L)).thenReturn(Optional.of(question));

            assertThatThrownBy(() -> adminQuestionDraftService.createDraft(1L))
                    .isInstanceOf(InvalidQuestionStatusTransitionException.class);
        }
    }

    @Test
    void rejectsDraftCreationWhenQuestionAlreadyHasLinkedPost() {
        StudentQuestion question = question(StudentQuestionStatus.PENDING);
        when(studentQuestionRepository.findByIdWithSectionAndAttachment(1L)).thenReturn(Optional.of(question));
        when(postRepository.existsBySourceQuestionId(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminQuestionDraftService.createDraft(1L))
                .isInstanceOf(QuestionDraftConflictException.class);

        verify(postRepository).existsBySourceQuestionId(1L);
    }

    @Test
    void translatesUniqueRaceToConflict() {
        StudentQuestion question = question(StudentQuestionStatus.PENDING);
        when(studentQuestionRepository.findByIdWithSectionAndAttachment(1L)).thenReturn(Optional.of(question));
        when(postRepository.existsBySourceQuestionId(1L)).thenReturn(false);
        when(postRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Post.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> adminQuestionDraftService.createDraft(1L))
                .isInstanceOf(QuestionDraftConflictException.class);
    }

    @Test
    void throwsNotFoundWhenQuestionDoesNotExistOnCreate() {
        when(studentQuestionRepository.findByIdWithSectionAndAttachment(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminQuestionDraftService.createDraft(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Question not found");
    }

    @Test
    void discardsDraftWithoutDeletingQuestionOrAttachment() {
        StudentQuestion question = withAttachment(question(StudentQuestionStatus.PENDING));
        Post draft = Post.createQuestionDraft(question, NOW);
        when(studentQuestionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(postRepository.findBySourceQuestionId(1L)).thenReturn(Optional.of(draft));

        adminQuestionDraftService.discardDraft(1L);

        verify(postRepository).delete(draft);
        assertThat(question.getStatus()).isEqualTo(StudentQuestionStatus.PENDING);
        assertThat(question.getAttachment()).isNotNull();
    }

    @Test
    void throwsNotFoundWhenQuestionDoesNotExistOnDiscard() {
        when(studentQuestionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminQuestionDraftService.discardDraft(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Question not found");

        verifyNoInteractions(postRepository);
    }

    @Test
    void throwsNotFoundWhenQuestionHasNoDraft() {
        StudentQuestion question = question(StudentQuestionStatus.PENDING);
        when(studentQuestionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(postRepository.findBySourceQuestionId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminQuestionDraftService.discardDraft(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Question draft not found");
    }

    @Test
    void rejectsDiscardWhenLinkedPostIsNotDraft() {
        StudentQuestion question = question(StudentQuestionStatus.PENDING);
        Post publishedPost = new Post(
                9L,
                question.getSection(),
                "Titulo",
                "Contenido",
                PostStatus.PUBLISHED,
                NOW,
                NOW,
                NOW,
                java.util.Set.of(),
                question);
        when(studentQuestionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(postRepository.findBySourceQuestionId(1L)).thenReturn(Optional.of(publishedPost));

        assertThatThrownBy(() -> adminQuestionDraftService.discardDraft(1L))
                .isInstanceOf(QuestionDraftConflictException.class);
    }

    private AdminPostResponse draftResponse() {
        return new AdminPostResponse(
                9L,
                "",
                "",
                contentDocument(""),
                PostStatus.DRAFT,
                1L,
                null,
                null,
                java.util.List.of(),
                NOW,
                NOW,
                null);
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

    private StudentQuestion question(StudentQuestionStatus status) {
        return new StudentQuestion(1L, section(), "Goca", "Pregunta", status, NOW, NOW);
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
                NOW);
    }

    private Map<String, Object> contentDocument(String content) {
        return Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", content)))));
    }
}
