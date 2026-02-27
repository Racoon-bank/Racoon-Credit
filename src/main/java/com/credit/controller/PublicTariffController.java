package com.credit.controller;

import com.credit.dto.CreditTariffResponse;
import com.credit.service.CreditTariffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
@Tag(name = "Тарифы кредитов")
public class PublicTariffController {

    private final CreditTariffService tariffService;

    @GetMapping("/{id}")
    @Operation(summary = "Получить тариф по ID")
    public ResponseEntity<CreditTariffResponse> getTariffById(@PathVariable Long id) {
        CreditTariffResponse response = tariffService.getTariffById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Получить все доступные тарифы")
    public ResponseEntity<List<CreditTariffResponse>> getAllTariffs() {
        List<CreditTariffResponse> tariffs = tariffService.getAllTariffs();
        return ResponseEntity.ok(tariffs);
    }
}
