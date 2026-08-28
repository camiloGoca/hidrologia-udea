package edu.udea.hidrologia.link.service;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.link.dto.AdminInterestingLinkRequest;
import edu.udea.hidrologia.link.dto.AdminInterestingLinkResponse;
import edu.udea.hidrologia.link.entity.InterestingLink;
import edu.udea.hidrologia.link.repository.InterestingLinkRepository;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;

@Service
public class AdminInterestingLinkService {

    private static final int MAX_TITLE_LENGTH = 160;
    private static final int MAX_URL_LENGTH = 2048;

    private final InterestingLinkRepository interestingLinkRepository;
    private final Clock clock;

    public AdminInterestingLinkService(InterestingLinkRepository interestingLinkRepository, Clock clock) {
        this.interestingLinkRepository = interestingLinkRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminInterestingLinkResponse> findAll() {
        return interestingLinkRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminInterestingLinkResponse create(AdminInterestingLinkRequest request) {
        NormalizedLink normalized = normalize(request, 0, true);
        Instant now = Instant.now(clock);
        InterestingLink link = interestingLinkRepository.saveAndFlush(new InterestingLink(
                null,
                normalized.title(),
                normalized.description(),
                normalized.url(),
                normalized.displayOrder(),
                normalized.active(),
                now,
                now));

        return toResponse(link);
    }

    @Transactional
    public AdminInterestingLinkResponse update(Long id, AdminInterestingLinkRequest request) {
        InterestingLink link = interestingLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interesting link not found"));
        NormalizedLink normalized = normalize(request, link.getDisplayOrder(), link.isActive());

        link.update(
                normalized.title(),
                normalized.description(),
                normalized.url(),
                normalized.displayOrder(),
                normalized.active(),
                Instant.now(clock));

        return toResponse(link);
    }

    @Transactional
    public void delete(Long id) {
        InterestingLink link = interestingLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interesting link not found"));

        interestingLinkRepository.delete(link);
    }

    private NormalizedLink normalize(AdminInterestingLinkRequest request, int defaultDisplayOrder, boolean defaultActive) {
        if (request == null) {
            throw new InvalidInterestingLinkRequestException("Request validation failed");
        }

        String title = normalizeRequiredText(request.title(), "El título del enlace es obligatorio.", MAX_TITLE_LENGTH);
        String url = normalizeUrl(request.url());
        String description = normalizeOptionalText(request.description());
        int displayOrder = request.displayOrder() == null ? defaultDisplayOrder : request.displayOrder();
        if (displayOrder < 0) {
            throw new InvalidInterestingLinkRequestException("El orden del enlace no puede ser negativo.");
        }

        boolean active = request.active() == null ? defaultActive : request.active();

        return new NormalizedLink(title, description, url, displayOrder, active);
    }

    private String normalizeRequiredText(String value, String blankMessage, int maxLength) {
        if (value == null) {
            throw new InvalidInterestingLinkRequestException(blankMessage);
        }

        String normalized = value.strip();
        if (normalized.isBlank()) {
            throw new InvalidInterestingLinkRequestException(blankMessage);
        }
        if (normalized.length() > maxLength) {
            throw new InvalidInterestingLinkRequestException("El texto supera la longitud permitida.");
        }

        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeUrl(String value) {
        String normalized = normalizeRequiredText(value, "La URL del enlace es obligatoria.", MAX_URL_LENGTH);
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
                throw new InvalidInterestingLinkRequestException("La URL debe usar http o https.");
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidInterestingLinkRequestException("La URL debe usar http o https.");
        }

        return normalized;
    }

    private AdminInterestingLinkResponse toResponse(InterestingLink link) {
        return new AdminInterestingLinkResponse(
                link.getId(),
                link.getTitle(),
                link.getDescription(),
                link.getUrl(),
                link.getDisplayOrder(),
                link.isActive());
    }

    private record NormalizedLink(
            String title,
            String description,
            String url,
            int displayOrder,
            boolean active) {
    }
}
