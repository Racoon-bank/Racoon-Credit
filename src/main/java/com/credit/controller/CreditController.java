package com.credit.controller;

import com.credit.dto.CreditPaymentResponse;
import com.credit.dto.CreditResponse;
import com.credit.dto.CreditStatisticsResponse;
import com.credit.dto.PaymentScheduleResponse;
import com.credit.dto.RepayCreditRequest;
import com.credit.dto.TakeCreditRequest;
import com.credit.service.CreditService;
import com.credit.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@Tag(name = "Управление кредитами")
public class CreditController {

    private final CreditService creditService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @Operation(summary = "Взять кредит")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditResponse> takeCredit(
            HttpServletRequest servletRequest,
            @Valid @RequestBody TakeCreditRequest request) {
        String authHeader = servletRequest.getHeader("Authorization");
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        CreditResponse response = creditService.takeCredit(userId, authHeader, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{creditId}/repay")
    @Operation(summary = "Погасить кредит")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditPaymentResponse> repayCredit(
            HttpServletRequest servletRequest,
            @PathVariable Long creditId,
            @Valid @RequestBody RepayCreditRequest request) {
        String authHeader = servletRequest.getHeader("Authorization");
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        CreditPaymentResponse response = creditService.repayCredit(userId, authHeader, creditId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить информацию о кредите")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditResponse> getCreditById(
            HttpServletRequest servletRequest,
            @PathVariable Long id) {
        String authHeader = servletRequest.getHeader("Authorization");
        java.util.List<String> roles = jwtUtil.getRolesFromAuthHeader(authHeader);
        CreditResponse response = creditService.getCreditById(id);
        if (!roles.contains("Employee")) {
            String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
            if (!userId.equals(response.getOwnerId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: this credit does not belong to you");
            }
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Получить все кредиты")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CreditResponse>> getAllCredits(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader("Authorization");
        java.util.List<String> roles = jwtUtil.getRolesFromAuthHeader(authHeader);
        if (!roles.contains("Employee")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: Employee role required");
        }
        List<CreditResponse> credits = creditService.getAllCredits();
        return ResponseEntity.ok(credits);
    }

    @GetMapping("/my")
    @Operation(summary = "Получить мои кредиты")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CreditResponse>> getMyCreditsByToken(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader("Authorization");
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        List<CreditResponse> credits = creditService.getCreditsByUserId(userId);
        return ResponseEntity.ok(credits);
    }

    @GetMapping("/{creditId}/payments")
    @Operation(summary = "Получить историю платежей по кредиту")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CreditPaymentResponse>> getCreditPayments(
            HttpServletRequest servletRequest,
            @PathVariable Long creditId) {
        String authHeader = servletRequest.getHeader("Authorization");
        java.util.List<String> roles = jwtUtil.getRolesFromAuthHeader(authHeader);
        if (!roles.contains("Employee")) {
            String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
            CreditResponse credit = creditService.getCreditById(creditId);
            if (!userId.equals(credit.getOwnerId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: this credit does not belong to you");
            }
        }
        List<CreditPaymentResponse> payments = creditService.getCreditPayments(creditId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{creditId}/schedule")
    @Operation(summary = "Получить график платежей")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<PaymentScheduleResponse>> getPaymentSchedule(
            HttpServletRequest servletRequest,
            @PathVariable Long creditId) {
        String authHeader = servletRequest.getHeader("Authorization");
        java.util.List<String> roles = jwtUtil.getRolesFromAuthHeader(authHeader);
        if (!roles.contains("Employee")) {
            String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
            CreditResponse credit = creditService.getCreditById(creditId);
            if (!userId.equals(credit.getOwnerId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: this credit does not belong to you");
            }
        }
        List<PaymentScheduleResponse> schedule = creditService.getPaymentSchedule(creditId);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/{creditId}/statistics")
    @Operation(summary = "Получить статистику по кредиту")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditStatisticsResponse> getCreditStatistics(
            HttpServletRequest servletRequest,
            @PathVariable Long creditId) {
        String authHeader = servletRequest.getHeader("Authorization");
        java.util.List<String> roles = jwtUtil.getRolesFromAuthHeader(authHeader);
        if (!roles.contains("Employee")) {
            String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
            CreditResponse credit = creditService.getCreditById(creditId);
            if (!userId.equals(credit.getOwnerId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: this credit does not belong to you");
            }
        }
        CreditStatisticsResponse statistics = creditService.getCreditStatistics(creditId);
        return ResponseEntity.ok(statistics);
    }
}
