package com.credit.controller;

import com.credit.dto.CreditPaymentResponse;
import com.credit.dto.CreditResponse;
import com.credit.dto.CreditStatisticsResponse;
import com.credit.dto.PaymentScheduleResponse;
import com.credit.dto.RepayCreditRequest;
import com.credit.dto.TakeCreditRequest;
import com.credit.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @PostMapping
    @Operation(summary = "Взять кредит")
    public ResponseEntity<CreditResponse> takeCredit(@Valid @RequestBody TakeCreditRequest request) {
        CreditResponse response = creditService.takeCredit(request);
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
    @Operation(summary = "Получить статистику по кредиту (переплата, проценты)")
    public ResponseEntity<CreditStatisticsResponse> getCreditStatistics(@PathVariable Long creditId) {
        CreditStatisticsResponse statistics = creditService.getCreditStatistics(creditId);
        return ResponseEntity.ok(statistics);
    }
}
