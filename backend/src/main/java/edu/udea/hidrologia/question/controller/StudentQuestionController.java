package edu.udea.hidrologia.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.question.dto.CreateStudentQuestionRequest;
import edu.udea.hidrologia.question.dto.CreateStudentQuestionResponse;
import edu.udea.hidrologia.question.service.StudentQuestionService;

@RestController
@RequestMapping("/api/v1/questions")
public class StudentQuestionController {

    private final StudentQuestionService studentQuestionService;

    public StudentQuestionController(StudentQuestionService studentQuestionService) {
        this.studentQuestionService = studentQuestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a student question")
    public CreateStudentQuestionResponse createQuestion(
            @Valid @RequestBody CreateStudentQuestionRequest request) {
        return studentQuestionService.createQuestion(request);
    }
}
