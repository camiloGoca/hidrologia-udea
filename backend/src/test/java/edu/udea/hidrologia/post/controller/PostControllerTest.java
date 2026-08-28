package edu.udea.hidrologia.post.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.post.dto.PostDetailResponse;
import edu.udea.hidrologia.post.dto.PostImageResponse;
import edu.udea.hidrologia.post.dto.PostSectionResponse;
import edu.udea.hidrologia.post.dto.PostSearchResultResponse;
import edu.udea.hidrologia.post.dto.PostSummaryResponse;
import edu.udea.hidrologia.post.dto.SectionPostsResponse;
import edu.udea.hidrologia.post.dto.TagPostsResponse;
import edu.udea.hidrologia.post.service.InvalidPostSearchQueryException;
import edu.udea.hidrologia.post.service.PostQueryService;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.tag.dto.TagResponse;

class PostControllerTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-02T00:00:00Z");

    private PostQueryService postQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        postQueryService = Mockito.mock(PostQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PostController(postQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPublishedPostsBySection() throws Exception {
        PostSectionResponse section = section();
        TagResponse tag = tag();
        when(postQueryService.findPublishedPostsBySection("taller-1"))
                .thenReturn(new SectionPostsResponse(
                        section,
                        List.of(new PostSummaryResponse(1L, "Pregunta publicada", section, List.of(tag), PUBLISHED_AT))));

        mockMvc.perform(get("/api/v1/sections/taller-1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.section.slug", is("taller-1")))
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.posts[0].title", is("Pregunta publicada")))
                .andExpect(jsonPath("$.posts[0].tags[0].slug", is("morfometria")));
    }

    @Test
    void returnsEmptyPostsBySection() throws Exception {
        when(postQueryService.findPublishedPostsBySection("taller-1"))
                .thenReturn(new SectionPostsResponse(section(), List.of()));

        mockMvc.perform(get("/api/v1/sections/taller-1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(0)));
    }

    @Test
    void returnsNotFoundForMissingSection() throws Exception {
        when(postQueryService.findPublishedPostsBySection("desconocida"))
                .thenThrow(new ResourceNotFoundException("Section not found"));

        mockMvc.perform(get("/api/v1/sections/desconocida/posts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Section not found")));
    }

    @Test
    void returnsPublishedPostDetail() throws Exception {
        when(postQueryService.findPublishedPostById(1L))
                .thenReturn(new PostDetailResponse(
                        1L,
                        "Pregunta publicada",
                        "Contenido de texto seguro.",
                        contentDocument("Contenido de texto seguro."),
                        section(),
                        List.of(tag()),
                        List.of(new PostImageResponse(
                                5L,
                                "https://res.cloudinary.com/demo/image/upload/post.png",
                                800,
                                600,
                                "Grafica")),
                        PUBLISHED_AT));

        mockMvc.perform(get("/api/v1/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.content", is("Contenido de texto seguro.")))
                .andExpect(jsonPath("$.contentDocument.type", is("doc")))
                .andExpect(jsonPath("$.images[0].id", is(5)))
                .andExpect(jsonPath("$.images[0].secureUrl", is("https://res.cloudinary.com/demo/image/upload/post.png")))
                .andExpect(jsonPath("$.images[0].altText", is("Grafica")))
                .andExpect(jsonPath("$.images[0].publicId").doesNotExist());
    }

    @Test
    void returnsNotFoundForUnavailablePost() throws Exception {
        when(postQueryService.findPublishedPostById(99L))
                .thenThrow(new ResourceNotFoundException("Post not found"));

        mockMvc.perform(get("/api/v1/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Post not found")));
    }

    @Test
    void returnsPublishedPostsByTag() throws Exception {
        PostSectionResponse section = section();
        TagResponse tag = tag();
        when(postQueryService.findPublishedPostsByTag("morfometria"))
                .thenReturn(new TagPostsResponse(
                        tag,
                        List.of(new PostSummaryResponse(1L, "Pregunta publicada", section, List.of(tag), PUBLISHED_AT))));

        mockMvc.perform(get("/api/v1/tags/morfometria/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tag.slug", is("morfometria")))
                .andExpect(jsonPath("$.posts", hasSize(1)));
    }

    @Test
    void searchesPublishedPosts() throws Exception {
        when(postQueryService.searchPublishedPosts("balance"))
                .thenReturn(List.of(new PostSearchResultResponse(
                        1L,
                        "Balance hidrico",
                        section(),
                        List.of(tag()),
                        "Extracto seguro sin HTML",
                        PUBLISHED_AT)));

        mockMvc.perform(get("/api/v1/posts/search").param("q", "balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Balance hidrico")))
                .andExpect(jsonPath("$[0].section.slug", is("taller-1")))
                .andExpect(jsonPath("$[0].tags[0].slug", is("morfometria")))
                .andExpect(jsonPath("$[0].snippet", is("Extracto seguro sin HTML")))
                .andExpect(jsonPath("$[0].contentDocument").doesNotExist())
                .andExpect(jsonPath("$[0].sourceQuestion").doesNotExist())
                .andExpect(jsonPath("$[0].images").doesNotExist());
    }

    @Test
    void returnsBadRequestForInvalidSearchQuery() throws Exception {
        when(postQueryService.searchPublishedPosts(""))
                .thenThrow(new InvalidPostSearchQueryException("Search query must contain at least 2 characters."));

        mockMvc.perform(get("/api/v1/posts/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Search query must contain at least 2 characters.")));
    }

    @Test
    void returnsNotFoundForMissingTag() throws Exception {
        when(postQueryService.findPublishedPostsByTag("desconocido"))
                .thenThrow(new ResourceNotFoundException("Tag not found"));

        mockMvc.perform(get("/api/v1/tags/desconocido/posts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Tag not found")));
    }

    private PostSectionResponse section() {
        return new PostSectionResponse(
                1L,
                SectionType.TALLER,
                "Taller 1",
                "taller-1",
                "Morfometria de cuencas");
    }

    private TagResponse tag() {
        return new TagResponse("Morfometria", "morfometria");
    }

    private Map<String, Object> contentDocument(String content) {
        return Map.of(
                "type", "doc",
                "content", List.of(Map.of(
                        "type", "paragraph",
                        "content", List.of(Map.of("type", "text", "text", content)))));
    }
}
