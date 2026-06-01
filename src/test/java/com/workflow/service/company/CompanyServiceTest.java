package com.workflow.service.company;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.CompanyAlreadyExistsException;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.dto.company.CompanyAddressRequest;
import com.workflow.dto.company.CompanyDashboardResponse;
import com.workflow.dto.company.CompanyProfileResponse;
import com.workflow.dto.company.CompanyProfileUpdateRequest;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanyAddress;
import com.workflow.entity.auth.User;
import com.workflow.entity.worker.Worker;
import com.workflow.repository.company.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    private User companyUser;
    private Company company;
    private CompanyProfileUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        companyUser = User.builder()
                .id(1L)
                .uuid("test-uuid")
                .username("companyowner")
                .password("encodedPassword")
                .email("company@example.com")
                .role(Role.COMPANY)
                .enabled(true)
                .build();

        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .user(companyUser)
                .email("company@example.com")
                .telephone("1234567890")
                .address(CompanyAddress.builder()
                        .addressLine1("123 Main St")
                        .town("New York")
                        .country("USA")
                        .postcode("10001")
                        .build())
                .archived(false)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        updateRequest = new CompanyProfileUpdateRequest(
                "Updated Company",
                new CompanyAddressRequest("456 Oak Ave", "Suite 200", null, "Los Angeles", "USA", "90001"),
                "9876543210",
                "5555555555",
                "1111111111",
                "updated@example.com",
                "contact@example.com",
                "ACC456",
                null,
                null,
                null
        );
    }

    // ============= updateProfile Tests =============

    @Test
    void updateProfile_ShouldUpdateCompanySuccessfully() {
        // Arrange
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCase("Updated Company")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        CompanyProfileResponse response = companyService.updateProfile(updateRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(companyRepository).findByUserIdAndNotArchived(1L);
        verify(companyRepository).existsByNameIgnoreCase("Updated Company");
        verify(companyRepository).save(company);
    }

    @Test
    void updateProfile_ShouldThrowException_WhenCompanyNotFound() {
        // Arrange
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companyService.updateProfile(updateRequest, 1L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("No active company found");

        verify(companyRepository).findByUserIdAndNotArchived(1L);
        verify(companyRepository, never()).save(any());
    }

    @Test
    void updateProfile_ShouldThrowException_WhenNewNameAlreadyExists() {
        // Arrange
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCase("Updated Company")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> companyService.updateProfile(updateRequest, 1L))
                .isInstanceOf(CompanyAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(companyRepository).findByUserIdAndNotArchived(1L);
        verify(companyRepository).existsByNameIgnoreCase("Updated Company");
        verify(companyRepository, never()).save(any());
    }

    @Test
    void updateProfile_ShouldAllowSameNameWithDifferentCase() {
        // Arrange
        CompanyProfileUpdateRequest sameNameRequest = new CompanyProfileUpdateRequest(
                "test company", // Same name, different case
                null, null, null, null, null, null, null, null, null, null
        );
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        CompanyProfileResponse response = companyService.updateProfile(sameNameRequest, 1L);

        // Assert
        assertThat(response).isNotNull();
        verify(companyRepository).findByUserIdAndNotArchived(1L);
        verify(companyRepository, never()).existsByNameIgnoreCase(anyString());
        verify(companyRepository).save(company);
    }

    @Test
    void updateProfile_ShouldUpdateAllFields() {
        // Arrange
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameIgnoreCase("Updated Company")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company savedCompany = invocation.getArgument(0);
            assertThat(savedCompany.getName()).isEqualTo("Updated Company");
            assertThat(savedCompany.getAddress().getAddressLine1()).isEqualTo("456 Oak Ave");
            assertThat(savedCompany.getAddress().getAddressLine2()).isEqualTo("Suite 200");
            assertThat(savedCompany.getAddress().getTown()).isEqualTo("Los Angeles");
            assertThat(savedCompany.getAddress().getCountry()).isEqualTo("USA");
            assertThat(savedCompany.getAddress().getPostcode()).isEqualTo("90001");
            assertThat(savedCompany.getTelephone()).isEqualTo("9876543210");
            assertThat(savedCompany.getMobile()).isEqualTo("5555555555");
            assertThat(savedCompany.getFax()).isEqualTo("1111111111");
            assertThat(savedCompany.getEmail()).isEqualTo("updated@example.com");
            assertThat(savedCompany.getContactEmail()).isEqualTo("contact@example.com");
            assertThat(savedCompany.getContactNumber()).isEqualTo("ACC456");
            return savedCompany;
        });

        // Act
        companyService.updateProfile(updateRequest, 1L);

        // Assert
        verify(companyRepository).save(company);
    }

    // ============= getProfile Tests =============

    @Test
    void getProfile_ShouldReturnCompanyProfile() {
        // Arrange
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.of(company));

        // Act
        CompanyProfileResponse response = companyService.getProfile(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Test Company");
        assertThat(response.email()).isEqualTo("company@example.com");
        assertThat(response.telephone()).isEqualTo("1234567890");
        assertThat(response.archived()).isFalse();
        verify(companyRepository).findByUserIdAndNotArchived(1L);
    }

    @Test
    void getProfile_ShouldThrowException_WhenCompanyNotFound() {
        // Arrange
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companyService.getProfile(1L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("No active company found");

        verify(companyRepository).findByUserIdAndNotArchived(1L);
    }

    // ============= getDashboard Tests =============

    @Test
    void getDashboard_ShouldReturnDashboardWithNoWorkers() {
        // Arrange
        company.setWorkers(new ArrayList<>());
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.of(company));

        // Act
        CompanyDashboardResponse response = companyService.getDashboard(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.companyId()).isEqualTo(1L);
        assertThat(response.companyName()).isEqualTo("Test Company");
        assertThat(response.totalWorkers()).isEqualTo(0);
        assertThat(response.activeWorkers()).isEqualTo(0);
        assertThat(response.archivedWorkers()).isEqualTo(0);
        assertThat(response.totalClients()).isEqualTo(0);
        verify(companyRepository).findByUserIdAndNotArchived(1L);
    }

    @Test
    void getDashboard_ShouldThrowException_WhenCompanyNotFound() {
        // Arrange
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companyService.getDashboard(1L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("No active company found");

        verify(companyRepository).findByUserIdAndNotArchived(1L);
    }

    // ============= findCompanyByUserId Tests =============

    @Test
    void findCompanyByUserId_ShouldReturnCompany() {
        // Arrange
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.of(company));

        // Act
        Company result = companyService.findCompanyByUserId(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Company");
        verify(companyRepository).findByUserIdAndNotArchived(1L);
    }

    @Test
    void findCompanyByUserId_ShouldThrowException_WhenNotFound() {
        // Arrange
        when(companyRepository.findByUserIdAndNotArchived(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companyService.findCompanyByUserId(1L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("No active company found");

        verify(companyRepository).findByUserIdAndNotArchived(1L);
    }
}
