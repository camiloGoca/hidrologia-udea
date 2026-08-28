package edu.udea.hidrologia.post.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.post.dto.PostDetailResponse;
import edu.udea.hidrologia.post.dto.PostSearchResultResponse;
import edu.udea.hidrologia.post.dto.SectionPostsResponse;
import edu.udea.hidrologia.post.dto.TagPostsResponse;
import edu.udea.hidrologia.post.service.PostQueryService;

@RestController
@RequestMapping("/api/v1")
public class PostController {

    private final PostQueryService postQueryService;

    public PostController(PostQueryService postQueryService) {
        this.postQueryService = postQueryService;
    }

    @GetMapping("/sections/{slug}/posts")
    @Operation(summary = "List published posts by section")
    public SectionPostsResponse findPublishedPostsBySection(@PathVariable String slug) {
        return postQueryService.findPublishedPostsBySection(slug);
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "Get a published post")
    public PostDetailResponse findPublishedPostById(@PathVariable Long id) {
        return postQueryService.findPublishedPostById(id);
    }

    @GetMapping("/posts/search")
    @Operation(summary = "Search published posts")
    public List<PostSearchResultResponse> searchPublishedPosts(@RequestParam(name = "q", defaultValue = "") String query) {
        return postQueryService.searchPublishedPosts(query);
    }

    @GetMapping("/tags/{slug}/posts")
    @Operation(summary = "List published posts by tag")
    public TagPostsResponse findPublishedPostsByTag(@PathVariable String slug) {
        return postQueryService.findPublishedPostsByTag(slug);
    }
}
