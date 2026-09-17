package com.workflow.controller.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.AbstractControllerIntegrationTest;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.PlanType;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.SubscriptionStatus;
import com.workflow.dto.company.UpdateSubscriptionAddonsRequest;
import com.workflow.dto.paddle.PaddleSubscriptionResponse;
import com.workflow.entity.auth.User;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanySubscription;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.company.CompanySubscriptionRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.auth.JwtService;
import com.workflow.service.paddle.IPaddleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PATCH /api/v1/companies/subscription/addons.
 * IPaddleService is mocked (external system) — everything else runs against the real
 * service/repository stack + H2, same pattern as the other controller integration tests.
 */
class SubscriptionAddonsControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySubscriptionRepository subscriptionRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private IPaddleService paddleService;

    private User adminUser;
    private Company company;
    private CompanySubscription subscription;
    private String adminToken;
    private String managerToken;

    @BeforeEach
    void setUp() {
        workerRepository.deleteAll();
        subscriptionRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("adns_adm_" + shortId())
                .password(passwordEncoder.encode("password123"))
                .email("addons_admin@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build());

        User managerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("adns_mgr_" + shortId())
                .password(passwordEncoder.encode("password123"))
                .email("addons_manager@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build());

        company = companyRepository.save(Company.builder()
                .name("Addons Test Co")
                .user(adminUser)
                .email("addons_co@example.com")
                .telephone("1234567890")
                .archived(false)
                .build());

        createCompanyMember(company, adminUser, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(company, managerUser, CompanyRole.MANAGER);

        adminToken = jwtService.generateToken(adminUser);
        managerToken = jwtService.generateToken(managerUser);
    }

    // users.username is varchar(50) — full UUIDs (36 chars) blow past that once prefixed, so use a
    // short random suffix instead.
    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private CompanySubscription baseSubscription(SubscriptionStatus status, PlanType planType) {
        return subscriptionRepository.save(CompanySubscription.builder()
                .company(company)
                .status(status)
                .planType(planType)
                .trialEndsAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(14))
                .paddleSubscriptionId("sub_addons_test")
                .paddleCustomerId("cust_addons_test")
                .extraUserSeats(2)
                .extraStorageBlocks(1)
                .build());
    }

    private String body(Integer extraSeats, Integer extraStorageBlocks) throws Exception {
        return objectMapper.writeValueAsString(new UpdateSubscriptionAddonsRequest(extraSeats, extraStorageBlocks));
    }

    // ============= happy path =============

    @Test
    void shouldUpdateAddonsSuccessfully() throws Exception {
        subscription = baseSubscription(SubscriptionStatus.ACTIVE, PlanType.STARTER);
        when(paddleService.updateSubscriptionAddons(eq("sub_addons_test"), eq(PlanType.STARTER), anyInt(), anyInt()))
                .thenReturn(new PaddleSubscriptionResponse(null));

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planType").value("STARTER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.extraUserSeats").value(5))
                .andExpect(jsonPath("$.extraStorageBlocks").value(3));

        verify(paddleService).updateSubscriptionAddons("sub_addons_test", PlanType.STARTER, 5, 3);

        CompanySubscription reloaded = subscriptionRepository.findById(subscription.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getExtraUserSeats()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(reloaded.getExtraStorageBlocks()).isEqualTo(3);
    }

    @Test
    void shouldAllowPartialUpdate_OnlySeats() throws Exception {
        subscription = baseSubscription(SubscriptionStatus.ACTIVE, PlanType.STARTER);
        when(paddleService.updateSubscriptionAddons(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PaddleSubscriptionResponse(null));

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(6, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extraUserSeats").value(6))
                .andExpect(jsonPath("$.extraStorageBlocks").value(1)); // unchanged
    }

    @Test
    void shouldBeNoOp_WhenRequestedValuesMatchCurrent() throws Exception {
        subscription = baseSubscription(SubscriptionStatus.ACTIVE, PlanType.STARTER);

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extraUserSeats").value(2))
                .andExpect(jsonPath("$.extraStorageBlocks").value(1));

        verify(paddleService, never()).updateSubscriptionAddons(any(), any(), anyInt(), anyInt());
    }

    // ============= auth / role guard rails =============

    @Test
    void shouldReturn403ForbiddenWithoutToken() throws Exception {
        subscription = baseSubscription(SubscriptionStatus.ACTIVE, PlanType.STARTER);

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403Forbidden_ForNonAdminCompanyRole() throws Exception {
        subscription = baseSubscription(SubscriptionStatus.ACTIVE, PlanType.STARTER);

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5, null)))
                .andExpect(status().isForbidden());
    }

    // ============= state guard rails =============

    @Test
    void shouldReturn400_WhenNoPaddleSubscriptionId() throws Exception {
        subscription = subscriptionRepository.save(CompanySubscription.builder()
                .company(company)
                .status(SubscriptionStatus.ACTIVE)
                .planType(PlanType.STARTER)
                .trialEndsAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(14))
                .paddleSubscriptionId(null)
                .extraUserSeats(0)
                .extraStorageBlocks(0)
                .build());

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_WhenPlanIsFree() throws Exception {
        subscription = baseSubscription(SubscriptionStatus.ACTIVE, PlanType.FREE);

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn402_WhenSubscriptionCancelled() throws Exception {
        subscription = baseSubscription(SubscriptionStatus.CANCELLED, PlanType.STARTER);

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(5, null)))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void shouldReturn400_ForNegativeExtraSeats() throws Exception {
        subscription = baseSubscription(SubscriptionStatus.ACTIVE, PlanType.STARTER);

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(-1, null)))
                .andExpect(status().isBadRequest());
    }

    // ============= decrease-conflict guard rails =============

    @Test
    void shouldReturn409_WhenSeatDecreaseWouldDropBelowActiveWorkerCount() throws Exception {
        // STARTER tier max-users=3 (application.yml); extraUserSeats=2 here -> effective max=5.
        // 4 active workers fit under 5, but a decrease to extraSeats=0 -> new max=3 < 4 in use.
        subscription = baseSubscription(SubscriptionStatus.ACTIVE, PlanType.STARTER);
        for (int i = 0; i < 4; i++) {
            User workerUser = userRepository.save(User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .username("adns_wkr_" + i + "_" + shortId())
                    .password(passwordEncoder.encode("password123"))
                    .email("addons_worker_" + i + "@example.com")
                    .role(Role.WORKER)
                    .enabled(true)
                    .build());
            workerRepository.save(Worker.builder()
                    .name("Worker " + i)
                    .company(company)
                    .user(workerUser)
                    .archived(false)
                    .build());
        }

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(0, null)))
                .andExpect(status().isConflict());

        verify(paddleService, never()).updateSubscriptionAddons(any(), any(), anyInt(), anyInt());
    }

    @Test
    void shouldReturn409_WhenStorageDecreaseWouldDropBelowUsedBytes() throws Exception {
        // STARTER: storage-limit-bytes=10_000_000_000, storage-overage-block-bytes=3_000_000_000.
        // extraStorageBlocks=1 here -> effective limit=13GB. Used=12GB fits.
        // Decrease to extraStorageBlocks=0 -> new limit=10GB < 12GB used.
        subscription = baseSubscription(SubscriptionStatus.ACTIVE, PlanType.STARTER);
        company.setStorageUsedBytes(12_000_000_000L);
        companyRepository.save(company);

        mockMvc.perform(patch("/api/v1/companies/subscription/addons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, 0)))
                .andExpect(status().isConflict());

        verify(paddleService, never()).updateSubscriptionAddons(any(), any(), anyInt(), anyInt());
    }
}
