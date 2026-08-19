package edu.udea.hidrologia.link.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.udea.hidrologia.link.dto.InterestingLinkResponse;
import edu.udea.hidrologia.link.service.InterestingLinkService;

class InterestingLinkControllerTest {

    private InterestingLinkService interestingLinkService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        interestingLinkService = Mockito.mock(InterestingLinkService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InterestingLinkController(interestingLinkService)).build();
    }

    @Test
    void returnsEmptyListWhenThereAreNoActiveLinks() throws Exception {
        when(interestingLinkService.findActiveLinks()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsActiveLinksAsJson() throws Exception {
        when(interestingLinkService.findActiveLinks()).thenReturn(List.of(
                new InterestingLinkResponse(
                        1L,
                        "Recurso aprobado",
                        "Descripcion publica opcional",
                        "https://example.edu/recurso",
                        10)));

        mockMvc.perform(get("/api/v1/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Recurso aprobado")))
                .andExpect(jsonPath("$[0].description", is("Descripcion publica opcional")))
                .andExpect(jsonPath("$[0].url", is("https://example.edu/recurso")))
                .andExpect(jsonPath("$[0].displayOrder", is(10)));
    }
}
