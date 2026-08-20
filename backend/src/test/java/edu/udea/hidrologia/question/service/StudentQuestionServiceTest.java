package edu.udea.hidrologia.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import edu.udea.hidrologia.question.entity.StudentQuestion;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.repository.StudentQuestionRepository;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class StudentQuestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private StudentQuestionRepository studentQuestionRepository;

    @Mock
    private SectionRepository sectionRepository;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private StudentQuestionService studentQuestionService;

    @BeforeEach
    void setUp() {
        studentQuestionService = new StudentQuestionService(
                studentQuestionRepository,
                sectionRepository,
                clock);
    }

    @Test
    void createsPendingQuestionWithNickname() {
        Section section = section();
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(studentQuestionRepository.save(org.mockito.ArgumentMatchers.any(StudentQuestion.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));

        CreateStudentQuestionResponse response = studentQuestionService.createQuestion(
                new CreateStudentQuestionRequest(" taller-1 ", "  Estudiante  ", "  Como calculo el area?  "));

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
    }

    @Test
    void createsAnonymousQuestionWhenNicknameIsNull() {
        Section section = section();
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(studentQuestionRepository.save(org.mockito.ArgumentMatchers.any(StudentQuestion.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 11L));

        studentQuestionService.createQuestion(
                new CreateStudentQuestionRequest("taller-1", null, "Pregunta anonima"));

        ArgumentCaptor<StudentQuestion> captor = ArgumentCaptor.forClass(StudentQuestion.class);
        verify(studentQuestionRepository).save(captor.capture());

        assertThat(captor.getValue().getNickname()).isNull();
    }

    @Test
    void normalizesBlankNicknameToNull() {
        Section section = section();
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(studentQuestionRepository.save(org.mockito.ArgumentMatchers.any(StudentQuestion.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 12L));

        studentQuestionService.createQuestion(
                new CreateStudentQuestionRequest("taller-1", "   ", "Pregunta anonima"));

        ArgumentCaptor<StudentQuestion> captor = ArgumentCaptor.forClass(StudentQuestion.class);
        verify(studentQuestionRepository).save(captor.capture());

        assertThat(captor.getValue().getNickname()).isNull();
    }

    @Test
    void normalizesEmptyNicknameToNull() {
        Section section = section();
        when(sectionRepository.findBySlugAndActiveTrue("taller-1")).thenReturn(Optional.of(section));
        when(studentQuestionRepository.save(org.mockito.ArgumentMatchers.any(StudentQuestion.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 13L));

        studentQuestionService.createQuestion(
                new CreateStudentQuestionRequest("taller-1", "", "Pregunta anonima"));

        ArgumentCaptor<StudentQuestion> captor = ArgumentCaptor.forClass(StudentQuestion.class);
        verify(studentQuestionRepository).save(captor.capture());

        assertThat(captor.getValue().getNickname()).isNull();
    }

    @Test
    void throwsNotFoundWhenSectionDoesNotExist() {
        when(sectionRepository.findBySlugAndActiveTrue("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentQuestionService.createQuestion(
                new CreateStudentQuestionRequest("no-existe", "Estudiante", "Pregunta")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Section not found");
    }

    @Test
    void throwsNotFoundAndDoesNotSaveWhenSectionIsInactive() {
        when(sectionRepository.findBySlugAndActiveTrue("taller-inactivo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentQuestionService.createQuestion(
                new CreateStudentQuestionRequest("taller-inactivo", "Estudiante", "Pregunta")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Section not found");

        verifyNoInteractions(studentQuestionRepository);
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
