package edu.udea.hidrologia.post.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.post.service.AdminPostService;

@RestController
@RequestMapping("/api/v1/admin/posts")
public class AdminPostController {

    private final AdminPostService adminPostService;

    public AdminPostController(AdminPostService adminPostService) {
        this.adminPostService = adminPostService;
    }

    @GetMapping("/{id}")
    public AdminPostResponse findAdminPostById(@PathVariable Long id) {
        return adminPostService.findAdminPostById(id);
    }
}
