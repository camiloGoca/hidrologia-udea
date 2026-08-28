package edu.udea.hidrologia.analytics.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AnalyticsSessionRequest(
        @NotNull UUID sessionId) {
}
