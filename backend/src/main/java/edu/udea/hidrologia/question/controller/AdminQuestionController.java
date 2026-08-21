package edu.udea.hidrologia.question.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.question.dto.AdminQuestionDetailResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionStatusUpdateResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionsResponse;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.service.AdminQuestionService;

@RestController
@RequestMapping("/api/v1/admin/questions")
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    public AdminQuestionController(AdminQuestionService adminQuestionService) {
        this.adminQuestionService = adminQuestionService;
    }

    @GetMapping
    public AdminQuestionsResponse findQuestionsByStatus(
            @RequestParam(defaultValue = "PENDING") StudentQuestionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminQuestionService.findQuestionsByStatus(status, page, size);
    }

    @GetMapping("/pending")
    public AdminQuestionsResponse findPendingQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminQuestionService.findPendingQuestions(page, size);
    }

    @GetMapping("/{id}")
    public AdminQuestionDetailResponse findQuestionById(@PathVariable Long id) {
        return adminQuestionService.findQuestionById(id);
    }

    @PostMapping("/{id}/reject")
    public AdminQuestionStatusUpdateResponse rejectQuestion(@PathVariable Long id) {
        return adminQuestionService.rejectQuestion(id);
    }

    @PostMapping("/{id}/archive")
    public AdminQuestionStatusUpdateResponse archiveQuestion(@PathVariable Long id) {
        return adminQuestionService.archiveQuestion(id);
    }

    @PostMapping("/{id}/reopen")
    public AdminQuestionStatusUpdateResponse reopenQuestion(@PathVariable Long id) {
        return adminQuestionService.reopenQuestion(id);
    }
}
