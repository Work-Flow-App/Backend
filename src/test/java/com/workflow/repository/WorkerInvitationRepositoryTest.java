package com.workflow.repository;

import com.workflow.common.constant.Role;
import com.workflow.entity.company.Company;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.WorkerInvitation;
import com.workflow.repository.worker.WorkerInvitationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("WorkerInvitationRepository Integration Tests")
class WorkerInvitationRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private WorkerInvitationRepository invitationRepository;

    private Company testCompany;
    private WorkerInvitation validInvitation;
    private WorkerInvitation expiredInvitation;
    private WorkerInvitation usedInvitation;

    @BeforeEach
    void setUp() {
        // Create company user
        User companyUser = User.builder()
                .uuid("company-uuid-123")
                .username("companyuser")
                .email("company@example.com")
                .password("$2a$10$encodedPassword")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        entityManager.persist(companyUser);

        // Create company
        testCompany = Company.builder()
                .name("Test Company")
                .user(companyUser)
                .archived(false)
                .build();
        entityManager.persist(testCompany);

        // Create valid invitation
        validInvitation = WorkerInvitation.builder()
                .invitationToken("valid-token-123")
                .email("worker1@example.com")
                .company(testCompany)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(validInvitation);

        // Create expired invitation
        expiredInvitation = WorkerInvitation.builder()
                .invitationToken("expired-token-456")
                .email("worker2@example.com")
                .company(testCompany)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .used(false)
                .createdAt(LocalDateTime.now().minusDays(8))
                .build();
        entityManager.persist(expiredInvitation);

        // Create used invitation
        usedInvitation = WorkerInvitation.builder()
                .invitationToken("used-token-789")
                .email("worker3@example.com")
                .company(testCompany)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .used(true)
                .usedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(4))
                .build();
        entityManager.persist(usedInvitation);

        entityManager.flush();
    }

    @Test
    @DisplayName("Should find invitation by token")
    void findByInvitationToken_Success() {
        // Act
        Optional<WorkerInvitation> found = invitationRepository.findByInvitationToken("valid-token-123");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("worker1@example.com", found.get().getEmail());
        assertEquals(testCompany.getId(), found.get().getCompany().getId());
    }

    @Test
    @DisplayName("Should return empty when token not found")
    void findByInvitationToken_NotFound() {
        // Act
        Optional<WorkerInvitation> found = invitationRepository.findByInvitationToken("non-existent-token");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should find active invitation by email and company")
    void findActiveInvitationByEmailAndCompany_Success() {
        // Act
        Optional<WorkerInvitation> found = invitationRepository.findActiveInvitationByEmailAndCompany(
                "worker1@example.com",
                testCompany.getId(),
                LocalDateTime.now()
        );

        // Assert
        assertTrue(found.isPresent());
        assertEquals("valid-token-123", found.get().getInvitationToken());
        assertFalse(found.get().isUsed());
    }

    @Test
    @DisplayName("Should not find expired invitation")
    void findActiveInvitationByEmailAndCompany_Expired() {
        // Act
        Optional<WorkerInvitation> found = invitationRepository.findActiveInvitationByEmailAndCompany(
                "worker2@example.com",
                testCompany.getId(),
                LocalDateTime.now()
        );

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should not find used invitation")
    void findActiveInvitationByEmailAndCompany_Used() {
        // Act
        Optional<WorkerInvitation> found = invitationRepository.findActiveInvitationByEmailAndCompany(
                "worker3@example.com",
                testCompany.getId(),
                LocalDateTime.now()
        );

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should invalidate previous invitations for email and company")
    void invalidatePreviousInvitations_Success() {
        // Arrange
        String email = "worker1@example.com";

        // Act
        invitationRepository.invalidatePreviousInvitations(email, testCompany.getId());
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<WorkerInvitation> updated = invitationRepository.findByInvitationToken("valid-token-123");
        assertTrue(updated.isPresent());
        assertTrue(updated.get().isUsed());
    }

    @Test
    @DisplayName("Should find all invitations by company ordered by creation date")
    void findByCompanyIdOrderByCreatedAtDesc_Success() {
        // Act
        List<WorkerInvitation> invitations = invitationRepository.findByCompanyIdOrderByCreatedAtDesc(testCompany.getId());

        // Assert
        assertEquals(3, invitations.size());
        // Should be ordered by createdAt DESC (newest first)
        assertTrue(invitations.get(0).getCreatedAt().isAfter(invitations.get(1).getCreatedAt()) ||
                   invitations.get(0).getCreatedAt().isEqual(invitations.get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("Should delete expired and used invitations")
    void deleteExpiredAndUsedInvitations_Success() {
        // Arrange
        LocalDateTime cutoff = LocalDateTime.now(); // Delete invitations expired before now

        // Act
        int deletedCount = invitationRepository.deleteExpiredAndUsedInvitations(cutoff);
        entityManager.flush();

        // Assert - should delete expired and used invitations
        assertTrue(deletedCount >= 2); // At least expired and used should be deleted
    }

    @Test
    @DisplayName("Should not delete valid pending invitations")
    void deleteExpiredAndUsedInvitations_PreservesValid() {
        // Arrange
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        String validToken = validInvitation.getInvitationToken();

        // Act
        invitationRepository.deleteExpiredAndUsedInvitations(cutoff);
        entityManager.flush();
        entityManager.clear();

        // Assert - valid invitation should still exist
        Optional<WorkerInvitation> found = invitationRepository.findByInvitationToken(validToken);
        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("Should handle multiple companies correctly")
    void findByCompanyIdOrderByCreatedAtDesc_MultipleCompanies() {
        // Arrange - create another company
        User anotherCompanyUser = User.builder()
                .uuid("company-uuid-456")
                .username("anothercompany")
                .email("another@example.com")
                .password("$2a$10$encodedPassword")
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        entityManager.persist(anotherCompanyUser);

        Company anotherCompany = Company.builder()
                .name("Another Company")
                .user(anotherCompanyUser)
                .archived(false)
                .build();
        entityManager.persist(anotherCompany);

        WorkerInvitation anotherInvitation = WorkerInvitation.builder()
                .invitationToken("another-token-123")
                .email("worker@anothercompany.com")
                .company(anotherCompany)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(anotherInvitation);
        entityManager.flush();

        // Act
        List<WorkerInvitation> testCompanyInvitations =
                invitationRepository.findByCompanyIdOrderByCreatedAtDesc(testCompany.getId());
        List<WorkerInvitation> anotherCompanyInvitations =
                invitationRepository.findByCompanyIdOrderByCreatedAtDesc(anotherCompany.getId());

        // Assert
        assertEquals(3, testCompanyInvitations.size());
        assertEquals(1, anotherCompanyInvitations.size());
        assertTrue(testCompanyInvitations.stream().allMatch(inv -> inv.getCompany().getId().equals(testCompany.getId())));
        assertEquals("worker@anothercompany.com", anotherCompanyInvitations.get(0).getEmail());
    }

    @Test
    @DisplayName("Should persist invitation with all fields correctly")
    void save_Success() {
        // Arrange
        WorkerInvitation newInvitation = WorkerInvitation.builder()
                .invitationToken("new-token-abc")
                .email("newworker@example.com")
                .company(testCompany)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .used(false)
                .build();

        // Act
        WorkerInvitation saved = invitationRepository.save(newInvitation);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<WorkerInvitation> found = invitationRepository.findByInvitationToken("new-token-abc");
        assertTrue(found.isPresent());
        assertEquals("newworker@example.com", found.get().getEmail());
        assertEquals(testCompany.getId(), found.get().getCompany().getId());
        assertFalse(found.get().isUsed());
        assertNotNull(found.get().getCreatedAt());
        assertNull(found.get().getUsedAt());
    }

    @Test
    @DisplayName("Should update invitation to used status")
    void update_MarkAsUsed() {
        // Arrange
        validInvitation.markAsUsed();

        // Act
        invitationRepository.save(validInvitation);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<WorkerInvitation> updated = invitationRepository.findByInvitationToken("valid-token-123");
        assertTrue(updated.isPresent());
        assertTrue(updated.get().isUsed());
        assertNotNull(updated.get().getUsedAt());
    }
}
