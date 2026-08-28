package edu.udea.hidrologia.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.udea.hidrologia.question.dto.CreateStudentQuestionRequest;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.entity.QuestionAttachment;
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.QuestionAttachmentRepository;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.shared.storage.StoredImage;

@ExtendWith(MockitoExtension.class)
class StudentQuestionPersistenceServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private StudentQuestionRepository studentQuestionRepository;

    @Mock
    private QuestionAttachmentRepository questionAttachmentRepository;

    @Mock
    private SectionRepository sectionRepository;

    private StudentQuestionPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new StudentQuestionPersistenceService(
                studentQuestionRepository,
                questionAttachmentRepository,
                sectionRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsPendingQuestionWithNickname() {
        Section section = section();
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(studentQuestionRepository.save(any(StudentQuestion.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));

        CreateStudentQuestionResponse response = persistenceService.persist(
                new CreateStudentQuestionRequest(" taller-1 ", "  Estudiante  ", "  Como calculo el area?  ", null),
                null);

        ArgumentCaptor<StudentQuestion> captor = ArgumentCaptor.forClass(StudentQuestion.class);
        verify(studentQuestionRepository).save(captor.capture());
        StudentQuestion saved = captor.getValue();

        assertThat(saved.getSection()).isSameAs(section);
        assertThat(saved.getNickname()).isEqualTo("Estudiante");
        assertThat(saved.getQuestion()).isEqualTo("Como calculo el area?");
        assertThat(saved.getStatus()).isEqualTo(StudentQuestionStatus.PENDING);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(StudentQuestionStatus.PENDING);
        assertThat(response.createdAt()).isEqualTo(NOW);
        verifyNoInteractions(questionAttachmentRepository);
    }

    @Test
    void createsAnonymousQuestionWhenNicknameIsNull() {
        Section section = section();
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(studentQuestionRepository.save(any(StudentQuestion.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 11L));

        persistenceService.persist(new CreateStudentQuestionRequest("taller-1", null, "Pregunta anonima", null), null);

        ArgumentCaptor<StudentQuestion> captor = ArgumentCaptor.forClass(StudentQuestion.class);
        verify(studentQuestionRepository).save(captor.capture());

        assertThat(captor.getValue().getNickname()).isNull();
    }

    @Test
    void normalizesBlankNicknameToNull() {
        Section section = section();
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(studentQuestionRepository.save(any(StudentQuestion.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 12L));

        persistenceService.persist(new CreateStudentQuestionRequest("taller-1", "   ", "Pregunta anonima", null), null);

        ArgumentCaptor<StudentQuestion> captor = ArgumentCaptor.forClass(StudentQuestion.class);
        verify(studentQuestionRepository).save(captor.capture());

        assertThat(captor.getValue().getNickname()).isNull();
    }

    @Test
    void persistsQuestionAttachmentWhenStoredImageExists() {
        Section section = section();
        StoredImage image = new StoredImage(
                "hidrologia-udea/questions/question-1",
                "https://res.cloudinary.com/demo/image/upload/question-1.png",
                "png",
                2,
                2,
                79);
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(studentQuestionRepository.save(any(StudentQuestion.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 13L));

        persistenceService.persist(new CreateStudentQuestionRequest("taller-1", "", "Pregunta con imagen", null), image);

        ArgumentCaptor<QuestionAttachment> captor = ArgumentCaptor.forClass(QuestionAttachment.class);
        verify(questionAttachmentRepository).save(captor.capture());
        QuestionAttachment attachment = captor.getValue();

        assertThat(attachment.getQuestion().getId()).isEqualTo(13L);
        assertThat(attachment.getPublicId()).isEqualTo(image.publicId());
        assertThat(attachment.getSecureUrl()).isEqualTo(image.secureUrl());
        assertThat(attachment.getFormat()).isEqualTo("png");
        assertThat(attachment.getWidth()).isEqualTo(2);
        assertThat(attachment.getHeight()).isEqualTo(2);
        assertThat(attachment.getBytes()).isEqualTo(79);
        assertThat(attachment.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void throwsNotFoundAndDoesNotSaveWhenSectionIsInactiveOrMissing() {
        when(sectionRepository.findBySlugAndActiveTrue("taller-inactivo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> persistenceService.persist(
                new CreateStudentQuestionRequest("taller-inactivo", "Estudiante", "Pregunta", null),
                null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Section not found");

        verifyNoInteractions(studentQuestionRepository, questionAttachmentRepository);
    }

    private StudentQuestion withId(StudentQuestion question, Long id) {
        return new StudentQuestion(
                id,
                question.getSection(),
                question.getNickname(),
                question.getQuestion(),
                question.getStatus(),
                question.getCreatedAt(),
                question.getUpdatedAt());
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
