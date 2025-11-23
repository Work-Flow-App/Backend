package com.workflow.service.company;

import com.workflow.common.exception.customException.CompanyAlreadyExistsException;
import com.workflow.common.exception.customException.CompanyNotFoundException;
import com.workflow.dto.company.CompanyDashboardResponse;
import com.workflow.dto.company.CompanyProfileResponse;
import com.workflow.dto.company.CompanyProfileUpdateRequest;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService implements ICompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CompanyProfileResponse updateProfile(CompanyProfileUpdateRequest request, Long userId) {
        Company company = findCompanyByUserId(userId);

        // Check if name is being changed and if it already exists
        if (!company.getName().equalsIgnoreCase(request.name()) &&
                companyRepository.existsByNameIgnoreCase(request.name())) {
            throw new CompanyAlreadyExistsException("Company with name '" + request.name() + "' already exists");
        }

        // Update company fields
        company.setName(request.name());
        company.setAddressLine1(request.addressLine1());
        company.setAddressLine2(request.addressLine2());
        company.setAddressLine3(request.addressLine3());
        company.setTown(request.town());
        company.setCountry(request.country());
        company.setPostcode(request.postcode());
        company.setTelephone(request.telephone());
        company.setMobile(request.mobile());
        company.setFax(request.fax());
        company.setEmail(request.email());
        company.setContactEmail(request.contactEmail());
        company.setContactNumber(request.contactNumber());

        Company savedCompany = companyRepository.save(company);
        return CompanyProfileResponse.fromEntity(savedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfile(Long userId) {
        Company company = findCompanyByUserId(userId);
        return CompanyProfileResponse.fromEntity(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyDashboardResponse getDashboard(Long userId) {
        Company company = findCompanyByUserId(userId);

        long totalWorkers = companyRepository.countWorkers(company.getId());
        long activeWorkers = companyRepository.countActiveWorkers(company.getId());
        long archivedWorkers = totalWorkers - activeWorkers;
        long totalClients = companyRepository.countClients(company.getId());

        return new CompanyDashboardResponse(
                company.getId(),
                company.getName(),
                totalWorkers,
                totalClients,
                activeWorkers,
                archivedWorkers
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Company findCompanyByUserId(Long userId) {
        return companyRepository.findByUserIdAndNotArchived(userId)
                .orElseThrow(() -> new CompanyNotFoundException("No active company found for this user"));
    }
}