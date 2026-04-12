package com.credit.controller;

import com.credit.dto.CreditApplicationDecisionRequest;
import com.credit.dto.CreditApplicationResponse;
import com.credit.dto.CreditPaymentResponse;
import com.credit.dto.CreditRatingResponse;
import com.credit.dto.CreditResponse;
import com.credit.dto.CreditStatisticsResponse;
import com.credit.dto.OverduePaymentResponse;
import com.credit.dto.PaymentScheduleResponse;
import com.credit.dto.RepayCreditRequest;
import com.credit.dto.TakeCreditRequest;
import com.credit.dto.TakeCreditResultResponse;
import com.credit.idempotency.Idempotent;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@Tag(name = "Управление кредитами")
public class CreditController {

    private final CreditService creditService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @Idempotent
    // 2.
    @Operation(summary = "Взять кредит")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TakeCreditResultResponse> takeCredit(
            HttpServletRequest servletRequest,
            @Valid @RequestBody TakeCreditRequest request) {
        String authHeader = servletRequest.getHeader("Authorization");
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        TakeCreditResultResponse response = creditService.takeCredit(userId, authHeader, request);
        HttpStatus status = "APPLICATION_CREATED".equals(response.getResultType()) ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/{creditId}/repay")
    @Idempotent
    // 2.
    @Operation(summary = "Погасить кредит")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditPaymentResponse> repayCredit(
            HttpServletRequest servletRequest,
            @PathVariable Long creditId,
            @Valid @RequestBody RepayCreditRequest request) {
        String authHeader = servletRequest.getHeader("Authorization");
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(creditService.repayCredit(userId, authHeader, creditId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить информацию о кредите")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditResponse> getCreditById(HttpServletRequest servletRequest, @PathVariable Long id) {
        String authHeader = servletRequest.getHeader("Authorization");
        List<String> roles = jwtUtil.getRolesFromAuthHeader(authHeader);
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
    @Operation(summary = "Получить все кредиты (Сотрудник)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CreditResponse>> getAllCredits(HttpServletRequest servletRequest) {
        requireEmployeeRole(servletRequest);
        return ResponseEntity.ok(creditService.getAllCredits());
    }

    @GetMapping("/my")
    @Operation(summary = "Получить мои кредиты")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CreditResponse>> getMyCreditsByToken(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader("Authorization");
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(creditService.getCreditsByUserId(userId));
    }

    @GetMapping("/my/applications")
    @Operation(summary = "Получить мои заявки на кредит")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CreditApplicationResponse>> getMyCreditApplications(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader("Authorization");
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(creditService.getMyCreditApplications(userId));
    }

    @GetMapping("/applications/pending")
    @Operation(summary = "Получить заявки на рассмотрение сотрудником")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CreditApplicationResponse>> getPendingCreditApplications(HttpServletRequest servletRequest) {
        requireEmployeeRole(servletRequest);
        return ResponseEntity.ok(creditService.getPendingCreditApplications());
    }

    @PostMapping("/applications/{applicationId}/approve")
    @Idempotent
    // 2.
    @Operation(summary = "Одобрить заявку на кредит")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TakeCreditResultResponse> approveCreditApplication(
            HttpServletRequest servletRequest,
            @PathVariable Long applicationId,
            @RequestBody(required = false) CreditApplicationDecisionRequest request) {
        requireEmployeeRole(servletRequest);
        String employeeId = jwtUtil.getUserIdFromAuthHeader(servletRequest.getHeader("Authorization"));
        return ResponseEntity.ok(creditService.approveCreditApplication(applicationId, employeeId, request));
    }

    @PostMapping("/applications/{applicationId}/reject")
    @Idempotent
    // 2.
    @Operation(summary = "Отклонить заявку на кредит")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditApplicationResponse> rejectCreditApplication(
            HttpServletRequest servletRequest,
            @PathVariable Long applicationId,
            @RequestBody(required = false) CreditApplicationDecisionRequest request) {
        requireEmployeeRole(servletRequest);
        String employeeId = jwtUtil.getUserIdFromAuthHeader(servletRequest.getHeader("Authorization"));
        return ResponseEntity.ok(creditService.rejectCreditApplication(applicationId, employeeId, request));
    }

    @GetMapping("/my/rating")
    @Operation(summary = "Получить мой кредитный рейтинг")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditRatingResponse> getMyCreditRating(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader("Authorization");
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(creditService.getUserCreditRating(userId));
    }

    @GetMapping("/users/{userId}/rating")
    @Operation(summary = "Получить кредитный рейтинг клиента")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditRatingResponse> getUserCreditRating(HttpServletRequest servletRequest, @PathVariable String userId) {
        requireEmployeeRole(servletRequest);
        return ResponseEntity.ok(creditService.getUserCreditRating(userId));
    }

    @GetMapping("/{creditId}/payments")
    @Operation(summary = "Получить историю платежей по кредиту")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<CreditPaymentResponse>> getCreditPayments(HttpServletRequest servletRequest, @PathVariable Long creditId) {
        assertCanAccessCredit(servletRequest, creditId);
        return ResponseEntity.ok(creditService.getCreditPayments(creditId));
    }

    @GetMapping("/{creditId}/schedule")
    @Operation(summary = "Получить график платежей")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<PaymentScheduleResponse>> getPaymentSchedule(HttpServletRequest servletRequest, @PathVariable Long creditId) {
        assertCanAccessCredit(servletRequest, creditId);
        return ResponseEntity.ok(creditService.getPaymentSchedule(creditId));
    }

    @GetMapping("/{creditId}/overdue-payments")
    @Operation(summary = "Получить просроченные платежи по кредиту")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<OverduePaymentResponse>> getOverduePayments(HttpServletRequest servletRequest, @PathVariable Long creditId) {
        assertCanAccessCredit(servletRequest, creditId);
        return ResponseEntity.ok(creditService.getOverduePayments(creditId));
    }

    @GetMapping("/my/overdue-payments")
    @Operation(summary = "Получить мои просроченные платежи")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<OverduePaymentResponse>> getMyOverduePayments(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader("Authorization");
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        return ResponseEntity.ok(creditService.getMyOverduePayments(userId));
    }

    @GetMapping("/{creditId}/statistics")
    @Operation(summary = "Получить статистику по кредиту")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditStatisticsResponse> getCreditStatistics(HttpServletRequest servletRequest, @PathVariable Long creditId) {
        assertCanAccessCredit(servletRequest, creditId);
        return ResponseEntity.ok(creditService.getCreditStatistics(creditId));
    }

    private void assertCanAccessCredit(HttpServletRequest servletRequest, Long creditId) {
        String authHeader = servletRequest.getHeader("Authorization");
        List<String> roles = jwtUtil.getRolesFromAuthHeader(authHeader);
        if (roles.contains("Employee")) {
            return;
        }
        String userId = jwtUtil.getUserIdFromAuthHeader(authHeader);
        CreditResponse credit = creditService.getCreditById(creditId);
        if (!userId.equals(credit.getOwnerId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: this credit does not belong to you");
        }
    }

    private void requireEmployeeRole(HttpServletRequest servletRequest) {
        List<String> roles = jwtUtil.getRolesFromAuthHeader(servletRequest.getHeader("Authorization"));
        if (!roles.contains("Employee")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: Employee role required");
        }
    }
}
