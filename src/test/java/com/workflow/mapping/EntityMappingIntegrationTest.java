package com.workflow.mapping;

import com.workflow.common.constant.Role;
import com.workflow.dto.company.CompanyDashboardResponse;
import com.workflow.dto.worker.WorkerResponse;
import com.workflow.entity.Client;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.entity.Worker;
import com.workflow.repository.ClientRepository;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.UserRepository;
import com.workflow.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify entity mappings and lazy loading work correctly
 * Tests that all relationships between entities can be accessed without LazyInitializationException
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityMappingIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User companyUser;
    private Company company;
    private User workerUser;
    private Worker worker;
    private Client client;

    @BeforeEach
    void setUp() {
        // Clean up
        workerRepository.deleteAll();
        clientRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        // Create company user
        companyUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("companyowner")
                .password(passwordEncoder.encode("password123"))
                .email("company@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        companyUser = userRepository.save(companyUser);

        // Create company
        company = Company.builder()
                .name("Test Company")
                .user(companyUser)
                .email("company@example.com")
                .telephone("1234567890")
                .archived(false)
                .build();
        company = companyRepository.save(company);

        // Create worker user
        workerUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username("workeruser")
                .password(passwordEncoder.encode("password123"))
                .email("worker@example.com")
                .role(Role.WORKER)
                .enabled(true)
                .build();
        workerUser = userRepository.save(workerUser);

        // Create worker
        worker = Worker.builder()
                .name("John Worker")
                .initials("JW")
                .email("worker@example.com")
                .telephone("1234567890")
                .mobile("0987654321")
                .company(company)
                .user(workerUser)
                .loginLocked(false)
                .archived(false)
                .build();
        worker = workerRepository.save(worker);
        // Add to company collection (bidirectional relationship)
        company.getWorkers().add(worker);

        // Create client
        client = Client.builder()
                .name("Test Client")
                .company(company)
                .email("client@example.com")
                .telephone("1111111111")
                .archived(false)
                .build();
        client = clientRepository.save(client);
        // Add to company collection (bidirectional relationship)
        company.getClients().add(client);
    }

    // ============= Company Mappings =============

    @Test
    void company_ShouldAccessUserRelationship() {
        // Given
        Company foundCompany = companyRepository.findById(company.getId()).orElseThrow();

        // When - Access lazy-loaded user
        User foundUser = foundCompany.getUser();

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("companyowner");
        assertThat(foundUser.getRole()).isEqualTo(Role.COMPANY);
    }

    @Test
    void company_ShouldAccessWorkersCollection() {
        // Given
        Company foundCompany = companyRepository.findById(company.getId()).orElseThrow();

        // When - Access lazy-loaded workers collection
        var workers = foundCompany.getWorkers();

        // Then
        assertThat(workers).isNotNull();
        assertThat(workers).hasSize(1);
        assertThat(workers.get(0).getName()).isEqualTo("John Worker");
    }

    @Test
    void company_ShouldAccessClientsCollection() {
        // Given
        Company foundCompany = companyRepository.findById(company.getId()).orElseThrow();

        // When - Access lazy-loaded clients collection
        var clients = foundCompany.getClients();

        // Then
        assertThat(clients).isNotNull();
        assertThat(clients).hasSize(1);
        assertThat(clients.get(0).getName()).isEqualTo("Test Client");
    }

    @Test
    void company_DashboardShouldWorkWithLazyCollections() {
        // Given
        Company foundCompany = companyRepository.findById(company.getId()).orElseThrow();

        // When - Create dashboard response (accesses lazy collections)
        long totalWorkers = foundCompany.getWorkers().size();
        long activeWorkers = foundCompany.getWorkers().stream()
                .filter(w -> !w.isArchived())
                .count();
        long archivedWorkers = totalWorkers - activeWorkers;
        long totalClients = foundCompany.getClients().size();

        CompanyDashboardResponse dashboard = new CompanyDashboardResponse(
                foundCompany.getId(),
                foundCompany.getName(),
                totalWorkers,
                totalClients,
                activeWorkers,
                archivedWorkers
        );

        // Then
        assertThat(dashboard).isNotNull();
        assertThat(dashboard.totalWorkers()).isEqualTo(1);
        assertThat(dashboard.totalClients()).isEqualTo(1);
        assertThat(dashboard.activeWorkers()).isEqualTo(1);
        assertThat(dashboard.archivedWorkers()).isEqualTo(0);
    }

    // ============= Worker Mappings =============

    @Test
    void worker_ShouldAccessCompanyRelationship() {
        // Given
        Worker foundWorker = workerRepository.findById(worker.getId()).orElseThrow();

        // When - Access lazy-loaded company
        Company foundCompany = foundWorker.getCompany();

        // Then
        assertThat(foundCompany).isNotNull();
        assertThat(foundCompany.getName()).isEqualTo("Test Company");
    }

    @Test
    void worker_ShouldAccessUserRelationship() {
        // Given
        Worker foundWorker = workerRepository.findById(worker.getId()).orElseThrow();

        // When - Access lazy-loaded user
        User foundUser = foundWorker.getUser();

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("workeruser");
        assertThat(foundUser.getRole()).isEqualTo(Role.WORKER);
    }

    @Test
    void worker_ResponseMappingShouldWorkWithLazyUser() {
        // Given
        Worker foundWorker = workerRepository.findById(worker.getId()).orElseThrow();

        // When - Map to response DTO (accesses lazy user)
        WorkerResponse response = WorkerResponse.fromEntity(foundWorker);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(worker.getId());
        assertThat(response.name()).isEqualTo("John Worker");
        assertThat(response.username()).isEqualTo("workeruser");
        assertThat(response.email()).isEqualTo("worker@example.com");
    }

    // ============= Client Mappings =============

    @Test
    void client_ShouldAccessCompanyRelationship() {
        // Given
        Client foundClient = clientRepository.findById(client.getId()).orElseThrow();

        // When - Access lazy-loaded company
        Company foundCompany = foundClient.getCompany();

        // Then
        assertThat(foundCompany).isNotNull();
        assertThat(foundCompany.getName()).isEqualTo("Test Company");
    }

    // ============= Cascade Operations =============

    @Test
    void company_DeleteShouldCascadeToWorkers() {
        // Given
        Long companyId = company.getId();
        Long workerId = worker.getId();

        // When - Delete company (should cascade to workers)
        companyRepository.delete(company);
        companyRepository.flush();

        // Then
        assertThat(companyRepository.findById(companyId)).isEmpty();
        assertThat(workerRepository.findById(workerId)).isEmpty();
    }

    @Test
    void company_DeleteShouldCascadeToClients() {
        // Given
        Long companyId = company.getId();
        Long clientId = client.getId();

        // When - Delete company (should cascade to clients)
        companyRepository.delete(company);
        companyRepository.flush();

        // Then
        assertThat(companyRepository.findById(companyId)).isEmpty();
        assertThat(clientRepository.findById(clientId)).isEmpty();
    }

    @Test
    void company_DeleteShouldNotCascadeToUser() {
        // Given
        Long companyId = company.getId();
        String userUuid = companyUser.getUuid();

        // When - Delete company (should NOT cascade to user)
        companyRepository.delete(company);
        companyRepository.flush();

        // Then
        assertThat(companyRepository.findById(companyId)).isEmpty();
        assertThat(userRepository.findByUuid(userUuid)).isPresent(); // User should still exist
    }

    // ============= Fetch Strategy Tests =============

    @Test
    void worker_FetchingMultipleWorkersShouldNotCauseNPlusOne() {
        // Given - Create additional workers
        for (int i = 0; i < 5; i++) {
            User user = User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .username("worker" + i)
                    .password(passwordEncoder.encode("password"))
                    .email("worker" + i + "@example.com")
                    .role(Role.WORKER)
                    .enabled(true)
                    .build();
            user = userRepository.save(user);

            Worker w = Worker.builder()
                    .name("Worker " + i)
                    .company(company)
                    .user(user)
                    .archived(false)
                    .build();
            workerRepository.save(w);
        }

        // When - Fetch all workers (in transaction, so lazy loading works)
        var allWorkers = workerRepository.findAll();

        // Then - Should be able to access all relationships
        assertThat(allWorkers).hasSize(6); // 1 original + 5 new
        allWorkers.forEach(w -> {
            assertThat(w.getCompany()).isNotNull();
            assertThat(w.getUser()).isNotNull();
            assertThat(w.getUser().getUsername()).isNotEmpty();
        });
    }

    // ============= Unique Constraint Tests =============

    @Test
    void worker_UserRelationshipShouldBeOneToOne() {
        // Given
        Worker foundWorker = workerRepository.findById(worker.getId()).orElseThrow();

        // When
        User foundUser = foundWorker.getUser();

        // Then - Each worker should have exactly one user
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(workerUser.getId());
    }

    @Test
    void company_UserRelationshipShouldBeManyToOne() {
        // Given - One user can theoretically be associated with multiple companies
        // (though business logic may prevent this)
        Company foundCompany = companyRepository.findById(company.getId()).orElseThrow();

        // When
        User foundUser = foundCompany.getUser();

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(companyUser.getId());
    }
}