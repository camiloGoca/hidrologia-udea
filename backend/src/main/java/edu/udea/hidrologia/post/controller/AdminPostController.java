package edu.udea.hidrologia.post.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostsResponse;
import edu.udea.hidrologia.post.dto.UpdatePostRequest;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.service.AdminPostPublicationService;
import edu.udea.hidrologia.post.service.AdminPostService;
import edu.udea.hidrologia.post.service.InvalidPostDraftRequestException;

@RestController
@RequestMapping("/api/v1/admin/posts")
public class AdminPostController {

    private static final Set<String> UPDATE_POST_FIELDS = Set.of("title", "content", "sectionSlug");

    private final AdminPostService adminPostService;
    private final AdminPostPublicationService adminPostPublicationService;

    public AdminPostController(
            AdminPostService adminPostService,
            AdminPostPublicationService adminPostPublicationService) {
        this.adminPostService = adminPostService;
        this.adminPostPublicationService = adminPostPublicationService;
    }

    @GetMapping
    public AdminPostsResponse findPostsByStatus(
            @RequestParam(defaultValue = "DRAFT") PostStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminPostService.findPostsByStatus(status, page, size);
    }

    @GetMapping("/{id}")
    public AdminPostResponse findAdminPostById(@PathVariable Long id) {
        return adminPostService.findAdminPostById(id);
    }

    @PatchMapping("/{id}")
    public AdminPostResponse updatePost(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        return adminPostService.updatePost(id, toUpdatePostRequest(request));
    }

    @PostMapping("/{id}/publish")
    public AdminPostResponse publishDraft(@PathVariable Long id) {
        return adminPostPublicationService.publishDraft(id);
    }

    @PostMapping("/{id}/archive")
    public AdminPostResponse archivePost(@PathVariable Long id) {
        return adminPostPublicationService.archivePost(id);
    }

    @PostMapping("/{id}/restore")
    public AdminPostResponse restorePost(@PathVariable Long id) {
        return adminPostPublicationService.restorePost(id);
    }

    private UpdatePostRequest toUpdatePostRequest(Map<String, Object> request) {
        if (request == null) {
            throw new InvalidPostDraftRequestException("Post update request is invalid");
        }

        if (!UPDATE_POST_FIELDS.containsAll(request.keySet())) {
            throw new InvalidPostDraftRequestException("Post update request contains unsupported fields");
        }

        return new UpdatePostRequest(
                requiredString(request, "title"),
                requiredString(request, "content"),
                requiredString(request, "sectionSlug"));
    }

    private String requiredString(Map<String, Object> request, String field) {
        Object value = request.get(field);
        if (!(value instanceof String stringValue)) {
            throw new InvalidPostDraftRequestException("Post update request is invalid");
        }

        if ("sectionSlug".equals(field) && stringValue.isBlank()) {
            throw new InvalidPostDraftRequestException("Post update request is invalid");
        }

        if ("title".equals(field) && stringValue.length() > 180) {
            throw new InvalidPostDraftRequestException("Post update request is invalid");
        }

        if ("sectionSlug".equals(field) && stringValue.length() > 120) {
            throw new InvalidPostDraftRequestException("Post update request is invalid");
        }

        return stringValue;
    }
}
