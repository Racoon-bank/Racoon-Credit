package com.credit.controller;

import com.credit.dto.CreditTariffRequest;
import com.credit.dto.CreditTariffResponse;
import com.credit.service.CreditTariffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/employee/tariffs")
@RequiredArgsConstructor
@Tag(name = "Тарифы кредитов (Сотрудник)")
public class CreditTariffController {

    private final CreditTariffService tariffService;

    @PostMapping
    @Operation(summary = "Создать новый тариф кредита")
    public ResponseEntity<CreditTariffResponse> createTariff(@Valid @RequestBody CreditTariffRequest request) {
        CreditTariffResponse response = tariffService.createTariff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить тариф по ID")
    public ResponseEntity<CreditTariffResponse> getTariffById(@PathVariable Long id) {
        CreditTariffResponse response = tariffService.getTariffById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить тариф")
    public ResponseEntity<Void> deleteTariff(@PathVariable Long id) {
        tariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }
}
