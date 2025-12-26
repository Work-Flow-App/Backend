package com.workflow.service.asset;

import com.workflow.common.exception.business.AssetNotFoundException;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.DuplicateNameException;
import com.workflow.dto.asset.*;
import com.workflow.entity.Asset;
import com.workflow.entity.AssetJobAssignment;
import com.workflow.entity.Company;
import com.workflow.repository.AssetJobAssignmentRepository;
import com.workflow.repository.AssetRepository;
import com.workflow.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetJobAssignmentRepository assignmentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private AssetService assetService;

    private Company company;
    private Asset asset;
    private AssetCreateRequest createRequest;
    private AssetUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .id(1L)
                .name("Test Company")
                .build();

        asset = Asset.builder()
                .id(1L)
                .company(company)
                .name("Excavator XL-500")
                .assetTag("EQ-001")
                .serialNumber("ABC123")
                .description("Heavy duty excavator")
                .purchasePrice(new BigDecimal("45000.00"))
                .purchaseDate(LocalDate.of(2024, 1, 15))
                .depreciationRate(new BigDecimal("20.00"))
                .salvageValue(new BigDecimal("5000.00"))
                .available(true)
                .archived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = AssetCreateRequest.builder()
                .name("Cordless Drill #42")
                .assetTag("TOOL-042")
                .serialNumber("XYZ789")
                .description("18V cordless drill")
                .purchasePrice(new BigDecimal("299.99"))
                .purchaseDate(LocalDate.of(2024, 6, 1))
                .depreciationRate(new BigDecimal("15.00"))
                .salvageValue(new BigDecimal("50.00"))
                .build();

        updateRequest = AssetUpdateRequest.builder()
                .name("Updated Excavator")
                .build();
    }

    // ==================== CREATE ASSET TESTS ====================

    @Test
    void createAsset_Success() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(assetRepository.existsByCompanyIdAndName(1L, "Cordless Drill #42")).thenReturn(false);
        when(assetRepository.existsByCompanyIdAndAssetTag(1L, "TOOL-042")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> {
            Asset savedAsset = invocation.getArgument(0);
            savedAsset.setId(2L);
            savedAsset.setCreatedAt(LocalDateTime.now());
            savedAsset.setUpdatedAt(LocalDateTime.now());
            return savedAsset;
        });

        AssetResponse response = assetService.createAsset(createRequest, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Cordless Drill #42");
        assertThat(response.getAssetTag()).isEqualTo("TOOL-042");
        assertThat(response.getPurchasePrice()).isEqualByComparingTo("299.99");
        assertThat(response.isAvailable()).isTrue();
        assertThat(response.isArchived()).isFalse();

        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void createAsset_CompanyNotFound_ThrowsException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessage("Company not found");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void createAsset_NameTooShort_ThrowsException() {
        createRequest.setName("A");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Asset name is required (2-150 chars)");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void createAsset_DuplicateName_ThrowsException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(assetRepository.existsByCompanyIdAndName(1L, "Cordless Drill #42")).thenReturn(true);

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(DuplicateNameException.class)
                .hasMessage("Asset name must be unique within the company");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void createAsset_DuplicateAssetTag_ThrowsException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(assetRepository.existsByCompanyIdAndName(1L, "Cordless Drill #42")).thenReturn(false);
        when(assetRepository.existsByCompanyIdAndAssetTag(1L, "TOOL-042")).thenReturn(true);

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(DuplicateNameException.class)
                .hasMessage("Asset tag must be unique within the company");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void createAsset_InvalidPurchasePrice_ThrowsException() {
        createRequest.setPurchasePrice(BigDecimal.ZERO);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(assetRepository.existsByCompanyIdAndName(anyLong(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Purchase price must be greater than 0");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void createAsset_FuturePurchaseDate_ThrowsException() {
        createRequest.setPurchaseDate(LocalDate.now().plusDays(1));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(assetRepository.existsByCompanyIdAndName(anyLong(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Purchase date is required and cannot be in the future");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void createAsset_InvalidDepreciationRate_ThrowsException() {
        createRequest.setDepreciationRate(new BigDecimal("150"));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(assetRepository.existsByCompanyIdAndName(anyLong(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Depreciation rate must be between 0 and 100");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void createAsset_SalvageValueGreaterThanPurchasePrice_ThrowsException() {
        createRequest.setPurchasePrice(new BigDecimal("100"));
        createRequest.setSalvageValue(new BigDecimal("200"));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(assetRepository.existsByCompanyIdAndName(anyLong(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Purchase price must be greater than salvage value");

        verify(assetRepository, never()).save(any());
    }

    // ==================== UPDATE ASSET TESTS ====================

    @Test
    void updateAsset_Success() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.existsByCompanyIdAndName(1L, "Updated Excavator")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        AssetResponse response = assetService.updateAsset(1L, updateRequest, 1L);

        assertThat(response).isNotNull();
        verify(assetRepository).save(asset);
        assertThat(asset.getName()).isEqualTo("Updated Excavator");
    }

    @Test
    void updateAsset_AssetNotFound_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.updateAsset(1L, updateRequest, 1L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void updateAsset_WrongCompany_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.updateAsset(1L, updateRequest, 999L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void updateAsset_ArchivedAsset_ThrowsException() {
        asset.setArchived(true);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.updateAsset(1L, updateRequest, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot update archived asset");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void updateAsset_DuplicateName_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.existsByCompanyIdAndName(1L, "Updated Excavator")).thenReturn(true);

        assertThatThrownBy(() -> assetService.updateAsset(1L, updateRequest, 1L))
                .isInstanceOf(DuplicateNameException.class)
                .hasMessage("Asset name must be unique within the company");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void updateAsset_InvalidSalvageValue_ThrowsException() {
        updateRequest.setSalvageValue(new BigDecimal("50000"));
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.updateAsset(1L, updateRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Purchase price must be greater than salvage value");

        verify(assetRepository, never()).save(any());
    }

    // ==================== GET ASSET TESTS ====================

    @Test
    void getAsset_Success() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        AssetResponse response = assetService.getAsset(1L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Excavator XL-500");
        assertThat(response.getAssetTag()).isEqualTo("EQ-001");
    }

    @Test
    void getAsset_NotFound_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getAsset(1L, 1L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");
    }

    @Test
    void getAsset_WrongCompany_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.getAsset(1L, 999L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");
    }

    // ==================== LIST ASSETS TESTS ====================

    @Test
    void listAssets_DefaultFilters_ReturnsNonArchived() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        Page<Asset> page = new PageImpl<>(List.of(asset));

        when(assetRepository.findByCompanyIdAndArchivedFalse(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<AssetResponse> response = assetService.listAssets(1L, 0, 20, null, null, "name", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Excavator XL-500");
        verify(assetRepository).findByCompanyIdAndArchivedFalse(eq(1L), any(Pageable.class));
    }

    @Test
    void listAssets_FilterByAvailable_ReturnsOnlyAvailable() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        Page<Asset> page = new PageImpl<>(List.of(asset));

        when(assetRepository.findByCompanyIdAndArchivedFalseAndAvailable(eq(1L), eq(true), any(Pageable.class)))
                .thenReturn(page);

        Page<AssetResponse> response = assetService.listAssets(1L, 0, 20, null, true, "name", "asc");

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(assetRepository).findByCompanyIdAndArchivedFalseAndAvailable(eq(1L), eq(true), any(Pageable.class));
    }

    @Test
    void listAssets_FilterByArchived_ReturnsAllAssets() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        Page<Asset> page = new PageImpl<>(List.of(asset));

        when(assetRepository.findByCompanyId(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<AssetResponse> response = assetService.listAssets(1L, 0, 20, true, null, "name", "asc");

        assertThat(response).isNotNull();
        verify(assetRepository).findByCompanyId(eq(1L), any(Pageable.class));
    }

    @Test
    void listAssets_DescendingSort_Works() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "purchaseDate"));
        Page<Asset> page = new PageImpl<>(List.of(asset));

        when(assetRepository.findByCompanyIdAndArchivedFalse(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<AssetResponse> response = assetService.listAssets(1L, 0, 20, null, null, "purchaseDate", "desc");

        assertThat(response).isNotNull();
        verify(assetRepository).findByCompanyIdAndArchivedFalse(eq(1L), any(Pageable.class));
    }

    // ==================== ARCHIVE ASSET TESTS ====================

    @Test
    void archiveAsset_Success() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.empty());
        when(assetRepository.save(asset)).thenReturn(asset);

        assetService.archiveAsset(1L, 1L);

        assertThat(asset.isArchived()).isTrue();
        verify(assetRepository).save(asset);
    }

    @Test
    void archiveAsset_AssetNotFound_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.archiveAsset(1L, 1L))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void archiveAsset_WithActiveAssignment_ThrowsException() {
        AssetJobAssignment activeAssignment = AssetJobAssignment.builder()
                .id(1L)
                .asset(asset)
                .assignedAt(LocalDateTime.now())
                .build();

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assignmentRepository.findByAssetIdAndReturnedAtIsNull(1L)).thenReturn(Optional.of(activeAssignment));

        assertThatThrownBy(() -> assetService.archiveAsset(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot archive asset currently assigned to active job");

        verify(assetRepository, never()).save(any());
    }

    // ==================== CALCULATE ASSET VALUE TESTS ====================

    @Test
    void calculateAssetValue_CurrentDate_ReturnsDepreciatedValue() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        LocalDate asOfDate = LocalDate.of(2025, 1, 15);
        AssetValueResponse response = assetService.calculateAssetValue(1L, 1L, asOfDate);

        assertThat(response).isNotNull();
        assertThat(response.getAssetId()).isEqualTo(1L);
        assertThat(response.getPurchasePrice()).isEqualByComparingTo("45000.00");
        assertThat(response.getCurrentValue()).isLessThan(response.getPurchasePrice());
        assertThat(response.getCurrentValue()).isGreaterThanOrEqualTo(response.getSalvageValue());
        assertThat(response.getDaysOwned()).isGreaterThan(0);
        assertThat(response.getYearsOwned()).isGreaterThan(0);
    }

    @Test
    void calculateAssetValue_BeforePurchaseDate_ReturnsPurchasePrice() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        LocalDate asOfDate = LocalDate.of(2023, 1, 1);
        AssetValueResponse response = assetService.calculateAssetValue(1L, 1L, asOfDate);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentValue()).isEqualByComparingTo(response.getPurchasePrice());
        assertThat(response.getTotalDepreciation()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getDaysOwned()).isZero();
        assertThat(response.getYearsOwned()).isZero();
    }

    @Test
    void calculateAssetValue_NullDate_UsesCurrentDate() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        AssetValueResponse response = assetService.calculateAssetValue(1L, 1L, null);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentValue()).isLessThanOrEqualTo(response.getPurchasePrice());
    }

    @Test
    void calculateAssetValue_AssetNotFound_ThrowsException() {
        when(assetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.calculateAssetValue(1L, 1L, LocalDate.now()))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessage("Asset not found");
    }

    // ==================== GET STATISTICS TESTS ====================

    @Test
    void getStatistics_Success() {
        Asset asset2 = Asset.builder()
                .id(2L)
                .company(company)
                .name("Drill")
                .purchasePrice(new BigDecimal("300.00"))
                .purchaseDate(LocalDate.of(2024, 1, 1))
                .depreciationRate(new BigDecimal("10.00"))
                .salvageValue(BigDecimal.ZERO)
                .available(false)
                .archived(false)
                .build();

        Page<Asset> page = new PageImpl<>(Arrays.asList(asset, asset2));

        when(assetRepository.findByCompanyIdAndArchivedFalse(eq(1L), any(Pageable.class))).thenReturn(page);

        AssetStatistics stats = assetService.getStatistics(1L);

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalAssets()).isEqualTo(2);
        assertThat(stats.getAvailableAssets()).isEqualTo(1);
        assertThat(stats.getAssetsInUse()).isEqualTo(1);
        assertThat(stats.getTotalPurchaseValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(stats.getTotalCurrentValue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(stats.getAverageDepreciationRate()).isGreaterThan(0);
    }

    @Test
    void getStatistics_NoAssets_ReturnsZeroStats() {
        Page<Asset> emptyPage = new PageImpl<>(List.of());

        when(assetRepository.findByCompanyIdAndArchivedFalse(eq(1L), any(Pageable.class))).thenReturn(emptyPage);

        AssetStatistics stats = assetService.getStatistics(1L);

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalAssets()).isZero();
        assertThat(stats.getAvailableAssets()).isZero();
        assertThat(stats.getAssetsInUse()).isZero();
        assertThat(stats.getTotalPurchaseValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}