package com.workflow;

import com.workflow.common.constant.CompanyRole;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanyMember;
import com.workflow.entity.auth.User;
import com.workflow.repository.company.CompanyMemberRepository;
import com.workflow.service.storage.IStorageService;
import com.workflow.templates.pdf.invoice.InvoicePdfRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractControllerIntegrationTest {

    @MockBean
    protected IStorageService storageService;

    @MockBean
    protected InvoicePdfRenderer pdfRenderer;

    @Autowired
    protected CompanyMemberRepository companyMemberRepository;

    protected CompanyMember createCompanyMember(Company company, User user, CompanyRole role) {
        return companyMemberRepository.save(CompanyMember.builder()
                .company(company)
                .user(user)
                .companyRole(role)
                .active(true)
                .build());
    }
}