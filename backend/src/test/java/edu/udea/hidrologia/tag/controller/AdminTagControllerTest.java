package edu.udea.hidrologia.tag.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.tag.dto.AdminTagResponse;
import edu.udea.hidrologia.tag.dto.UpsertTagRequest;
import edu.udea.hidrologia.tag.service.AdminTagService;
import edu.udea.hidrologia.tag.service.TagConflictException;

class AdminTagControllerTest {

    private AdminTagService adminTagService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminTagService = Mockito.mock(AdminTagService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminTagController(adminTagService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAdminTags() throws Exception {
        when(adminTagService.findAll()).thenReturn(List.of(
                new AdminTagResponse(1L, "Morfometría", "morfometria", 3),
                new AdminTagResponse(2L, "Cuencas", "cuencas", 0)));

        mockMvc.perform(get("/api/v1/admin/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Morfometría")))
                .andExpect(jsonPath("$[0].slug", is("morfometria")))
                .andExpect(jsonPath("$[0].usageCount", is(3)));
    }

    @Test
    void createsTag() throws Exception {
        when(adminTagService.create(Mockito.any(UpsertTagRequest.class)))
                .thenReturn(new AdminTagResponse(1L, "Morfometría", "morfometria", 0));

        mockMvc.perform(post("/api/v1/admin/tags")
                .contentType("application/json")
                .content("""
                        {
                          "name": "Morfometría"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/admin/tags/1"))
                .andExpect(jsonPath("$.usageCount", is(0)));
    }

    @Test
    void rejectsSlugFromClient() throws Exception {
        mockMvc.perform(post("/api/v1/admin/tags")
                .contentType("application/json")
                .content("""
                        {
                          "name": "Morfometría",
                          "slug": "manual"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renamesTag() throws Exception {
        when(adminTagService.rename(Mockito.eq(1L), Mockito.any(UpsertTagRequest.class)))
                .thenReturn(new AdminTagResponse(1L, "Morfometría de cuencas", "morfometria", 2));

        mockMvc.perform(patch("/api/v1/admin/tags/1")
                .contentType("application/json")
                .content("""
                        {
                          "name": "Morfometría de cuencas"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Morfometría de cuencas")))
                .andExpect(jsonPath("$.slug", is("morfometria")));
    }

    @Test
    void returnsConflictForDuplicateTag() throws Exception {
        when(adminTagService.create(Mockito.any(UpsertTagRequest.class)))
                .thenThrow(new TagConflictException("Ya existe un hashtag con ese nombre o URL."));

        mockMvc.perform(post("/api/v1/admin/tags")
                .contentType("application/json")
                .content("""
                        {
                          "name": "Morfometría"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Ya existe un hashtag con ese nombre o URL.")));
    }

    @Test
    void deletesUnusedTag() throws Exception {
        doNothing().when(adminTagService).delete(1L);

        mockMvc.perform(delete("/api/v1/admin/tags/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsConflictWhenTagIsUsed() throws Exception {
        doThrow(new TagConflictException(
                "El hashtag está siendo usado por publicaciones. Quítalo de ellas antes de eliminarlo."))
                .when(adminTagService).delete(1L);

        mockMvc.perform(delete("/api/v1/admin/tags/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundWhenTagDoesNotExist() throws Exception {
        when(adminTagService.rename(Mockito.eq(404L), Mockito.any(UpsertTagRequest.class)))
                .thenThrow(new ResourceNotFoundException("Tag not found"));

        mockMvc.perform(patch("/api/v1/admin/tags/404")
                .contentType("application/json")
                .content("""
                        {
                          "name": "Morfometría"
                        }
                        """))
                .andExpect(status().isNotFound());
    }
}
