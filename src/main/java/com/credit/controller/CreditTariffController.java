package com.credit.controller;

import com.credit.dto.CreditTariffRequest;
import com.credit.dto.CreditTariffResponse;
import com.credit.idempotency.Idempotent;
import com.credit.service.CreditTariffService;
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


@RestController
@RequestMapping("/api/employee/tariffs")
@RequiredArgsConstructor
@Tag(name = "Тарифы кредитов (Сотрудник)")
public class CreditTariffController {

    private final CreditTariffService tariffService;
    private final JwtUtil jwtUtil;

    private void requireEmployeeRole(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader("Authorization");
        java.util.List<String> roles = jwtUtil.getRolesFromAuthHeader(authHeader);
        if (!roles.contains("Employee")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied: Employee role required");
        }
    }

    @PostMapping
    @Idempotent
    // 2. Повторное создание тарифа с тем же ключом не должно создавать дубликат.
    @Operation(summary = "Создать новый тариф кредита (Сотрудник)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CreditTariffResponse> createTariff(
            HttpServletRequest servletRequest,
            @Valid @RequestBody CreditTariffRequest request) {
        requireEmployeeRole(servletRequest);
        CreditTariffResponse response = tariffService.createTariff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @Idempotent
    // 2. Повторное удаление тарифа с тем же ключом должно возвращать уже сохраненный результат.
    @Operation(summary = "Удалить тариф (Сотрудник)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteTariff(
            HttpServletRequest servletRequest,
            @PathVariable Long id) {
        requireEmployeeRole(servletRequest);
        tariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }
}
