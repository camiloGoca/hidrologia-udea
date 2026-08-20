package edu.udea.hidrologia.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit a student question",
            requestBody = @RequestBody(content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CreateStudentQuestionMultipartRequest.class))))
    public CreateStudentQuestionResponse createQuestion(
            @Valid @RequestPart("data") CreateStudentQuestionRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return studentQuestionService.createQuestion(request, image);
    }

    private record CreateStudentQuestionMultipartRequest(
            CreateStudentQuestionRequest data,
            MultipartFile image) {
    }
}
