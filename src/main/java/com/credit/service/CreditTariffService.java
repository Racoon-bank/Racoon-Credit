package com.credit.service;

import com.credit.dto.CreditTariffRequest;
import com.credit.dto.CreditTariffResponse;
import com.credit.entity.CreditTariff;
import com.credit.repository.CreditTariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditTariffService {

    private final CreditTariffRepository creditTariffRepository;
    // Создание нового кредитного тарифа    
    @Transactional
    public CreditTariffResponse createTariff(CreditTariffRequest request) {
        log.info("Creating new credit tariff with name: {}", request.getName());
        
        if (creditTariffRepository.existsByName(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tariff with name " + request.getName() + " already exists");
        }

        CreditTariff tariff = new CreditTariff();
        tariff.setName(request.getName());
        tariff.setInterestRate(request.getInterestRate().divide(java.math.BigDecimal.valueOf(100)));
        tariff.setDueDate(request.getDueDate());
        tariff.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        CreditTariff savedTariff = creditTariffRepository.save(tariff);
        log.info("Credit tariff created with id: {}", savedTariff.getId());
        
        return mapToResponse(savedTariff);
    }

    // Получение тарифа по ID
    @Transactional(readOnly = true)
    public CreditTariffResponse getTariffById(Long id) {
        log.info("Fetching tariff with id: {}", id);
        CreditTariff tariff = creditTariffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tariff not found with id: " + id));
        return mapToResponse(tariff);
    }

    // Получение списка всех тарифов
    @Transactional(readOnly = true)
    public List<CreditTariffResponse> getAllTariffs() {
        log.info("Fetching all tariffs");
        return creditTariffRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Удаление тарифа
    @Transactional
    public void deleteTariff(Long id) {
        log.info("Deleting tariff with id: {}", id);
        CreditTariff tariff = creditTariffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tariff not found with id: " + id));
        creditTariffRepository.delete(tariff);
        log.info("Tariff with id {} deleted successfully", id);
    }

    private CreditTariffResponse mapToResponse(CreditTariff tariff) {
        return new CreditTariffResponse(
                tariff.getId(),
                tariff.getName(),
                tariff.getInterestRate().multiply(java.math.BigDecimal.valueOf(100)).stripTrailingZeros(),
                tariff.getDueDate(),
                tariff.getIsActive(),
                tariff.getCreatedAt()
        );
    }
}
