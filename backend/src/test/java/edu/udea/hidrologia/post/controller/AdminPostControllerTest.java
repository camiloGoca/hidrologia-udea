package edu.udea.hidrologia.post.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostSourceQuestionResponse;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.service.AdminPostService;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

class AdminPostControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private AdminPostService adminPostService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminPostService = Mockito.mock(AdminPostService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPostController(adminPostService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAdminPostDetail() throws Exception {
        when(adminPostService.findAdminPostById(9L))
                .thenReturn(new AdminPostResponse(
                        9L,
                        "",
                        "",
                        PostStatus.DRAFT,
                        1L,
                        section(),
                        sourceQuestion(),
                        NOW,
                        NOW));

        mockMvc.perform(get("/api/v1/admin/posts/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(9)))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.sourceQuestionId", is(1)))
                .andExpect(jsonPath("$.sourceQuestion.question", is("Pregunta original")))
                .andExpect(jsonPath("$.sourceQuestion.hasAttachment", is(true)))
                .andExpect(jsonPath("$.sourceQuestion.publicId").doesNotExist());
    }

    @Test
    void returnsNotFoundForMissingAdminPost() throws Exception {
        when(adminPostService.findAdminPostById(404L)).thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(get("/api/v1/admin/posts/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Post not found")));
    }

    private PostSectionResponse section() {
        return new PostSectionResponse(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas");
    }

    private AdminPostSourceQuestionResponse sourceQuestion() {
        return new AdminPostSourceQuestionResponse(
                1L,
                null,
                "Pregunta original",
                StudentQuestionStatus.PENDING,
                NOW,
                true);
    }
}
