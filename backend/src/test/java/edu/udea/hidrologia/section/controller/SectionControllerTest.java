package edu.udea.hidrologia.section.controller;

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

import edu.udea.hidrologia.section.dto.SectionResponse;
import edu.udea.hidrologia.section.entity.SectionType;
import edu.udea.hidrologia.section.service.SectionService;

class SectionControllerTest {

    private SectionService sectionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sectionService = Mockito.mock(SectionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SectionController(sectionService)).build();
    }

    @Test
    void returnsActiveSectionsAsJson() throws Exception {
        when(sectionService.findActiveSections()).thenReturn(List.of(
                new SectionResponse(
                        1L,
                        SectionType.TALLER,
                        "Taller 1",
                        "taller-1",
                        "Morfometría de cuencas",
                        1),
                new SectionResponse(
                        2L,
                        SectionType.TALLER,
                        "Taller 2",
                        "taller-2",
                        "Estadística y balance hídrico",
                        2)));

        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].type", is("TALLER")))
                .andExpect(jsonPath("$[0].name", is("Taller 1")))
                .andExpect(jsonPath("$[0].slug", is("taller-1")))
                .andExpect(jsonPath("$[0].description", is("Morfometría de cuencas")))
                .andExpect(jsonPath("$[0].displayOrder", is(1)))
                .andExpect(jsonPath("$[1].description", is("Estadística y balance hídrico")))
                .andExpect(jsonPath("$[1].displayOrder", is(2)));
    }
}
