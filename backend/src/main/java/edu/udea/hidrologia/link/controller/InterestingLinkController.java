package edu.udea.hidrologia.link.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.link.dto.InterestingLinkResponse;
import edu.udea.hidrologia.link.service.InterestingLinkService;

@RestController
@RequestMapping("/api/v1/links")
public class InterestingLinkController {

    private final InterestingLinkService interestingLinkService;

    public InterestingLinkController(InterestingLinkService interestingLinkService) {
        this.interestingLinkService = interestingLinkService;
    }

    @GetMapping
    @Operation(summary = "List active interesting links")
    public List<InterestingLinkResponse> findActiveLinks() {
        return interestingLinkService.findActiveLinks();
    }
}
