package com.workflow.controller.customer;

import com.workflow.dto.customer.CustomerCreateRequest;
import com.workflow.dto.customer.CustomerUpdateRequest;
import com.workflow.dto.customer.CustomerResponse;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.customer.ICustomerService;
import com.workflow.service.company.ICompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Customers")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final ICustomerService customerService;
    private final ICompanyService companyService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerCreateRequest request,
            Authentication auth
    ) {
        CustomerResponse response = customerService.createCustomer(request, getCompanyId(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers(Authentication auth) {
        List<CustomerResponse> customers = customerService.getAllCustomers(getCompanyId(auth));
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable Long id,
            Authentication auth
    ) {
        CustomerResponse response = customerService.getCustomerById(id, getCompanyId(auth));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request,
            Authentication auth
    ) {
        CustomerResponse response = customerService.updateCustomer(id, request, getCompanyId(auth));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id,
            Authentication auth
    ) {
        customerService.deleteCustomer(id, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }

    private Long getCompanyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }
}
