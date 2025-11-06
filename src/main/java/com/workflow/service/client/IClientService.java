package com.workflow.service.client;

import com.workflow.dto.client.ClientCreateRequest;
import com.workflow.dto.client.ClientUpdateRequest;
import com.workflow.dto.client.ClientResponse;

import java.util.List;

public interface IClientService {
    ClientResponse createClient(ClientCreateRequest request, Long companyId);
    ClientResponse getClientById(Long clientId, Long companyId);
    List<ClientResponse> getAllClients(Long companyId);
    ClientResponse updateClient(Long clientId, ClientUpdateRequest request, Long companyId);
    void deleteClient(Long clientId, Long companyId);
}
