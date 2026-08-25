package edu.udea.hidrologia.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

import edu.udea.hidrologia.post.entity.Post;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.repository.PostRepository;
import edu.udea.hidrologia.question.dto.AdminQuestionDetailResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionStatusUpdateResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionsResponse;
import edu.udea.hidrologia.question.entity.QuestionAttachment;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminQuestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Mock
    private StudentQuestionRepository studentQuestionRepository;

    @Mock
    private PostRepository postRepository;

    private AdminQuestionService adminQuestionService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(UPDATED_AT, ZoneOffset.UTC);
        adminQuestionService = new AdminQuestionService(studentQuestionRepository, postRepository, clock);
        lenient().when(postRepository.findBySourceQuestionIdIn(any())).thenReturn(List.of());
        lenient().when(postRepository.findBySourceQuestionId(any())).thenReturn(Optional.empty());
    }

    @Test
    void returnsEmptyPendingQuestionsPage() {
        PageRequest pageRequest = pageRequest(0, 20);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AdminQuestionsResponse response = adminQuestionService.findPendingQuestions(0, 20);

        assertThat(response.items()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void returnsQuestionsByAllowedStatusMappedAsSummaries() {
        StudentQuestion archivedQuestion = withAttachment(question(
                2L,
                "Goca",
                "Pregunta archivada",
                StudentQuestionStatus.ARCHIVED,
                NOW.plusSeconds(60)));
        PageRequest pageRequest = pageRequest(0, 20);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.ARCHIVED, pageRequest))
                .thenReturn(new PageImpl<>(List.of(archivedQuestion), pageRequest, 1));

        AdminQuestionsResponse response = adminQuestionService.findQuestionsByStatus(
                StudentQuestionStatus.ARCHIVED,
                0,
                20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).id()).isEqualTo(2L);
        assertThat(response.items().get(0).nickname()).isEqualTo("Goca");
        assertThat(response.items().get(0).status()).isEqualTo(StudentQuestionStatus.ARCHIVED);
        assertThat(response.items().get(0).hasAttachment()).isTrue();
        assertThat(response.items().get(0).hasLinkedPost()).isFalse();
        assertThat(response.items().get(0).section().slug()).isEqualTo("taller-1");
    }

    @Test
    void mapsLinkedPostsForQuestionSummariesWithSingleAdditionalQuery() {
        StudentQuestion question = question(1L, "Goca", "Pregunta", StudentQuestionStatus.PENDING, NOW);
        Post draft = draftPost(9L, question);
        PageRequest pageRequest = pageRequest(0, 20);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(question), pageRequest, 1));
        when(postRepository.findBySourceQuestionIdIn(Set.of(1L))).thenReturn(List.of(draft));

        AdminQuestionsResponse response = adminQuestionService.findPendingQuestions(0, 20);

        assertThat(response.items().get(0).hasLinkedPost()).isTrue();
        verify(postRepository).findBySourceQuestionIdIn(Set.of(1L));
    }

    @Test
    void listsPendingArchivedAndRejectedStatusesOnly() {
        PageRequest pageRequest = pageRequest(0, 20);
        when(studentQuestionRepository.findByStatus(eq(StudentQuestionStatus.PENDING), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));
        when(studentQuestionRepository.findByStatus(eq(StudentQuestionStatus.ARCHIVED), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));
        when(studentQuestionRepository.findByStatus(eq(StudentQuestionStatus.REJECTED), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        adminQuestionService.findQuestionsByStatus(StudentQuestionStatus.PENDING, 0, 20);
        adminQuestionService.findQuestionsByStatus(StudentQuestionStatus.ARCHIVED, 0, 20);
        adminQuestionService.findQuestionsByStatus(StudentQuestionStatus.REJECTED, 0, 20);

        assertThatThrownBy(() -> adminQuestionService.findQuestionsByStatus(StudentQuestionStatus.PUBLISHED, 0, 20))
                .isInstanceOf(UnsupportedQuestionStatusFilterException.class);
    }

    @Test
    void requestsQuestionsOrderedByCreatedAtDescAndIdDesc() {
        PageRequest pageRequest = pageRequest(0, 20);
        when(studentQuestionRepository.findByStatus(eq(StudentQuestionStatus.REJECTED), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        adminQuestionService.findQuestionsByStatus(StudentQuestionStatus.REJECTED, 0, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(studentQuestionRepository).findByStatus(eq(StudentQuestionStatus.REJECTED), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    void clampsInvalidPageAndOversizedSize() {
        PageRequest pageRequest = pageRequest(0, 50);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AdminQuestionsResponse response = adminQuestionService.findQuestionsByStatus(
                StudentQuestionStatus.PENDING,
                -10,
                500);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(50);
    }

    @Test
    void usesDefaultSizeWhenSizeIsNotPositive() {
        PageRequest pageRequest = pageRequest(1, 20);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AdminQuestionsResponse response = adminQuestionService.findQuestionsByStatus(
                StudentQuestionStatus.PENDING,
                1,
                0);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    void buildsShortPreviewWithoutEllipsis() {
        PageRequest pageRequest = pageRequest(0, 20);
        StudentQuestion question = question(1L, null, "  Pregunta corta  ", StudentQuestionStatus.PENDING, NOW);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(question), pageRequest, 1));

        AdminQuestionsResponse response = adminQuestionService.findQuestionsByStatus(
                StudentQuestionStatus.PENDING,
                0,
                20);

        assertThat(response.items().get(0).questionPreview()).isEqualTo("Pregunta corta");
    }

    @Test
    void truncatesLongPreviewWithEllipsis() {
        PageRequest pageRequest = pageRequest(0, 20);
        StudentQuestion question = question(1L, null, "a".repeat(220), StudentQuestionStatus.PENDING, NOW);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(question), pageRequest, 1));

        AdminQuestionsResponse response = adminQuestionService.findQuestionsByStatus(
                StudentQuestionStatus.PENDING,
                0,
                20);

        assertThat(response.items().get(0).questionPreview()).hasSize(201);
        assertThat(response.items().get(0).questionPreview()).endsWith("\u2026");
    }

    @Test
    void returnsDetailWithoutAttachment() {
        StudentQuestion question = question(1L, null, "Pregunta completa", StudentQuestionStatus.PUBLISHED, NOW);
        when(studentQuestionRepository.findByIdWithSectionAndAttachment(1L)).thenReturn(Optional.of(question));

        AdminQuestionDetailResponse response = adminQuestionService.findQuestionById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nickname()).isNull();
        assertThat(response.question()).isEqualTo("Pregunta completa");
        assertThat(response.status()).isEqualTo(StudentQuestionStatus.PUBLISHED);
        assertThat(response.attachment()).isNull();
        assertThat(response.linkedPost()).isNull();
    }

    @Test
    void returnsDetailWithLinkedPostSummary() {
        StudentQuestion question = question(1L, "Goca", "Pregunta completa", StudentQuestionStatus.PENDING, NOW);
        when(studentQuestionRepository.findByIdWithSectionAndAttachment(1L)).thenReturn(Optional.of(question));
        when(postRepository.findBySourceQuestionId(1L)).thenReturn(Optional.of(draftPost(9L, question)));

        AdminQuestionDetailResponse response = adminQuestionService.findQuestionById(1L);

        assertThat(response.linkedPost()).isNotNull();
        assertThat(response.linkedPost().id()).isEqualTo(9L);
        assertThat(response.linkedPost().status()).isEqualTo(PostStatus.DRAFT);
        assertThat(response.linkedPost().title()).isEmpty();
    }

    @Test
    void returnsDetailWithAttachmentWithoutPublicId() {
        StudentQuestion question = withAttachment(question(1L, "Goca", "Pregunta completa", StudentQuestionStatus.PENDING, NOW));
        when(studentQuestionRepository.findByIdWithSectionAndAttachment(1L)).thenReturn(Optional.of(question));

        AdminQuestionDetailResponse response = adminQuestionService.findQuestionById(1L);

        assertThat(response.attachment()).isNotNull();
        assertThat(response.attachment().secureUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/question.png");
        assertThat(response.attachment().format()).isEqualTo("png");
        assertThat(response.attachment().width()).isEqualTo(640);
        assertThat(response.attachment().height()).isEqualTo(480);
        assertThat(response.attachment().bytes()).isEqualTo(1000L);
    }

    @Test
    void throwsNotFoundWhenQuestionDoesNotExist() {
        when(studentQuestionRepository.findByIdWithSectionAndAttachment(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminQuestionService.findQuestionById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Question not found");
    }

    @Test
    void rejectsPendingQuestion() {
        StudentQuestion question = question(1L, null, "Pregunta", StudentQuestionStatus.PENDING, NOW);
        when(studentQuestionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(studentQuestionRepository.save(question)).thenReturn(question);

        AdminQuestionStatusUpdateResponse response = adminQuestionService.rejectQuestion(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(StudentQuestionStatus.REJECTED);
        assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void archivesPendingQuestionAndKeepsAttachment() {
        StudentQuestion question = withAttachment(question(1L, null, "Pregunta", StudentQuestionStatus.PENDING, NOW));
        when(studentQuestionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(studentQuestionRepository.save(question)).thenReturn(question);

        AdminQuestionStatusUpdateResponse response = adminQuestionService.archiveQuestion(1L);

        assertThat(response.status()).isEqualTo(StudentQuestionStatus.ARCHIVED);
        assertThat(question.getAttachment()).isNotNull();
    }

    @Test
    void blocksRejectAndArchiveWhenPendingQuestionHasLinkedPost() {
        StudentQuestion question = question(1L, null, "Pregunta", StudentQuestionStatus.PENDING, NOW);
        when(studentQuestionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(postRepository.existsBySourceQuestionId(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminQuestionService.rejectQuestion(1L))
                .isInstanceOf(QuestionDraftConflictException.class);
        assertThatThrownBy(() -> adminQuestionService.archiveQuestion(1L))
                .isInstanceOf(QuestionDraftConflictException.class);
    }

    @Test
    void reopensRejectedQuestion() {
        StudentQuestion question = question(1L, null, "Pregunta", StudentQuestionStatus.REJECTED, NOW);
        when(studentQuestionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(studentQuestionRepository.save(question)).thenReturn(question);

        AdminQuestionStatusUpdateResponse response = adminQuestionService.reopenQuestion(1L);

        assertThat(response.status()).isEqualTo(StudentQuestionStatus.PENDING);
    }

    @Test
    void reopensArchivedQuestion() {
        StudentQuestion question = question(1L, null, "Pregunta", StudentQuestionStatus.ARCHIVED, NOW);
        when(studentQuestionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(studentQuestionRepository.save(question)).thenReturn(question);

        AdminQuestionStatusUpdateResponse response = adminQuestionService.reopenQuestion(1L);

        assertThat(response.status()).isEqualTo(StudentQuestionStatus.PENDING);
    }

    @Test
    void rejectsInvalidTransitionsWithConflictException() {
        assertInvalidTransition(StudentQuestionStatus.REJECTED, "reject");
        assertInvalidTransition(StudentQuestionStatus.ARCHIVED, "archive");
        assertInvalidTransition(StudentQuestionStatus.REJECTED, "archive");
        assertInvalidTransition(StudentQuestionStatus.ARCHIVED, "reject");
        assertInvalidTransition(StudentQuestionStatus.PUBLISHED, "reject");
        assertInvalidTransition(StudentQuestionStatus.PUBLISHED, "archive");
        assertInvalidTransition(StudentQuestionStatus.PUBLISHED, "reopen");
    }

    @Test
    void throwsNotFoundWhenTransitionQuestionDoesNotExist() {
        when(studentQuestionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminQuestionService.rejectQuestion(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Question not found");
    }

    private void assertInvalidTransition(StudentQuestionStatus currentStatus, String action) {
        StudentQuestion question = question(1L, null, "Pregunta", currentStatus, NOW);
        when(studentQuestionRepository.findById(1L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> runAction(action))
                .isInstanceOf(InvalidQuestionStatusTransitionException.class);
    }

    private void runAction(String action) {
        switch (action) {
            case "reject" -> adminQuestionService.rejectQuestion(1L);
            case "archive" -> adminQuestionService.archiveQuestion(1L);
            case "reopen" -> adminQuestionService.reopenQuestion(1L);
            default -> throw new IllegalArgumentException("Unknown action");
        }
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

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                page,
                size,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id")));
    }

    private StudentQuestion question(
            Long id,
            String nickname,
            String content,
            StudentQuestionStatus status,
            Instant createdAt) {
        return new StudentQuestion(
                id,
                section(),
                nickname,
                content,
                status,
                createdAt,
                createdAt.plusSeconds(30));
    }

    private Post draftPost(Long id, StudentQuestion question) {
        return new Post(
                id,
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
}
