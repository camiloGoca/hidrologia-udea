package edu.udea.hidrologia.post.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.post.content.PostContentDocumentService;
import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.dto.AdminPostsResponse;
import edu.udea.hidrologia.post.dto.CreatePostRequest;
import edu.udea.hidrologia.post.dto.UpdatePostRequest;
import edu.udea.hidrologia.post.entity.PostStatus;
import edu.udea.hidrologia.post.service.AdminPostPublicationService;
import edu.udea.hidrologia.post.service.AdminPostService;
import edu.udea.hidrologia.post.service.InvalidPostDraftRequestException;

@RestController
@RequestMapping("/api/v1/admin/posts")
public class AdminPostController {

    private static final Set<String> CREATE_POST_FIELDS = Set.of("sectionSlug");
    private static final Set<String> UPDATE_POST_FIELDS = Set.of("title", "contentDocument", "sectionSlug", "tagIds");

    private final AdminPostService adminPostService;
    private final AdminPostPublicationService adminPostPublicationService;
    private final PostContentDocumentService postContentDocumentService;

    public AdminPostController(
            AdminPostService adminPostService,
            AdminPostPublicationService adminPostPublicationService,
            PostContentDocumentService postContentDocumentService) {
        this.adminPostService = adminPostService;
        this.adminPostPublicationService = adminPostPublicationService;
        this.postContentDocumentService = postContentDocumentService;
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

    @PostMapping
    public ResponseEntity<AdminPostResponse> createManualDraft(@RequestBody Map<String, Object> request) {
        AdminPostResponse response = adminPostService.createManualDraft(toCreatePostRequest(request));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public AdminPostResponse updatePost(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        return adminPostService.updatePost(id, toUpdatePostRequest(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discardManualDraft(@PathVariable Long id) {
        adminPostService.discardManualDraft(id);
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
                requiredJsonObject(request, "contentDocument"),
                requiredString(request, "sectionSlug"),
                optionalPositiveLongList(request, "tagIds"));
    }

    private CreatePostRequest toCreatePostRequest(Map<String, Object> request) {
        if (request == null) {
            throw new InvalidPostDraftRequestException("Post create request is invalid");
        }

        if (!CREATE_POST_FIELDS.containsAll(request.keySet())) {
            throw new InvalidPostDraftRequestException("Post create request contains unsupported fields");
        }

        return new CreatePostRequest(requiredString(request, "sectionSlug"));
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

    private List<Long> optionalPositiveLongList(Map<String, Object> request, String field) {
        if (!request.containsKey(field) || request.get(field) == null) {
            return null;
        }

        Object value = request.get(field);
        if (!(value instanceof List<?> values)) {
            throw new InvalidPostDraftRequestException("Post update request is invalid");
        }

        List<Long> ids = new ArrayList<>();
        for (Object item : values) {
            if (!(item instanceof Number number) || number.longValue() <= 0
                    || Double.compare(number.doubleValue(), number.longValue()) != 0) {
                throw new InvalidPostDraftRequestException("Post update request is invalid");
            }
            ids.add(number.longValue());
        }

        return ids;
    }

    private Map<String, Object> requiredJsonObject(Map<String, Object> request, String field) {
        Object value = request.get(field);
        if (!(value instanceof Map<?, ?>)) {
            throw new InvalidPostDraftRequestException("Post update request is invalid");
        }

        return postContentDocumentService.toDocument(value);
    }
}
