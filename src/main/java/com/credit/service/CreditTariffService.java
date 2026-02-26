package com.credit.service;

import com.credit.dto.CreditTariffRequest;
import com.credit.dto.CreditTariffResponse;
import com.credit.entity.CreditTariff;
import com.credit.repository.CreditTariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            throw new RuntimeException("Tariff with name " + request.getName() + " already exists");
        }

        CreditTariff tariff = new CreditTariff();
        tariff.setName(request.getName());
        tariff.setInterestRate(request.getInterestRate());

        CreditTariff savedTariff = creditTariffRepository.save(tariff);
        log.info("Credit tariff created with id: {}", savedTariff.getId());
        
        return mapToResponse(savedTariff);
    }

    // Получение тарифа по ID
    @Transactional(readOnly = true)
    public CreditTariffResponse getTariffById(Long id) {
        log.info("Fetching tariff with id: {}", id);
        CreditTariff tariff = creditTariffRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tariff not found with id: " + id));
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

    private CreditTariffResponse mapToResponse(CreditTariff tariff) {
        return new CreditTariffResponse(
                tariff.getId(),
                tariff.getName(),
                tariff.getInterestRate(),
                tariff.getCreatedAt()
        );
    }
}
