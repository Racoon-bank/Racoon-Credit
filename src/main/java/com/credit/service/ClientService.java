package com.credit.service;

import com.credit.dto.ClientRequest;
import com.credit.dto.ClientResponse;
import com.credit.entity.Client;
import com.credit.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional
    public ClientResponse createClient(ClientRequest request) {
        log.info("Creating new client with email: {}", request.getEmail());
        
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Client with email " + request.getEmail() + " already exists");
        }

        Client client = new Client();
        client.setFirstName(request.getFirstName());
        client.setLastName(request.getLastName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setBlocked(false);

        Client savedClient = clientRepository.save(client);
        log.info("Client created with id: {}", savedClient.getId());
        
        return mapToResponse(savedClient);
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        log.info("Fetching client with id: {}", id);
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
        return mapToResponse(client);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        log.info("Fetching all clients");
        return clientRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ClientResponse mapToResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getPhone(),
                client.getCreatedAt()
        );
    }
}
