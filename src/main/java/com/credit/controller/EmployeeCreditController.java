package com.credit.controller;

import com.credit.dto.CreditPaymentResponse;
import com.credit.dto.CreditResponse;
import com.credit.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee/credits")
@RequiredArgsConstructor
@Tag(name = "Кредиты (Сотрудник)")
public class EmployeeCreditController {

    private final CreditService creditService;

    @GetMapping
    @Operation(summary = "Получить все кредиты")
    public ResponseEntity<List<CreditResponse>> getAllCredits() {
        List<CreditResponse> credits = creditService.getAllCredits();
        return ResponseEntity.ok(credits);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить детальную информацию о кредите")
    public ResponseEntity<CreditResponse> getCreditById(@PathVariable Long id) {
        CreditResponse response = creditService.getCreditById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Получить все кредиты клиента")
    public ResponseEntity<List<CreditResponse>> getCreditsByClientId(@PathVariable Long clientId) {
        List<CreditResponse> credits = creditService.getCreditsByClientId(clientId);
        return ResponseEntity.ok(credits);
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "Получить историю платежей по кредиту")
    public ResponseEntity<List<CreditPaymentResponse>> getCreditPayments(@PathVariable Long id) {
        List<CreditPaymentResponse> payments = creditService.getCreditPayments(id);
        return ResponseEntity.ok(payments);
    }
}
