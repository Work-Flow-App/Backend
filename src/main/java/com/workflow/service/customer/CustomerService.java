package com.workflow.service.customer;

import com.workflow.common.exception.business.*;
import com.workflow.dto.customer.CustomerAddressDto;
import com.workflow.dto.customer.CustomerCreateRequest;
import com.workflow.dto.customer.CustomerUpdateRequest;
import com.workflow.dto.customer.CustomerResponse;
import com.workflow.entity.customer.Customer;
import com.workflow.entity.customer.CustomerAddress;
import com.workflow.entity.company.Company;
import com.workflow.repository.customer.CustomerRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.service.sequence.CompanyCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService implements ICustomerService {

    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final CompanyCounterService companyCounterService;

    @Override
    public CustomerResponse createCustomer(CustomerCreateRequest request, Long companyId) {
        Company company = companyRepository.getReferenceById(companyId);
        if (customerRepository.existsByCompanyIdAndName(companyId, request.getName()))
            throw new DuplicateNameException("A customer with this name already exists");
        if (request.getEmail() != null && customerRepository.existsByCompanyIdAndEmail(companyId, request.getEmail()))
            throw new DuplicateNameException("A customer with this email already exists");
        Customer customer = Customer.builder()
                .name(request.getName())
                .company(company)
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .mobile(request.getMobile())
                .address(toAddressEntity(request.getAddress()))
                .archived(false)
                .customerRef(companyCounterService.nextCustomerId(companyId))
                .build();
        customerRepository.save(customer);
        return mapToResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long customerId, Long companyId) {
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> c.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        return mapToResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers(Long companyId) {
        return customerRepository.findByCompanyId(companyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request, Long companyId) {
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> c.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        if (customerRepository.existsByCompanyIdAndNameAndIdNot(companyId, request.getName(), customerId))
            throw new DuplicateNameException("A customer with this name already exists");
        if (request.getEmail() != null && customerRepository.existsByCompanyIdAndEmailAndIdNot(companyId, request.getEmail(), customerId))
            throw new DuplicateNameException("A customer with this email already exists");
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setTelephone(request.getTelephone());
        customer.setMobile(request.getMobile());
        customer.setAddress(toAddressEntity(request.getAddress()));
        customer.setArchived(request.isArchived());
        customerRepository.save(customer);
        return mapToResponse(customer);
    }

    @Override
    public void deleteCustomer(Long customerId, Long companyId) {
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> c.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        customerRepository.delete(customer);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .customerRef(customer.getCustomerRef())
                .name(customer.getName())
                .email(customer.getEmail())
                .telephone(customer.getTelephone())
                .mobile(customer.getMobile())
                .address(toAddressDto(customer.getAddress()))
                .archived(customer.isArchived())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private CustomerAddress toAddressEntity(CustomerAddressDto dto) {
        if (dto == null) return null;
        return CustomerAddress.builder()
                .houseNumber(dto.getHouseNumber())
                .street(dto.getStreet())
                .city(dto.getCity())
                .county(dto.getCounty())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .build();
    }

    private CustomerAddressDto toAddressDto(CustomerAddress address) {
        if (address == null) return null;
        return CustomerAddressDto.builder()
                .houseNumber(address.getHouseNumber())
                .street(address.getStreet())
                .city(address.getCity())
                .county(address.getCounty())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }
}
