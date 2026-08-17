package edu.udea.hidrologia.shared.health.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", "Hidrologia UdeA API is running", Instant.now());
    }

    public record HealthResponse(String status, String message, Instant checkedAt) {
    }
}
