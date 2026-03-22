package com.credit.controller;

import com.credit.dto.MasterAccountResponse;
import com.credit.dto.MasterAccountTopUpRequest;
import com.credit.service.MasterAccountService;
import com.credit.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employee/master-account")
@RequiredArgsConstructor
@Tag(name = "Мастер-счет банка")
public class MasterAccountController {

    private final MasterAccountService masterAccountService;
    private final JwtUtil jwtUtil;

    @GetMapping
    @Operation(summary = "Получить мастер-счет банка (Сотрудник)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MasterAccountResponse> getMasterAccount(HttpServletRequest servletRequest) {
        requireEmployeeRole(servletRequest);
        return ResponseEntity.ok(masterAccountService.getMasterAccount());
    }

    @PostMapping("/top-up")
    @Operation(summary = "Пополнить мастер-счет банка (Сотрудник)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MasterAccountResponse> topUp(
            HttpServletRequest servletRequest,
            @Valid @RequestBody MasterAccountTopUpRequest request) {
        requireEmployeeRole(servletRequest);
        return ResponseEntity.ok(masterAccountService.topUp(request.getAmount()));
    }

    private void requireEmployeeRole(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader("Authorization");
        List<String> roles = jwtUtil.getRolesFromAuthHeader(authHeader);
        if (!roles.contains("Employee")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: Employee role required");
        }
    }
}
