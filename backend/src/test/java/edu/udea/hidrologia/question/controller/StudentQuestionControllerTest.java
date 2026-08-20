package edu.udea.hidrologia.question.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import edu.udea.hidrologia.question.dto.CreateStudentQuestionRequest;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.service.StudentQuestionService;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.shared.storage.ImageStorageUnavailableException;
import edu.udea.hidrologia.shared.storage.ImageTooLargeException;

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
    void createsQuestionWithoutImageAndReturnsCreated() throws Exception {
        when(studentQuestionService.createQuestion(any(CreateStudentQuestionRequest.class), isNull()))
                .thenReturn(response(1L));

        mockMvc.perform(multipart("/api/v1/questions")
                .file(dataPart("""
                        {
                          "sectionSlug": "taller-1",
                          "nickname": "Estudiante",
                          "question": "Pregunta de prueba"
                        }
                        """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.createdAt", is("2026-01-01T00:00:00Z")));

        ArgumentCaptor<CreateStudentQuestionRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateStudentQuestionRequest.class);
        verify(studentQuestionService).createQuestion(requestCaptor.capture(), isNull());

        assertThat(requestCaptor.getValue().sectionSlug()).isEqualTo("taller-1");
        assertThat(requestCaptor.getValue().nickname()).isEqualTo("Estudiante");
        assertThat(requestCaptor.getValue().question()).isEqualTo("Pregunta de prueba");
    }

    @Test
    void createsQuestionWithImageAndReturnsCreated() throws Exception {
        when(studentQuestionService.createQuestion(any(CreateStudentQuestionRequest.class), any(MultipartFile.class)))
                .thenReturn(response(2L));

        MockMultipartFile image = new MockMultipartFile("image", "image.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/v1/questions")
                .file(dataPart("""
                        {
                          "sectionSlug": "taller-1",
                          "question": "Pregunta con imagen"
                        }
                        """))
                .file(image))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(2)));
    }

    @Test
    void ignoresClientProvidedStatusAndReturnsServiceStatus() throws Exception {
        when(studentQuestionService.createQuestion(any(CreateStudentQuestionRequest.class), isNull()))
                .thenReturn(response(3L));

        mockMvc.perform(multipart("/api/v1/questions")
                .file(dataPart("""
                        {
                          "sectionSlug": "taller-1",
                          "nickname": "Estudiante",
                          "question": "Pregunta de prueba",
                          "status": "PUBLISHED"
                        }
                        """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")));

        ArgumentCaptor<CreateStudentQuestionRequest> captor =
                ArgumentCaptor.forClass(CreateStudentQuestionRequest.class);
        verify(studentQuestionService).createQuestion(captor.capture(), isNull());

        assertThat(captor.getValue().sectionSlug()).isEqualTo("taller-1");
        assertThat(captor.getValue().nickname()).isEqualTo("Estudiante");
        assertThat(captor.getValue().question()).isEqualTo("Pregunta de prueba");
    }

    @Test
    void returnsBadRequestForEmptyQuestion() throws Exception {
        mockMvc.perform(multipart("/api/v1/questions")
                .file(dataPart("""
                        {
                          "sectionSlug": "taller-1",
                          "nickname": "Estudiante",
                          "question": "   "
                        }
                        """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Request validation failed")));
    }

    @Test
    void returnsBadRequestForQuestionThatIsTooLong() throws Exception {
        String longQuestion = "a".repeat(2001);

        mockMvc.perform(multipart("/api/v1/questions")
                .file(dataPart("""
                        {
                          "sectionSlug": "taller-1",
                          "question": "%s"
                        }
                        """.formatted(longQuestion))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Request validation failed")));
    }

    @Test
    void returnsBadRequestForInvalidRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/questions")
                .file(dataPart("{}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingSection() throws Exception {
        when(studentQuestionService.createQuestion(any(CreateStudentQuestionRequest.class), isNull()))
                .thenThrow(new ResourceNotFoundException("Section not found"));

        mockMvc.perform(multipart("/api/v1/questions")
                .file(dataPart("""
                        {
                          "sectionSlug": "no-existe",
                          "question": "Pregunta de prueba"
                        }
                        """)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Section not found")));
    }

    @Test
    void returnsServiceUnavailableWhenImageStorageIsDisabled() throws Exception {
        when(studentQuestionService.createQuestion(any(CreateStudentQuestionRequest.class), any(MultipartFile.class)))
                .thenThrow(new ImageStorageUnavailableException("Image uploads are temporarily unavailable"));

        mockMvc.perform(multipart("/api/v1/questions")
                .file(dataPart("""
                        {
                          "sectionSlug": "taller-1",
                          "question": "Pregunta de prueba"
                        }
                        """))
                .file(new MockMultipartFile("image", "image.png", "image/png", new byte[] {1, 2, 3})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", is("Image uploads are temporarily unavailable")));
    }

    @Test
    void returnsContentTooLargeWhenImageExceedsLimit() throws Exception {
        when(studentQuestionService.createQuestion(any(CreateStudentQuestionRequest.class), any(MultipartFile.class)))
                .thenThrow(new ImageTooLargeException("The uploaded image exceeds the maximum size"));

        mockMvc.perform(multipart("/api/v1/questions")
                .file(dataPart("""
                        {
                          "sectionSlug": "taller-1",
                          "question": "Pregunta de prueba"
                        }
                        """))
                .file(new MockMultipartFile("image", "image.png", "image/png", new byte[] {1, 2, 3})))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.message", is("The uploaded image exceeds the maximum size")));
    }

    @Test
    void rejectsOldJsonContract() throws Exception {
        mockMvc.perform(post("/api/v1/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sectionSlug": "taller-1",
                          "question": "Pregunta de prueba"
                        }
                        """))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void doesNotExposePublicQuestionGetEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/questions"))
                .andExpect(status().isMethodNotAllowed());
    }

    private MockMultipartFile dataPart(String json) {
        return new MockMultipartFile(
                "data",
                "data.json",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private CreateStudentQuestionResponse response(Long id) {
        return new CreateStudentQuestionResponse(
                id,
                StudentQuestionStatus.PENDING,
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
