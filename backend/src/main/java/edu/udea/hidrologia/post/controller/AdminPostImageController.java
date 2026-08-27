package edu.udea.hidrologia.post.controller;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.udea.hidrologia.post.dto.AdminPostImageResponse;
import edu.udea.hidrologia.post.service.AdminPostImageService;

@RestController
@RequestMapping("/api/v1/admin/posts/{postId}/images")
public class AdminPostImageController {

    private final AdminPostImageService adminPostImageService;

    public AdminPostImageController(AdminPostImageService adminPostImageService) {
        this.adminPostImageService = adminPostImageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminPostImageResponse> upload(
            @PathVariable Long postId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("altText") String altText) {
        AdminPostImageResponse response = adminPostImageService.upload(postId, file, altText);

        return ResponseEntity.created(URI.create("/api/v1/admin/posts/" + postId + "/images/" + response.id()))
                .body(response);
    }

    @PatchMapping("/{imageId}")
    public AdminPostImageResponse updateAltText(
            @PathVariable Long postId,
            @PathVariable Long imageId,
            @RequestBody Map<String, Object> request) {
        return adminPostImageService.updateAltText(postId, imageId, request);
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long postId,
            @PathVariable Long imageId) {
        adminPostImageService.delete(postId, imageId);
    }
}
