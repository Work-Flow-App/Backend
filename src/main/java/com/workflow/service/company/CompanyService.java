package com.workflow.service.company;

import com.workflow.common.exception.business.CompanyAlreadyExistsException;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.dto.company.CompanyAddressRequest;
import com.workflow.dto.company.CompanyBankDetailsRequest;
import com.workflow.dto.company.CompanyDashboardResponse;
import com.workflow.dto.company.CompanyProfileResponse;
import com.workflow.dto.company.CompanyProfileUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanyAddress;
import com.workflow.entity.company.CompanyBankDetails;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.auth.UserRepository;
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
        if (request.address() != null) {
            CompanyAddressRequest a = request.address();
            company.setAddress(CompanyAddress.builder()
                    .addressLine1(a.addressLine1())
                    .addressLine2(a.addressLine2())
                    .addressLine3(a.addressLine3())
                    .town(a.town())
                    .country(a.country())
                    .postcode(a.postcode())
                    .build());
        }
        company.setTelephone(request.telephone());
        company.setMobile(request.mobile());
        company.setFax(request.fax());
        company.setEmail(request.email());
        company.setContactEmail(request.contactEmail());
        company.setContactNumber(request.contactNumber());
        company.setVatNumber(request.vatNumber());
        if (request.currency() != null) {
            company.setCurrency(request.currency());
        }

        if (request.bankDetails() != null) {
            CompanyBankDetailsRequest bd = request.bankDetails();
            CompanyBankDetails bankDetails = company.getBankDetails();
            if (bankDetails == null) {
                bankDetails = CompanyBankDetails.builder().company(company).build();
                company.setBankDetails(bankDetails);
            }
            bankDetails.setBankName(bd.bankName());
            bankDetails.setAccountName(bd.accountName());
            bankDetails.setAccountNo(bd.accountNo());
            bankDetails.setSortCode(bd.sortCode());
        }

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