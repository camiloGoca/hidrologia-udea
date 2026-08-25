package edu.udea.hidrologia.tag.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.tag.dto.AdminTagResponse;
import edu.udea.hidrologia.tag.dto.UpsertTagRequest;
import edu.udea.hidrologia.tag.service.AdminTagService;
import edu.udea.hidrologia.tag.service.InvalidTagRequestException;

@RestController
@RequestMapping("/api/v1/admin/tags")
public class AdminTagController {

    private static final Set<String> TAG_FIELDS = Set.of("name");

    private final AdminTagService adminTagService;

    public AdminTagController(AdminTagService adminTagService) {
        this.adminTagService = adminTagService;
    }

    @GetMapping
    public List<AdminTagResponse> findAll() {
        return adminTagService.findAll();
    }

    @PostMapping
    public ResponseEntity<AdminTagResponse> create(@RequestBody Map<String, Object> request) {
        AdminTagResponse response = adminTagService.create(toRequest(request));

        return ResponseEntity.created(URI.create("/api/v1/admin/tags/" + response.id())).body(response);
    }

    @PatchMapping("/{id}")
    public AdminTagResponse rename(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return adminTagService.rename(id, toRequest(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminTagService.delete(id);

        return ResponseEntity.noContent().build();
    }

    private UpsertTagRequest toRequest(Map<String, Object> request) {
        if (request == null || !TAG_FIELDS.containsAll(request.keySet())) {
            throw new InvalidTagRequestException("Tag request is invalid");
        }

        Object value = request.get("name");
        if (!(value instanceof String name)) {
            throw new InvalidTagRequestException("Tag request is invalid");
        }

        return new UpsertTagRequest(name);
    }
}
