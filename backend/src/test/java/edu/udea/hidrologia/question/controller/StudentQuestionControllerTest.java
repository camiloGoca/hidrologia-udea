package edu.udea.hidrologia.question.controller;

import static org.hamcrest.Matchers.is;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.question.dto.CreateStudentQuestionRequest;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.service.StudentQuestionService;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

class StudentQuestionControllerTest {

    private StudentQuestionService studentQuestionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        studentQuestionService = Mockito.mock(StudentQuestionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StudentQuestionController(studentQuestionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsQuestionAndReturnsCreated() throws Exception {
        when(studentQuestionService.createQuestion(Mockito.any(CreateStudentQuestionRequest.class)))
                .thenReturn(new CreateStudentQuestionResponse(
                        1L,
                        StudentQuestionStatus.PENDING,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/v1/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sectionSlug": "taller-1",
                          "nickname": "Estudiante",
                          "question": "Pregunta de prueba"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.createdAt", is("2026-01-01T00:00:00Z")));
    }

    @Test
    void ignoresClientProvidedStatusAndReturnsServiceStatus() throws Exception {
        when(studentQuestionService.createQuestion(Mockito.any(CreateStudentQuestionRequest.class)))
                .thenReturn(new CreateStudentQuestionResponse(
                        2L,
                        StudentQuestionStatus.PENDING,
                        Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/v1/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sectionSlug": "taller-1",
                          "nickname": "Estudiante",
                          "question": "Pregunta de prueba",
                          "status": "PUBLISHED"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")));

        ArgumentCaptor<CreateStudentQuestionRequest> captor =
                ArgumentCaptor.forClass(CreateStudentQuestionRequest.class);
        verify(studentQuestionService).createQuestion(captor.capture());

        assertThat(captor.getValue().sectionSlug()).isEqualTo("taller-1");
        assertThat(captor.getValue().nickname()).isEqualTo("Estudiante");
        assertThat(captor.getValue().question()).isEqualTo("Pregunta de prueba");
    }

    @Test
    void returnsBadRequestForEmptyQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sectionSlug": "taller-1",
                          "nickname": "Estudiante",
                          "question": "   "
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Request validation failed")));
    }

    @Test
    void returnsBadRequestForQuestionThatIsTooLong() throws Exception {
        String longQuestion = "a".repeat(2001);

        mockMvc.perform(post("/api/v1/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sectionSlug": "taller-1",
                          "question": "%s"
                        }
                        """.formatted(longQuestion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Request validation failed")));
    }

    @Test
    void returnsBadRequestForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingSection() throws Exception {
        when(studentQuestionService.createQuestion(Mockito.any(CreateStudentQuestionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Section not found"));

        mockMvc.perform(post("/api/v1/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sectionSlug": "no-existe",
                          "question": "Pregunta de prueba"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Section not found")));
    }

    @Test
    void doesNotExposePublicQuestionGetEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/questions"))
                .andExpect(status().isMethodNotAllowed());
    }
}
