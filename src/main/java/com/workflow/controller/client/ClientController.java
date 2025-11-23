package com.workflow.controller.client;

import com.workflow.dto.client.ClientCreateRequest;
import com.workflow.dto.client.ClientUpdateRequest;
import com.workflow.dto.client.ClientResponse;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.client.IClientService;
import com.workflow.service.company.ICompanyService;
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
    private final ICompanyService companyService;

    private Long getCompanyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody ClientCreateRequest request,
            Authentication auth
    ) {
        ClientResponse response = clientService.createClient(request, getCompanyId(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients(Authentication auth) {
        List<ClientResponse> clients = clientService.getAllClients(getCompanyId(auth));
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(
            @PathVariable Long id,
            Authentication auth
    ) {
        ClientResponse response = clientService.getClientById(id, getCompanyId(auth));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientUpdateRequest request,
            Authentication auth
    ) {
        ClientResponse response = clientService.updateClient(id, request, getCompanyId(auth));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(
            @PathVariable Long id,
            Authentication auth
    ) {
        clientService.deleteClient(id, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }
}
