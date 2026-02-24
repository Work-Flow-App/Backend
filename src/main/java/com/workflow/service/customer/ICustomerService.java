package com.workflow.service.customer;

import com.workflow.dto.customer.CustomerCreateRequest;
import com.workflow.dto.customer.CustomerUpdateRequest;
import com.workflow.dto.customer.CustomerResponse;

import java.util.List;

public interface ICustomerService {
    CustomerResponse createCustomer(CustomerCreateRequest request, Long companyId);
    CustomerResponse getCustomerById(Long customerId, Long companyId);
    List<CustomerResponse> getAllCustomers(Long companyId);
    CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request, Long companyId);
    void deleteCustomer(Long customerId, Long companyId);
}