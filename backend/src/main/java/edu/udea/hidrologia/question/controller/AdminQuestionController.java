package edu.udea.hidrologia.question.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.question.dto.AdminPendingQuestionsResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionDetailResponse;
import edu.udea.hidrologia.question.service.AdminQuestionService;

@RestController
@RequestMapping("/api/v1/admin/questions")
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    public AdminQuestionController(AdminQuestionService adminQuestionService) {
        this.adminQuestionService = adminQuestionService;
    }

    @GetMapping("/pending")
    public AdminPendingQuestionsResponse findPendingQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminQuestionService.findPendingQuestions(page, size);
    }

    @GetMapping("/{id}")
    public AdminQuestionDetailResponse findQuestionById(@PathVariable Long id) {
        return adminQuestionService.findQuestionById(id);
    }
}
