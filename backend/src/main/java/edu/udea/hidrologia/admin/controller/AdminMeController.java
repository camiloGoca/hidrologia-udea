package edu.udea.hidrologia.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udea.hidrologia.admin.dto.AdminMeResponse;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminMeController {

    @GetMapping("/me")
    @Operation(summary = "Verify the current administrator session")
    public AdminMeResponse me() {
        return new AdminMeResponse(true, "ADMIN");
    }
}
