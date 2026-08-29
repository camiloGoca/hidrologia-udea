package edu.udea.hidrologia.question.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.post.dto.AdminPostResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionDetailResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionStatusUpdateResponse;
import edu.udea.hidrologia.question.dto.AdminQuestionsResponse;
import edu.udea.hidrologia.question.entity.StudentQuestionStatus;
import edu.udea.hidrologia.question.service.AdminQuestionDraftService;
import edu.udea.hidrologia.question.service.AdminQuestionService;

@RestController
@RequestMapping("/api/v1/admin/questions")
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;
    private final AdminQuestionDraftService adminQuestionDraftService;

    public AdminQuestionController(
            AdminQuestionService adminQuestionService,
            AdminQuestionDraftService adminQuestionDraftService) {
        this.adminQuestionService = adminQuestionService;
        this.adminQuestionDraftService = adminQuestionDraftService;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRejectedQuestion(@PathVariable Long id) {
        adminQuestionService.deleteRejectedQuestion(id);

        return ResponseEntity.noContent().build();
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

    @PostMapping("/{id}/draft")
    public ResponseEntity<AdminPostResponse> createDraft(@PathVariable Long id) {
        AdminPostResponse response = adminQuestionDraftService.createDraft(id);

        return ResponseEntity
                .created(URI.create("/api/v1/admin/posts/" + response.id()))
                .body(response);
    }

    @DeleteMapping("/{id}/draft")
    public ResponseEntity<Void> discardDraft(@PathVariable Long id) {
        adminQuestionDraftService.discardDraft(id);

        return ResponseEntity.noContent().build();
    }
}
