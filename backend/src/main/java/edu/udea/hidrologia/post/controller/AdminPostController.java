package edu.udea.hidrologia.post.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.UpdatePostDraftRequest;
import edu.udea.hidrologia.post.service.AdminPostPublicationService;
import edu.udea.hidrologia.post.service.AdminPostService;
import edu.udea.hidrologia.post.service.InvalidPostDraftRequestException;

@RestController
@RequestMapping("/api/v1/admin/posts")
public class AdminPostController {

    private static final Set<String> UPDATE_DRAFT_FIELDS = Set.of("title", "content", "sectionSlug");

    private final AdminPostService adminPostService;
    private final AdminPostPublicationService adminPostPublicationService;

    public AdminPostController(
            AdminPostService adminPostService,
            AdminPostPublicationService adminPostPublicationService) {
        this.adminPostService = adminPostService;
        this.adminPostPublicationService = adminPostPublicationService;
    }

    @GetMapping("/{id}")
    public AdminPostResponse findAdminPostById(@PathVariable Long id) {
        return adminPostService.findAdminPostById(id);
    }

    @PatchMapping("/{id}")
    public AdminPostResponse updateDraft(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        return adminPostService.updateDraft(id, toUpdateDraftRequest(request));
    }

    @PostMapping("/{id}/publish")
    public AdminPostResponse publishDraft(@PathVariable Long id) {
        return adminPostPublicationService.publishDraft(id);
    }

    private UpdatePostDraftRequest toUpdateDraftRequest(Map<String, Object> request) {
        if (!UPDATE_DRAFT_FIELDS.containsAll(request.keySet())) {
            throw new InvalidPostDraftRequestException("Draft update request contains unsupported fields");
        }

        return new UpdatePostDraftRequest(
                requiredString(request, "title"),
                requiredString(request, "content"),
                requiredString(request, "sectionSlug"));
    }

    private String requiredString(Map<String, Object> request, String field) {
        Object value = request.get(field);
        if (!(value instanceof String stringValue)) {
            throw new InvalidPostDraftRequestException("Draft update request is invalid");
        }

        if ("sectionSlug".equals(field) && stringValue.isBlank()) {
            throw new InvalidPostDraftRequestException("Draft update request is invalid");
        }

        if ("title".equals(field) && stringValue.length() > 180) {
            throw new InvalidPostDraftRequestException("Draft update request is invalid");
        }

        if ("sectionSlug".equals(field) && stringValue.length() > 120) {
            throw new InvalidPostDraftRequestException("Draft update request is invalid");
        }

        return stringValue;
    }
}
