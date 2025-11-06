package com.workflow.controller.client;

import com.workflow.dto.client.ClientCreateRequest;
import com.workflow.dto.client.ClientUpdateRequest;
import com.workflow.dto.client.ClientResponse;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.client.IClientService;
import com.workflow.repository.CompanyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final IClientService clientService;
    private final CompanyRepository companyRepository;


    @PostMapping
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody ClientCreateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        Company company = companyRepository.findByUserIdAndNotArchived(user.getId())
                .orElseThrow(() -> new RuntimeException("Company not found"));
        Long companyId = company.getId();

        ClientResponse response = clientService.createClient(request, companyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Company company = companyRepository.findByUserIdAndNotArchived(user.getId())
                .orElseThrow(() -> new RuntimeException("Company not found"));
        Long companyId = company.getId();
        
        List<ClientResponse> clients = clientService.getAllClients(companyId);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        Company company = companyRepository.findByUserIdAndNotArchived(user.getId())
                .orElseThrow(() -> new RuntimeException("Company not found"));
        Long companyId = company.getId();
        ClientResponse response = clientService.getClientById(id, companyId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientUpdateRequest request,
            Authentication authentication
    ) {
                User user = (User) authentication.getPrincipal();
        Company company = companyRepository.findByUserIdAndNotArchived(user.getId())
                .orElseThrow(() -> new RuntimeException("Company not found"));
        Long companyId = company.getId();

        ClientResponse response = clientService.updateClient(id, request, companyId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(
            @PathVariable Long id,
            Authentication authentication
    ) {
                User user = (User) authentication.getPrincipal();
        Company company = companyRepository.findByUserIdAndNotArchived(user.getId())
                .orElseThrow(() -> new RuntimeException("Company not found"));
        Long companyId = company.getId();
        clientService.deleteClient(id, companyId);
        return ResponseEntity.noContent().build();
    }
}
