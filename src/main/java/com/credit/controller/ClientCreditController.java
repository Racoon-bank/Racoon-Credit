package com.credit.controller;

import com.credit.dto.*;
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
@RequestMapping("/api/client/credits")
@RequiredArgsConstructor
@Tag(name = "Кредиты (Клиент)")
public class ClientCreditController {

    private final CreditService creditService;

    @PostMapping("/take")
    @Operation(summary = "Взять кредит")
    public ResponseEntity<CreditResponse> takeCredit(@Valid @RequestBody TakeCreditRequest request) {
        CreditResponse response = creditService.takeCredit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/repay")
    @Operation(summary = "Погасить кредит")
    public ResponseEntity<CreditPaymentResponse> repayCredit(
            @PathVariable Long id,
            @Valid @RequestBody RepayCreditRequest request) {
        CreditPaymentResponse response = creditService.repayCredit(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить информацию о кредите")
    public ResponseEntity<CreditResponse> getCreditById(@PathVariable Long id) {
        CreditResponse response = creditService.getCreditById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "Получить историю платежей по кредиту")
    public ResponseEntity<List<CreditPaymentResponse>> getCreditPayments(@PathVariable Long id) {
        List<CreditPaymentResponse> payments = creditService.getCreditPayments(id);
        return ResponseEntity.ok(payments);
    }
}
