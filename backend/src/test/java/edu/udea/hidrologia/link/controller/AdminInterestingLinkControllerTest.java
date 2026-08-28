package edu.udea.hidrologia.link.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
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

import edu.udea.hidrologia.link.dto.AdminInterestingLinkRequest;
import edu.udea.hidrologia.link.dto.AdminInterestingLinkResponse;
import edu.udea.hidrologia.link.service.AdminInterestingLinkService;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

class AdminInterestingLinkControllerTest {

    private AdminInterestingLinkService adminInterestingLinkService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminInterestingLinkService = Mockito.mock(AdminInterestingLinkService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminInterestingLinkController(adminInterestingLinkService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAdminLinks() throws Exception {
        when(adminInterestingLinkService.findAll()).thenReturn(List.of(
                new AdminInterestingLinkResponse(
                        1L,
                        "IDEAM",
                        "Recurso oficial",
                        "https://example.edu/ideam",
                        1,
                        true)));

        mockMvc.perform(get("/api/v1/admin/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("IDEAM")))
                .andExpect(jsonPath("$[0].description", is("Recurso oficial")))
                .andExpect(jsonPath("$[0].url", is("https://example.edu/ideam")))
                .andExpect(jsonPath("$[0].displayOrder", is(1)))
                .andExpect(jsonPath("$[0].active", is(true)));
    }

    @Test
    void createsLink() throws Exception {
        when(adminInterestingLinkService.create(Mockito.any(AdminInterestingLinkRequest.class)))
                .thenReturn(new AdminInterestingLinkResponse(
                        1L,
                        "IDEAM",
                        null,
                        "https://example.edu/ideam",
                        0,
                        true));

        mockMvc.perform(post("/api/v1/admin/links")
                .contentType("application/json")
                .content("""
                        {
                          "title": "IDEAM",
                          "description": null,
                          "url": "https://example.edu/ideam",
                          "displayOrder": 0,
                          "active": true
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/admin/links/1"))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void rejectsUnknownFields() throws Exception {
        mockMvc.perform(post("/api/v1/admin/links")
                .contentType("application/json")
                .content("""
                        {
                          "title": "IDEAM",
                          "url": "https://example.edu/ideam",
                          "createdAt": "2026-01-01T00:00:00Z"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidFieldTypes() throws Exception {
        mockMvc.perform(post("/api/v1/admin/links")
                .contentType("application/json")
                .content("""
                        {
                          "title": "IDEAM",
                          "url": "https://example.edu/ideam",
                          "displayOrder": "primero"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesLink() throws Exception {
        when(adminInterestingLinkService.update(Mockito.eq(1L), Mockito.any(AdminInterestingLinkRequest.class)))
                .thenReturn(new AdminInterestingLinkResponse(
                        1L,
                        "IDEAM actualizado",
                        "Descripcion",
                        "https://example.edu/nuevo",
                        2,
                        false));

        mockMvc.perform(patch("/api/v1/admin/links/1")
                .contentType("application/json")
                .content("""
                        {
                          "title": "IDEAM actualizado",
                          "description": "Descripcion",
                          "url": "https://example.edu/nuevo",
                          "displayOrder": 2,
                          "active": false
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("IDEAM actualizado")))
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void deletesLink() throws Exception {
        doNothing().when(adminInterestingLinkService).delete(1L);

        mockMvc.perform(delete("/api/v1/admin/links/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenLinkDoesNotExist() throws Exception {
        when(adminInterestingLinkService.update(Mockito.eq(404L), Mockito.any(AdminInterestingLinkRequest.class)))
                .thenThrow(new ResourceNotFoundException("Interesting link not found"));

        mockMvc.perform(patch("/api/v1/admin/links/404")
                .contentType("application/json")
                .content("""
                        {
                          "title": "IDEAM",
                          "url": "https://example.edu/ideam"
                        }
                        """))
                .andExpect(status().isNotFound());
    }
}
