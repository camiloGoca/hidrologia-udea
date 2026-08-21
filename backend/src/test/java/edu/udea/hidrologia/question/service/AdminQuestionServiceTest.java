package edu.udea.hidrologia.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

import edu.udea.hidrologia.question.dto.AdminPendingQuestionsResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionDetailResponse;
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

    @Mock
    private StudentQuestionRepository studentQuestionRepository;

    private AdminQuestionService adminQuestionService;

    @BeforeEach
    void setUp() {
        adminQuestionService = new AdminQuestionService(studentQuestionRepository);
    }

    @Test
    void returnsEmptyPendingQuestionsPage() {
        PageRequest pageRequest = pageRequest(0, 20);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AdminPendingQuestionsResponse response = adminQuestionService.findPendingQuestions(0, 20);

        assertThat(response.items()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void returnsOnlyPendingQuestionsMappedAsSummaries() {
        StudentQuestion anonymousQuestion = question(1L, null, "Pregunta corta", StudentQuestionStatus.PENDING, NOW);
        StudentQuestion questionWithAttachment = withAttachment(question(
                2L,
                "Goca",
                "Pregunta con imagen",
                StudentQuestionStatus.PENDING,
                NOW.plusSeconds(60)));
        PageRequest pageRequest = pageRequest(0, 20);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(questionWithAttachment, anonymousQuestion), pageRequest, 2));

        AdminPendingQuestionsResponse response = adminQuestionService.findPendingQuestions(0, 20);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).id()).isEqualTo(2L);
        assertThat(response.items().get(0).nickname()).isEqualTo("Goca");
        assertThat(response.items().get(0).hasAttachment()).isTrue();
        assertThat(response.items().get(0).section().slug()).isEqualTo("taller-1");
        assertThat(response.items().get(1).nickname()).isNull();
        assertThat(response.items().get(1).hasAttachment()).isFalse();
    }

    @Test
    void requestsPendingQuestionsOrderedByCreatedAtDescAndIdDesc() {
        PageRequest pageRequest = pageRequest(0, 20);
        when(studentQuestionRepository.findByStatus(eq(StudentQuestionStatus.PENDING), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        adminQuestionService.findPendingQuestions(0, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(studentQuestionRepository).findByStatus(eq(StudentQuestionStatus.PENDING), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    void clampsInvalidPageAndOversizedSize() {
        PageRequest pageRequest = pageRequest(0, 50);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AdminPendingQuestionsResponse response = adminQuestionService.findPendingQuestions(-10, 500);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(50);
    }

    @Test
    void usesDefaultSizeWhenSizeIsNotPositive() {
        PageRequest pageRequest = pageRequest(1, 20);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        AdminPendingQuestionsResponse response = adminQuestionService.findPendingQuestions(1, 0);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    void buildsShortPreviewWithoutEllipsis() {
        PageRequest pageRequest = pageRequest(0, 20);
        StudentQuestion question = question(1L, null, "  Pregunta corta  ", StudentQuestionStatus.PENDING, NOW);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(question), pageRequest, 1));

        AdminPendingQuestionsResponse response = adminQuestionService.findPendingQuestions(0, 20);

        assertThat(response.items().get(0).questionPreview()).isEqualTo("Pregunta corta");
    }

    @Test
    void truncatesLongPreviewWithEllipsis() {
        PageRequest pageRequest = pageRequest(0, 20);
        StudentQuestion question = question(1L, null, "a".repeat(220), StudentQuestionStatus.PENDING, NOW);
        when(studentQuestionRepository.findByStatus(StudentQuestionStatus.PENDING, pageRequest))
                .thenReturn(new PageImpl<>(List.of(question), pageRequest, 1));

        AdminPendingQuestionsResponse response = adminQuestionService.findPendingQuestions(0, 20);

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
