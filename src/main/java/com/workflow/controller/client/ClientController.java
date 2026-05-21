package com.workflow.controller.client;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.client.ClientCreateRequest;
import com.workflow.dto.client.ClientUpdateRequest;
import com.workflow.dto.client.ClientResponse;
import com.workflow.service.client.IClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Clients")
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final IClientService clientService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PostMapping
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody ClientCreateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientService.createClient(request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients(Authentication auth) {
        return ResponseEntity.ok(clientService.getAllClients(getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(
            @PathVariable Long id,
            Authentication auth
    ) {
        return ResponseEntity.ok(clientService.getClientById(id, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(clientService.updateClient(id, request, getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(
            @PathVariable Long id,
            Authentication auth
    ) {
        clientService.deleteClient(id, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }
}
