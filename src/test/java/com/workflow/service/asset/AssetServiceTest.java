package com.workflow.service.asset;

import com.workflow.common.exception.business.AssetNotFoundException;
import com.workflow.common.exception.business.DuplicateNameException;
import com.workflow.dto.asset.*;
import com.workflow.entity.asset.Asset;
import com.workflow.entity.asset.AssetJobAssignment;
import com.workflow.entity.company.Company;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.service.sequence.CompanyCounterService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Set;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    @Mock
    private CompanyCounterService companyCounterService;

    @InjectMocks
    private AssetService assetService;

    // Used for testing Bean Validation constraints on DTOs
    private Validator validator;

    private Company company;
    private Asset asset;
    private AssetCreateRequest createRequest;
    private AssetUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
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
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
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
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(assetRepository.existsByCompanyIdAndName(1L, "Cordless Drill #42")).thenReturn(false);
        when(assetRepository.existsByCompanyIdAndAssetTag(1L, "TOOL-042")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> {
            Asset savedAsset = invocation.getArgument(0);
            savedAsset.setId(2L);
            savedAsset.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            savedAsset.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
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

    // Company existence is now enforced by FK constraint at commit, not upfront.
    // createAsset_CompanyNotFound_ThrowsException removed: getReferenceById returns a
    // proxy without a DB hit; the constraint fires at flush in an integration context.

    @Test
    void createAsset_NameTooShort_DtoViolation() {
        // Field-level validation is now on AssetCreateRequest via @Size(min=2).
        // The service no longer enforces this — it is enforced by @Valid at the controller.
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("A")
                .purchasePrice(new BigDecimal("299.99"))
                .purchaseDate(LocalDate.of(2024, 6, 1))
                .depreciationRate(new BigDecimal("15.00"))
                .build();

        Set<ConstraintViolation<AssetCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("name") &&
                v.getMessage().contains("between 2 and 150 characters"));
    }

    @Test
    void createAsset_DuplicateName_ThrowsException() {
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(assetRepository.existsByCompanyIdAndName(1L, "Cordless Drill #42")).thenReturn(true);

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(DuplicateNameException.class)
                .hasMessage("Asset name must be unique within the company");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void createAsset_DuplicateAssetTag_ThrowsException() {
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
        when(assetRepository.existsByCompanyIdAndName(1L, "Cordless Drill #42")).thenReturn(false);
        when(assetRepository.existsByCompanyIdAndAssetTag(1L, "TOOL-042")).thenReturn(true);

        assertThatThrownBy(() -> assetService.createAsset(createRequest, 1L))
                .isInstanceOf(DuplicateNameException.class)
                .hasMessage("Asset tag must be unique within the company");

        verify(assetRepository, never()).save(any());
    }

    @Test
    void createAsset_InvalidPurchasePrice_DtoViolation() {
        // Field-level validation is now on AssetCreateRequest via @DecimalMin("0.01").
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("Valid Name")
                .purchasePrice(BigDecimal.ZERO)
                .purchaseDate(LocalDate.of(2024, 6, 1))
                .depreciationRate(new BigDecimal("15.00"))
                .build();

        Set<ConstraintViolation<AssetCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("purchasePrice") &&
                v.getMessage().contains("greater than 0"));
    }

    @Test
    void createAsset_FuturePurchaseDate_DtoViolation() {
        // Field-level validation is now on AssetCreateRequest via @PastOrPresent.
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("Valid Name")
                .purchasePrice(new BigDecimal("299.99"))
                .purchaseDate(LocalDate.now().plusDays(1))
                .depreciationRate(new BigDecimal("15.00"))
                .build();

        Set<ConstraintViolation<AssetCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("purchaseDate"));
    }

    @Test
    void createAsset_InvalidDepreciationRate_DtoViolation() {
        // Field-level validation is now on AssetCreateRequest via @DecimalMax("100.00").
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("Valid Name")
                .purchasePrice(new BigDecimal("299.99"))
                .purchaseDate(LocalDate.of(2024, 6, 1))
                .depreciationRate(new BigDecimal("150"))
                .build();

        Set<ConstraintViolation<AssetCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("depreciationRate") &&
                v.getMessage().contains("must not exceed 100"));
    }

    @Test
    void createAsset_SalvageValueGreaterThanPurchasePrice_ThrowsException() {
        createRequest.setPurchasePrice(new BigDecimal("100"));
        createRequest.setSalvageValue(new BigDecimal("200"));
        when(companyRepository.getReferenceById(1L)).thenReturn(company);
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
                .assignedAt(LocalDateTime.now(ZoneOffset.UTC))
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

        when(assetRepository.countActiveByCompanyId(1L)).thenReturn(2L);
        when(assetRepository.countAvailableByCompanyId(1L)).thenReturn(1L);
        when(assetRepository.findActiveByCompanyId(1L)).thenReturn(Arrays.asList(asset, asset2));

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
        when(assetRepository.countActiveByCompanyId(1L)).thenReturn(0L);
        when(assetRepository.countAvailableByCompanyId(1L)).thenReturn(0L);
        when(assetRepository.findActiveByCompanyId(1L)).thenReturn(List.of());

        AssetStatistics stats = assetService.getStatistics(1L);

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalAssets()).isZero();
        assertThat(stats.getAvailableAssets()).isZero();
        assertThat(stats.getAssetsInUse()).isZero();
        assertThat(stats.getTotalPurchaseValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}