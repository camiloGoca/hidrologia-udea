package edu.udea.hidrologia.link.controller;

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

import edu.udea.hidrologia.link.dto.AdminInterestingLinkRequest;
import edu.udea.hidrologia.link.dto.AdminInterestingLinkResponse;
import edu.udea.hidrologia.link.service.AdminInterestingLinkService;
import edu.udea.hidrologia.link.service.InvalidInterestingLinkRequestException;

@RestController
@RequestMapping("/api/v1/admin/links")
public class AdminInterestingLinkController {

    private static final Set<String> LINK_FIELDS = Set.of(
            "title",
            "description",
            "url",
            "displayOrder",
            "active");

    private final AdminInterestingLinkService adminInterestingLinkService;

    public AdminInterestingLinkController(AdminInterestingLinkService adminInterestingLinkService) {
        this.adminInterestingLinkService = adminInterestingLinkService;
    }

    @GetMapping
    public List<AdminInterestingLinkResponse> findAll() {
        return adminInterestingLinkService.findAll();
    }

    @PostMapping
    public ResponseEntity<AdminInterestingLinkResponse> create(@RequestBody Map<String, Object> request) {
        AdminInterestingLinkResponse response = adminInterestingLinkService.create(toRequest(request));

        return ResponseEntity.created(URI.create("/api/v1/admin/links/" + response.id())).body(response);
    }

    @PatchMapping("/{id}")
    public AdminInterestingLinkResponse update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return adminInterestingLinkService.update(id, toRequest(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminInterestingLinkService.delete(id);

        return ResponseEntity.noContent().build();
    }

    private AdminInterestingLinkRequest toRequest(Map<String, Object> request) {
        if (request == null || !LINK_FIELDS.containsAll(request.keySet())) {
            throw new InvalidInterestingLinkRequestException("Link request is invalid");
        }

        return new AdminInterestingLinkRequest(
                optionalString(request.get("title"), "title"),
                optionalString(request.get("description"), "description"),
                optionalString(request.get("url"), "url"),
                optionalInteger(request.get("displayOrder"), "displayOrder"),
                optionalBoolean(request.get("active"), "active"));
    }

    private String optionalString(Object value, String fieldName) {
        if (value == null) {
            return null;
        }

        if (value instanceof String text) {
            return text;
        }

        throw new InvalidInterestingLinkRequestException("Invalid field: " + fieldName);
    }

    private Integer optionalInteger(Object value, String fieldName) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer integer) {
            return integer;
        }

        throw new InvalidInterestingLinkRequestException("Invalid field: " + fieldName);
    }

    private Boolean optionalBoolean(Object value, String fieldName) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        throw new InvalidInterestingLinkRequestException("Invalid field: " + fieldName);
    }
}
