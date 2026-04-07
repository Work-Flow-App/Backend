package com.workflow.entity;

import com.workflow.entity.company.Company;
import com.workflow.entity.worker.WorkerInvitation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkerInvitation Entity Tests")
class WorkerInvitationTest {

    @Test
    @DisplayName("Should return false when invitation is not expired")
    void isExpired_NotExpired() {
        // Arrange
        WorkerInvitation invitation = WorkerInvitation.builder()
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        // Act
        boolean expired = invitation.isExpired();

        // Assert
        assertFalse(expired);
    }

    @Test
    @DisplayName("Should return true when invitation is expired")
    void isExpired_Expired() {
        // Arrange
        WorkerInvitation invitation = WorkerInvitation.builder()
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        // Act
        boolean expired = invitation.isExpired();

        // Assert
        assertTrue(expired);
    }

    @Test
    @DisplayName("Should return true when invitation is valid (not used and not expired)")
    void isValid_ValidInvitation() {
        // Arrange
        WorkerInvitation invitation = WorkerInvitation.builder()
                .expiresAt(LocalDateTime.now().plusDays(5))
                .used(false)
                .build();

        // Act
        boolean valid = invitation.isValid();

        // Assert
        assertTrue(valid);
    }

    @Test
    @DisplayName("Should return false when invitation is used")
    void isValid_UsedInvitation() {
        // Arrange
        WorkerInvitation invitation = WorkerInvitation.builder()
                .expiresAt(LocalDateTime.now().plusDays(5))
                .used(true)
                .build();

        // Act
        boolean valid = invitation.isValid();

        // Assert
        assertFalse(valid);
    }

    @Test
    @DisplayName("Should return false when invitation is expired")
    void isValid_ExpiredInvitation() {
        // Arrange
        WorkerInvitation invitation = WorkerInvitation.builder()
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        // Act
        boolean valid = invitation.isValid();

        // Assert
        assertFalse(valid);
    }

    @Test
    @DisplayName("Should return false when invitation is both used and expired")
    void isValid_UsedAndExpired() {
        // Arrange
        WorkerInvitation invitation = WorkerInvitation.builder()
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(true)
                .build();

        // Act
        boolean valid = invitation.isValid();

        // Assert
        assertFalse(valid);
    }

    @Test
    @DisplayName("Should mark invitation as used and set usedAt timestamp")
    void markAsUsed_Success() {
        // Arrange
        WorkerInvitation invitation = WorkerInvitation.builder()
                .used(false)
                .usedAt(null)
                .build();

        // Act
        invitation.markAsUsed();

        // Assert
        assertTrue(invitation.isUsed());
        assertNotNull(invitation.getUsedAt());
        assertTrue(invitation.getUsedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(invitation.getUsedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    @DisplayName("Should update usedAt when marking as used multiple times")
    void markAsUsed_MultipleTimes() {
        // Arrange
        WorkerInvitation invitation = WorkerInvitation.builder()
                .used(false)
                .usedAt(null)
                .build();

        LocalDateTime firstUsedAt = LocalDateTime.now().minusMinutes(5);
        invitation.setUsed(true);
        invitation.setUsedAt(firstUsedAt);

        // Act
        invitation.markAsUsed();

        // Assert
        assertTrue(invitation.isUsed());
        assertNotNull(invitation.getUsedAt());
        assertTrue(invitation.getUsedAt().isAfter(firstUsedAt));
    }

    @Test
    @DisplayName("Should build invitation with all fields")
    void builder_AllFields() {
        // Arrange
        Company company = Company.builder().id(1L).name("Test Company").build();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(7);

        // Act
        WorkerInvitation invitation = WorkerInvitation.builder()
                .id(1L)
                .invitationToken("token-123")
                .email("worker@example.com")
                .company(company)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(now)
                .usedAt(null)
                .build();

        // Assert
        assertEquals(1L, invitation.getId());
        assertEquals("token-123", invitation.getInvitationToken());
        assertEquals("worker@example.com", invitation.getEmail());
        assertEquals(company, invitation.getCompany());
        assertEquals(expiresAt, invitation.getExpiresAt());
        assertFalse(invitation.isUsed());
        assertEquals(now, invitation.getCreatedAt());
        assertNull(invitation.getUsedAt());
    }

    @Test
    @DisplayName("Should handle edge case when invitation expires exactly now")
    void isExpired_ExpiresNow() {
        // Arrange
        WorkerInvitation invitation = WorkerInvitation.builder()
                .expiresAt(LocalDateTime.now())
                .build();

        // Wait a tiny bit to ensure we're past expiration
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        boolean expired = invitation.isExpired();

        // Assert - invitation should be expired
        assertTrue(expired);
    }

    @Test
    @DisplayName("Should properly check validity for invitation expiring soon")
    void isValid_ExpiringSoon() {
        // Arrange - expires in 1 second
        WorkerInvitation invitation = WorkerInvitation.builder()
                .expiresAt(LocalDateTime.now().plusSeconds(1))
                .used(false)
                .build();

        // Act
        boolean valid = invitation.isValid();

        // Assert - should still be valid
        assertTrue(valid);
    }
}
