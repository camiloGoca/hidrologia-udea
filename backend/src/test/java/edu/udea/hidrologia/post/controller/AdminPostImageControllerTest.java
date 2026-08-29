package edu.udea.hidrologia.post.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import edu.udea.hidrologia.post.dto.AdminPostImageResponse;
import edu.udea.hidrologia.post.service.AdminPostImageService;
import edu.udea.hidrologia.post.service.PostStateConflictException;
import edu.udea.hidrologia.shared.error.GlobalExceptionHandler;
import edu.udea.hidrologia.shared.error.ResourceNotFoundException;
import edu.udea.hidrologia.shared.storage.ImageStorageException;
import edu.udea.hidrologia.shared.storage.InvalidImageException;

class AdminPostImageControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-03T00:00:00Z");

    private AdminPostImageService adminPostImageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminPostImageService = Mockito.mock(AdminPostImageService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPostImageController(adminPostImageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadsPostImage() throws Exception {
        when(adminPostImageService.upload(eq(9L), any(MultipartFile.class), eq("Diagrama")))
                .thenReturn(response());

        mockMvc.perform(multipart("/api/v1/admin/posts/9/images")
                .file(new MockMultipartFile("file", "image.png", "image/png", new byte[] {1, 2, 3}))
                .param("altText", "Diagrama"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/admin/posts/9/images/15"))
                .andExpect(jsonPath("$.id", is(15)))
                .andExpect(jsonPath("$.secureUrl", is("https://res.cloudinary.com/demo/image/upload/post.png")))
                .andExpect(jsonPath("$.format", is("png")))
                .andExpect(jsonPath("$.width", is(800)))
                .andExpect(jsonPath("$.height", is(600)))
                .andExpect(jsonPath("$.bytes", is(1000)))
                .andExpect(jsonPath("$.altText", is("Diagrama")))
                .andExpect(jsonPath("$.createdAt", is("2026-01-03T00:00:00Z")))
                .andExpect(jsonPath("$.publicId").doesNotExist());
    }

    @Test
    void returnsBadRequestForInvalidImage() throws Exception {
        when(adminPostImageService.upload(eq(9L), any(MultipartFile.class), eq("Imagen")))
                .thenThrow(new InvalidImageException("Only JPEG and PNG images are supported"));

        mockMvc.perform(multipart("/api/v1/admin/posts/9/images")
                .file(new MockMultipartFile("file", "image.svg", "image/svg+xml", new byte[] {1, 2, 3}))
                .param("altText", "Imagen"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesAltText() throws Exception {
        when(adminPostImageService.updateAltText(eq(9L), eq(15L), any(Map.class)))
                .thenReturn(new AdminPostImageResponse(
                        15L,
                        "https://res.cloudinary.com/demo/image/upload/post.png",
                        "png",
                        800,
                        600,
                        1000L,
                        "Nueva descripción",
                        NOW));

        mockMvc.perform(patch("/api/v1/admin/posts/9/images/15")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "altText": "Nueva descripción"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.altText", is("Nueva descripción")))
                .andExpect(jsonPath("$.publicId").doesNotExist());
    }

    @Test
    void returnsNotFoundWhenPostImageDoesNotExist() throws Exception {
        when(adminPostImageService.updateAltText(eq(9L), eq(404L), any(Map.class)))
                .thenThrow(new ResourceNotFoundException("Post image not found"));

        mockMvc.perform(patch("/api/v1/admin/posts/9/images/404")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "altText": "Nueva descripción"
                        }
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesPostImage() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/posts/9/images/15"))
                .andExpect(status().isNoContent());

        verify(adminPostImageService).delete(9L, 15L);
    }

    @Test
    void returnsServiceUnavailableWhenImageStorageDeleteFails() throws Exception {
        Mockito.doThrow(new ImageStorageException(
                "Image storage is temporarily unavailable",
                new RuntimeException("Request forbidden due to missing permissions")))
                .when(adminPostImageService)
                .delete(9L, 15L);

        mockMvc.perform(delete("/api/v1/admin/posts/9/images/15"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", is("Image uploads are temporarily unavailable")));
    }

    @Test
    void returnsConflictWhenImageIsStillReferenced() throws Exception {
        Mockito.doThrow(new PostStateConflictException("Remove the image from the post content before deleting it"))
                .when(adminPostImageService)
                .delete(9L, 15L);

        mockMvc.perform(delete("/api/v1/admin/posts/9/images/15"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Remove the image from the post content before deleting it")));
    }

    private AdminPostImageResponse response() {
        return new AdminPostImageResponse(
                15L,
                "https://res.cloudinary.com/demo/image/upload/post.png",
                "png",
                800,
                600,
                1000L,
                "Diagrama",
                NOW);
    }
}
