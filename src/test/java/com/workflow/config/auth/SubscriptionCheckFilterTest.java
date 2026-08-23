package com.workflow.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.config.properties.PaddleConfigProperties;
import com.workflow.entity.auth.User;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.company.ICompanyService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionCheckFilterTest {

    @Mock
    private ICompanyService companyService;

    @Mock
    private CompanySubscriptionRepository subscriptionRepository;

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private FilterChain filterChain;

    private PaddleConfigProperties paddleProps;
    private SubscriptionCheckFilter filter;

    private static final Long USER_ID = 1L;
    private static final Long COMPANY_ID = 10L;

    @BeforeEach
    void setUp() {
        paddleProps = new PaddleConfigProperties();
        paddleProps.setPastDueGraceDays(3);
        filter = new SubscriptionCheckFilter(
                companyService,
                subscriptionRepository,
                workerRepository,
                paddleProps,
                new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Role role) {
        User user = User.builder().id(USER_ID).role(role).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private CompanySubscription subscriptionWithStatus(SubscriptionStatus status) {
        return CompanySubscription.builder()
                .status(status)
                .trialEndsAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .currentPeriodEnd(LocalDateTime.now(ZoneOffset.UTC).minusDays(10))
                .build();
    }

    @Test
    void getRequest_companyRole_expiredSubscription_passesThroughWithoutLookups() throws Exception {
        authenticateAs(Role.COMPANY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(402);
        verifyNoInteractions(companyService, subscriptionRepository, workerRepository);
    }

    @Test
    void postRequest_companyRole_expiredSubscription_writes402() throws Exception {
        authenticateAs(Role.COMPANY);
        Company company = Company.builder().id(COMPANY_ID).build();
        when(companyService.findCompanyByUserId(USER_ID)).thenReturn(company);
        when(subscriptionRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(subscriptionWithStatus(SubscriptionStatus.EXPIRED)));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(402);
        assertThat(response.getContentAsString()).contains("\"status\":402");
        assertThat(response.getContentAsString()).contains("\"error\":\"Payment Required\"");
        assertThat(response.getContentAsString()).contains("\"message\"");
        assertThat(response.getContentAsString()).contains("\"path\":\"/api/v1/jobs\"");
    }

    @Test
    void postRequest_companyRole_activeSubscription_passesThrough() throws Exception {
        authenticateAs(Role.COMPANY);
        Company company = Company.builder().id(COMPANY_ID).build();
        when(companyService.findCompanyByUserId(USER_ID)).thenReturn(company);
        when(subscriptionRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(subscriptionWithStatus(SubscriptionStatus.ACTIVE)));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(402);
    }

    @Test
    void postRequest_workerRole_expiredSubscription_writes402() throws Exception {
        authenticateAs(Role.WORKER);
        Company company = Company.builder().id(COMPANY_ID).build();
        Worker worker = Worker.builder().id(100L).company(company).build();
        when(workerRepository.findByUserId(USER_ID)).thenReturn(Optional.of(worker));
        when(subscriptionRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(subscriptionWithStatus(SubscriptionStatus.EXPIRED)));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/worker/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(402);
    }

    @Test
    void getRequest_workerRole_expiredSubscription_passesThrough() throws Exception {
        authenticateAs(Role.WORKER);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/worker/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(402);
    }

    @Test
    void postRequest_workerRole_workerNotFound_failsOpen() throws Exception {
        authenticateAs(Role.WORKER);
        when(workerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/worker/jobs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(402);
    }

    @Test
    void postToSubscriptionManagementUri_expiredSubscription_passesThrough() throws Exception {
        authenticateAs(Role.COMPANY);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/companies/subscription/checkout");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(402);
        verifyNoInteractions(companyService, subscriptionRepository, workerRepository);
    }

    @Test
    void postRequest_adminRole_passesThroughUnconditionally() throws Exception {
        authenticateAs(Role.ADMIN);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/anything");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(402);
        verifyNoInteractions(companyService, subscriptionRepository, workerRepository);
    }
}
