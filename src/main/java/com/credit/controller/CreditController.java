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
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        CreditResponse response = creditService.takeCredit(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{creditId}/repay")
    @Operation(summary = "Погасить кредит")
    public ResponseEntity<CreditPaymentResponse> repayCredit(
            @PathVariable Long creditId,
            @Valid @RequestBody RepayCreditRequest request) {
        CreditPaymentResponse response = creditService.repayCredit(creditId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить информацию о кредите")
    public ResponseEntity<CreditResponse> getCreditById(@PathVariable Long id) {
        CreditResponse response = creditService.getCreditById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Получить все кредиты")
    public ResponseEntity<List<CreditResponse>> getAllCredits() {
        List<CreditResponse> credits = creditService.getAllCredits();
        return ResponseEntity.ok(credits);
    }

    @GetMapping("/my")
    @Operation(summary = "Получить мои кредиты")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CreditResponse>> getMyCreditsByToken(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        List<CreditResponse> credits = creditService.getCreditsByUserId(userId);
        return ResponseEntity.ok(credits);
    }

    @GetMapping("/{creditId}/payments")
    @Operation(summary = "Получить историю платежей по кредиту")
    public ResponseEntity<List<CreditPaymentResponse>> getCreditPayments(@PathVariable Long creditId) {
        List<CreditPaymentResponse> payments = creditService.getCreditPayments(creditId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{creditId}/schedule")
    @Operation(summary = "Получить график платежей")
    public ResponseEntity<List<PaymentScheduleResponse>> getPaymentSchedule(@PathVariable Long creditId) {
        List<PaymentScheduleResponse> schedule = creditService.getPaymentSchedule(creditId);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/{creditId}/statistics")
    @Operation(summary = "Получить статистику по кредиту")
    public ResponseEntity<CreditStatisticsResponse> getCreditStatistics(@PathVariable Long creditId) {
        CreditStatisticsResponse statistics = creditService.getCreditStatistics(creditId);
        return ResponseEntity.ok(statistics);
    }
}
