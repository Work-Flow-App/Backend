package com.workflow.service.client;

import com.workflow.common.exception.business.*;
import com.workflow.dto.client.ClientCreateRequest;
import com.workflow.dto.client.ClientUpdateRequest;
import com.workflow.dto.client.ClientResponse;
import com.workflow.entity.Client;
import com.workflow.entity.Company;
import com.workflow.repository.ClientRepository;
import com.workflow.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService implements IClientService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;

    @Override
    public ClientResponse createClient(ClientCreateRequest request, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
        Client client = Client.builder()
                .name(request.getName())
                .company(company)
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .mobile(request.getMobile())
                .address(request.getAddress())
                .archived(false)
                .build();
        clientRepository.save(client);
        return mapToResponse(client);
    }

    @Override
    public ClientResponse getClientById(Long clientId, Long companyId) {
        Client client = clientRepository.findById(clientId)
                .filter(c -> c.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));
        return mapToResponse(client);
    }

    @Override
    public List<ClientResponse> getAllClients(Long companyId) {
        return clientRepository.findByCompanyId(companyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClientResponse updateClient(Long clientId, ClientUpdateRequest request, Long companyId) {
        Client client = clientRepository.findById(clientId)
                .filter(c -> c.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setTelephone(request.getTelephone());
        client.setMobile(request.getMobile());
        client.setAddress(request.getAddress());
        client.setArchived(request.isArchived());
        clientRepository.save(client);
        return mapToResponse(client);
    }

    @Override
    public void deleteClient(Long clientId, Long companyId) {
        Client client = clientRepository.findById(clientId)
                .filter(c -> c.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ClientNotFoundException("Client not found"));
        clientRepository.delete(client);
    }

    private ClientResponse mapToResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .telephone(client.getTelephone())
                .mobile(client.getMobile())
                .address(client.getAddress())
                .archived(client.isArchived())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }
}
