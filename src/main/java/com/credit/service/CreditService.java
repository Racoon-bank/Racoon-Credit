package com.credit.service;

import com.credit.dto.*;
import com.credit.entity.*;
import com.credit.repository.ClientRepository;
import com.credit.repository.CreditPaymentRepository;
import com.credit.repository.CreditRepository;
import com.credit.repository.CreditTariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {

    private final CreditRepository creditRepository;
    private final ClientRepository clientRepository;
    private final CreditTariffRepository tariffRepository;
    private final CreditPaymentRepository paymentRepository;

    @Transactional
    public CreditResponse takeCredit(TakeCreditRequest request) {
        log.info("Client {} is taking credit", request.getClientId());

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + request.getClientId()));

        if (client.getBlocked()) {
            throw new RuntimeException("Client is blocked");
        }

        CreditTariff tariff = tariffRepository.findById(request.getTariffId())
                .orElseThrow(() -> new RuntimeException("Tariff not found with id: " + request.getTariffId()));

        BigDecimal interestAmount = request.getAmount()
                .multiply(tariff.getInterestRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalAmount = request.getAmount().add(interestAmount);

        BigDecimal dailyPayment = totalAmount
                .divide(BigDecimal.valueOf(request.getDurationDays()), 2, RoundingMode.HALF_UP);

        Credit credit = new Credit();
        credit.setClient(client);
        credit.setTariff(tariff);
        credit.setAmount(request.getAmount());
        credit.setRemainingAmount(totalAmount);
        credit.setDailyPayment(dailyPayment);
        credit.setStatus(CreditStatus.ACTIVE);
        credit.setIssueDate(LocalDateTime.now());
        credit.setNextPaymentDate(LocalDateTime.now().plusDays(1));

        Credit savedCredit = creditRepository.save(credit);
        log.info("Credit created with id: {}", savedCredit.getId());

        return mapToResponse(savedCredit);
    }

    @Transactional
    public CreditPaymentResponse repayCredit(Long creditId, RepayCreditRequest request) {
        log.info("Repaying credit {} with amount {}", creditId, request.getAmount());

        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Credit not found with id: " + creditId));

        if (credit.getStatus() != CreditStatus.ACTIVE) {
            throw new RuntimeException("Credit is not active");
        }

        BigDecimal paymentAmount = request.getAmount();
        if (paymentAmount.compareTo(credit.getRemainingAmount()) > 0) {
            paymentAmount = credit.getRemainingAmount();
        }

        CreditPayment payment = new CreditPayment();
        payment.setCredit(credit);
        payment.setAmount(paymentAmount);
        payment.setPaymentType(PaymentType.MANUAL_REPAYMENT);
        payment.setPaymentDate(LocalDateTime.now());

        CreditPayment savedPayment = paymentRepository.save(payment);

        BigDecimal newRemaining = credit.getRemainingAmount().subtract(paymentAmount);
        credit.setRemainingAmount(newRemaining);

        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(CreditStatus.PAID_OFF);
            credit.setRemainingAmount(BigDecimal.ZERO);
            log.info("Credit {} is fully paid off", creditId);
        }

        creditRepository.save(credit);

        return mapPaymentToResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public CreditResponse getCreditById(Long id) {
        log.info("Fetching credit with id: {}", id);
        Credit credit = creditRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit not found with id: " + id));
        return mapToResponse(credit);
    }

    @Transactional(readOnly = true)
    public List<CreditResponse> getCreditsByClientId(Long clientId) {
        log.info("Fetching credits for client: {}", clientId);
        return creditRepository.findByClientId(clientId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditResponse> getAllCredits() {
        log.info("Fetching all credits");
        return creditRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditPaymentResponse> getCreditPayments(Long creditId) {
        log.info("Fetching payments for credit: {}", creditId);
        return paymentRepository.findByCreditIdOrderByPaymentDateDesc(creditId).stream()
                .map(this::mapPaymentToResponse)
                .collect(Collectors.toList());
    }

    private CreditResponse mapToResponse(Credit credit) {
        return new CreditResponse(
                credit.getId(),
                credit.getClient().getId(),
                credit.getClient().getFirstName() + " " + credit.getClient().getLastName(),
                credit.getTariff().getId(),
                credit.getTariff().getName(),
                credit.getTariff().getInterestRate(),
                credit.getAmount(),
                credit.getRemainingAmount(),
                credit.getDailyPayment(),
                credit.getStatus(),
                credit.getIssueDate(),
                credit.getNextPaymentDate(),
                credit.getCreatedAt(),
                credit.getUpdatedAt()
        );
    }

    private CreditPaymentResponse mapPaymentToResponse(CreditPayment payment) {
        return new CreditPaymentResponse(
                payment.getId(),
                payment.getCredit().getId(),
                payment.getAmount(),
                payment.getPaymentType(),
                payment.getPaymentDate(),
                payment.getCreatedAt()
        );
    }
}
