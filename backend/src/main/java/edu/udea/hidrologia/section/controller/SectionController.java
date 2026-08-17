package edu.udea.hidrologia.section.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.section.dto.SectionResponse;
import edu.udea.hidrologia.section.service.SectionService;

@RestController
@RequestMapping("/api/v1/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping
    @Operation(summary = "List active academic sections")
    public List<SectionResponse> findActiveSections() {
        return sectionService.findActiveSections();
    }
}
