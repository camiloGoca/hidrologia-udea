package edu.udea.hidrologia.shared.security;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.udea.hidrologia.link.service.InterestingLinkService;
import edu.udea.hidrologia.post.dto.PostDetailResponse;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.dto.SectionPostsResponse;
import edu.udea.hidrologia.post.dto.TagPostsResponse;
import edu.udea.hidrologia.post.service.PostQueryService;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.service.StudentQuestionService;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.repository.SectionRepository;
import edu.udea.hidrologia.tag.dto.TagResponse;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @MockitoBean
    private SectionRepository sectionRepository;

    @MockitoBean
    private InterestingLinkService interestingLinkService;

    @MockitoBean
    private PostQueryService postQueryService;

    @MockitoBean
    private StudentQuestionService studentQuestionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsPublicLinksEndpoint() throws Exception {
        when(interestingLinkService.findActiveLinks()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/links"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPublicSectionPostsEndpoint() throws Exception {
        PostSectionResponse section = new PostSectionResponse(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas");
        when(postQueryService.findPublishedPostsBySection("taller-1"))
                .thenReturn(new SectionPostsResponse(section, List.of()));

        mockMvc.perform(get("/api/v1/sections/taller-1/posts"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPublicPostDetailEndpoint() throws Exception {
        PostSectionResponse section = new PostSectionResponse(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas");
        when(postQueryService.findPublishedPostById(1L))
                .thenReturn(new PostDetailResponse(1L, "Titulo", "Contenido", section, List.of(), null));

        mockMvc.perform(get("/api/v1/posts/1"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPublicTagPostsEndpoint() throws Exception {
        TagResponse tag = new TagResponse("Morfometria", "morfometria");
        when(postQueryService.findPublishedPostsByTag("morfometria"))
                .thenReturn(new TagPostsResponse(tag, List.of()));

        mockMvc.perform(get("/api/v1/tags/morfometria/posts"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsPublicQuestionSubmissionEndpoint() throws Exception {
        when(studentQuestionService.createQuestion(any()))
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
                .andExpect(status().isCreated());
    }

    @Test
    void deniesUnapprovedApiRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/links"))
                .andExpect(status().isForbidden());
    }
}
