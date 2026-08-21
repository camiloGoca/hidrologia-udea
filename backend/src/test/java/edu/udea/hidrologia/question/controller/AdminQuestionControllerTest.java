package edu.udea.hidrologia.question.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.question.dto.AdminPendingQuestionsResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionAttachmentResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionDetailResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionSectionResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionSummaryResponse;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.service.AdminQuestionService;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

class AdminQuestionControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private AdminQuestionService adminQuestionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminQuestionService = Mockito.mock(AdminQuestionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminQuestionController(adminQuestionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPendingQuestions() throws Exception {
        when(adminQuestionService.findPendingQuestions(2, 10))
                .thenReturn(new AdminPendingQuestionsResponse(
                        List.of(new AdminQuestionSummaryResponse(
                                1L,
                                null,
                                section(),
                                "Pregunta",
                                true,
                                NOW)),
                        2,
                        10,
                        21,
                        3));

        mockMvc.perform(get("/api/v1/admin/questions/pending?page=2&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(1)))
                .andExpect(jsonPath("$.items[0].nickname").doesNotExist())
                .andExpect(jsonPath("$.items[0].questionPreview", is("Pregunta")))
                .andExpect(jsonPath("$.items[0].hasAttachment", is(true)))
                .andExpect(jsonPath("$.items[0].section.slug", is("taller-1")))
                .andExpect(jsonPath("$.page", is(2)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalElements", is(21)))
                .andExpect(jsonPath("$.totalPages", is(3)));

        verify(adminQuestionService).findPendingQuestions(2, 10);
    }

    @Test
    void usesDefaultPaginationParameters() throws Exception {
        when(adminQuestionService.findPendingQuestions(0, 20))
                .thenReturn(new AdminPendingQuestionsResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/admin/questions/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(adminQuestionService).findPendingQuestions(0, 20);
    }

    @Test
    void returnsQuestionDetailWithAttachment() throws Exception {
        when(adminQuestionService.findQuestionById(1L))
                .thenReturn(new AdminQuestionDetailResponse(
                        1L,
                        "Goca",
                        "Pregunta completa",
                        StudentQuestionStatus.PENDING,
                        NOW,
                        NOW.plusSeconds(30),
                        section(),
                        new AdminQuestionAttachmentResponse(
                                "https://res.cloudinary.com/demo/image/upload/question.png",
                                "png",
                                640,
                                480,
                                1000L)));

        mockMvc.perform(get("/api/v1/admin/questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nickname", is("Goca")))
                .andExpect(jsonPath("$.question", is("Pregunta completa")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.attachment.secureUrl", is("https://res.cloudinary.com/demo/image/upload/question.png")))
                .andExpect(jsonPath("$.attachment.publicId").doesNotExist());
    }

    @Test
    void returnsNotFoundForMissingQuestion() throws Exception {
        when(adminQuestionService.findQuestionById(404L)).thenThrow(new ResourceNotFoundException("Question not found"));

        mockMvc.perform(get("/api/v1/admin/questions/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Question not found")));
    }

    private AdminQuestionSectionResponse section() {
        return new AdminQuestionSectionResponse(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas");
    }
}
