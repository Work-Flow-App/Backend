package com.workflow.controller.worker;

import com.workflow.AbstractControllerIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.common.constant.worker.LeaveRequestStatus;
import com.workflow.common.constant.worker.LeaveType;
import com.workflow.dto.worker.LeaveRequestCreateRequest;
import com.workflow.dto.worker.LeaveRequestDecisionRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.worker.WorkerLeaveRequest;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.worker.WorkerLeaveRequestRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkerLeaveRequestControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private WorkerLeaveRequestRepository leaveRequestRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Worker worker1;
    private Worker worker2;
    private WorkerLeaveRequest existingRequest;
    private String companyAAdminToken;
    private String companyAEditorToken;
    private String companyBAdminToken;
    private String worker1Token;
    private String worker2Token;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        workerRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyAAdmin = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("leavecompanyAadmin")
                .password(passwordEncoder.encode("password")).email("leaveA-admin@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User companyAEditor = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("leavecompanyAeditor")
                .password(passwordEncoder.encode("password")).email("leaveA-editor@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User companyBAdmin = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("leavecompanyBadmin")
                .password(passwordEncoder.encode("password")).email("leaveB-admin@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User worker1User = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("leaveworker1")
                .password(passwordEncoder.encode("password")).email("leaveworker1@test.com")
                .role(Role.WORKER).enabled(true).build());

        User worker2User = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("leaveworker2")
                .password(passwordEncoder.encode("password")).email("leaveworker2@test.com")
                .role(Role.WORKER).enabled(true).build());

        Company companyA = companyRepository.save(Company.builder()
                .name("Leave Company A").user(companyAAdmin).email("leaveA-admin@test.com").archived(false).build());

        Company companyB = companyRepository.save(Company.builder()
                .name("Leave Company B").user(companyBAdmin).email("leaveB-admin@test.com").archived(false).build());

        createCompanyMember(companyA, companyAAdmin, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(companyA, companyAEditor, CompanyRole.EDITOR);
        createCompanyMember(companyB, companyBAdmin, CompanyRole.COMPANY_ADMIN);

        worker1 = workerRepository.save(Worker.builder()
                .name("Leave Worker One").company(companyA).user(worker1User).archived(false).build());

        worker2 = workerRepository.save(Worker.builder()
                .name("Leave Worker Two").company(companyA).user(worker2User).archived(false).build());

        existingRequest = leaveRequestRepository.save(WorkerLeaveRequest.builder()
                .worker(worker1)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 5))
                .reason("Family trip")
                .status(LeaveRequestStatus.PENDING)
                .build());

        companyAAdminToken = jwtService.generateToken(companyAAdmin);
        companyAEditorToken = jwtService.generateToken(companyAEditor);
        companyBAdminToken = jwtService.generateToken(companyBAdmin);
        worker1Token = jwtService.generateToken(worker1User);
        worker2Token = jwtService.generateToken(worker2User);
    }

    // ============= POST /api/v1/worker/leave-requests (self) =============

    @Test
    void shouldSubmitLeaveRequestSuccessfully() throws Exception {
        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(
                LeaveType.SICK, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), "Flu");

        mockMvc.perform(post("/api/v1/worker/leave-requests")
                        .header("Authorization", "Bearer " + worker1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.workerId").value(worker1.getId()));
    }

    @Test
    void shouldReturn409WhenOverlappingExistingRequest() throws Exception {
        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(
                LeaveType.SICK, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4), "Overlaps");

        mockMvc.perform(post("/api/v1/worker/leave-requests")
                        .header("Authorization", "Bearer " + worker1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ============= GET /api/v1/worker/leave-requests (self) - JOIN FETCH / OSIV regression =============

    @Test
    void shouldListOwnLeaveRequestsWithoutLazyInitializationException() throws Exception {
        mockMvc.perform(get("/api/v1/worker/leave-requests")
                        .header("Authorization", "Bearer " + worker1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].workerId").value(worker1.getId()));
    }

    // ============= IDOR: self-service cannot touch another worker's leave request =============

    @Test
    void shouldReturn404WhenAnotherWorkerCancelsLeaveRequest() throws Exception {
        mockMvc.perform(delete("/api/v1/worker/leave-requests/" + existingRequest.getId())
                        .header("Authorization", "Bearer " + worker2Token))
                .andExpect(status().isNotFound());
    }

    // ============= Admin approve/reject =============

    @Test
    void shouldApproveLeaveRequestSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/workers/leave-requests/" + existingRequest.getId() + "/approve")
                        .header("Authorization", "Bearer " + companyAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionByUsername").value("leavecompanyAadmin"));
    }

    @Test
    void shouldReturn400WhenRejectingWithoutNote() throws Exception {
        mockMvc.perform(post("/api/v1/workers/leave-requests/" + existingRequest.getId() + "/reject")
                        .header("Authorization", "Bearer " + companyAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveRequestDecisionRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectLeaveRequestSuccessfullyWithNote() throws Exception {
        mockMvc.perform(post("/api/v1/workers/leave-requests/" + existingRequest.getId() + "/reject")
                        .header("Authorization", "Bearer " + companyAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LeaveRequestDecisionRequest("Short-staffed that week"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionNote").value("Short-staffed that week"));
    }

    @Test
    void shouldReturn403WhenEditorTriesToApprove() throws Exception {
        mockMvc.perform(post("/api/v1/workers/leave-requests/" + existingRequest.getId() + "/approve")
                        .header("Authorization", "Bearer " + companyAEditorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenApprovingLeaveRequestFromAnotherCompany() throws Exception {
        mockMvc.perform(post("/api/v1/workers/leave-requests/" + existingRequest.getId() + "/approve")
                        .header("Authorization", "Bearer " + companyBAdminToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/workers/leave-requests/calendar =============

    @Test
    void shouldGetCompanyLeaveCalendarOnlyReturnsApprovedRequests() throws Exception {
        mockMvc.perform(post("/api/v1/workers/leave-requests/" + existingRequest.getId() + "/approve")
                        .header("Authorization", "Bearer " + companyAAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/workers/leave-requests/calendar")
                        .param("from", "2026-07-01")
                        .param("to", "2026-09-30")
                        .header("Authorization", "Bearer " + companyAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].workerId").value(worker1.getId()));
    }
}
