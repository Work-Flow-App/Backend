package com.workflow.controller.asset;

import com.workflow.AbstractControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.Role;
import com.workflow.dto.asset.AssetCreateRequest;
import com.workflow.dto.asset.AssetUpdateRequest;
import com.workflow.entity.asset.Asset;
import com.workflow.entity.company.Company;
import com.workflow.entity.auth.User;
import com.workflow.repository.asset.AssetJobAssignmentRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AssetControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private AssetJobAssignmentRepository assetJobAssignmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private Company company;
    private Company anotherCompany;
    private Asset existingAsset;
    private String companyToken;
    private String anotherCompanyToken;
    private String workerToken;

    @BeforeEach
    void setUp() {
        assetJobAssignmentRepository.deleteAll();
        assetRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        User companyUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("assetowner")
                .password(passwordEncoder.encode("password")).email("assetowner@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User anotherUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("anotherassetowner")
                .password(passwordEncoder.encode("password")).email("anotherasset@test.com")
                .role(Role.COMPANY).enabled(true).build());

        User workerUser = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).username("assetworker")
                .password(passwordEncoder.encode("password")).email("assetworker@test.com")
                .role(Role.WORKER).enabled(true).build());

        company = companyRepository.save(Company.builder()
                .name("Test Company").user(companyUser).email("assetowner@test.com").archived(false).build());

        anotherCompany = companyRepository.save(Company.builder()
                .name("Another Company").user(anotherUser).email("anotherasset@test.com").archived(false).build());

        createCompanyMember(company, companyUser, CompanyRole.COMPANY_ADMIN);
        createCompanyMember(anotherCompany, anotherUser, CompanyRole.COMPANY_ADMIN);

        existingAsset = assetRepository.save(Asset.builder()
                .company(company)
                .name("Test Laptop")
                .description("A test laptop")
                .serialNumber("SN-001")
                .assetTag("AT-001")
                .purchasePrice(new BigDecimal("1200.00"))
                .purchaseDate(LocalDate.of(2023, 1, 15))
                .depreciationRate(new BigDecimal("20.00"))
                .salvageValue(new BigDecimal("200.00"))
                .available(true)
                .archived(false)
                .build());

        companyToken = jwtService.generateToken(companyUser);
        anotherCompanyToken = jwtService.generateToken(anotherUser);
        workerToken = jwtService.generateToken(workerUser);
    }

    // ============= POST /api/v1/assets =============

    @Test
    void shouldCreateAssetSuccessfully() throws Exception {
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("New Laptop")
                .description("Brand new laptop")
                .serialNumber("SN-NEW-001")
                .assetTag("AT-NEW-001")
                .purchasePrice(new BigDecimal("2000.00"))
                .purchaseDate(LocalDate.now())
                .depreciationRate(new BigDecimal("25.00"))
                .salvageValue(new BigDecimal("300.00"))
                .build();

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Laptop"))
                .andExpect(jsonPath("$.description").value("Brand new laptop"))
                .andExpect(jsonPath("$.serialNumber").value("SN-NEW-001"))
                .andExpect(jsonPath("$.assetTag").value("AT-NEW-001"))
                .andExpect(jsonPath("$.purchasePrice").value(2000.00))
                .andExpect(jsonPath("$.depreciationRate").value(25.00))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void shouldReturn400WhenAssetNameIsBlank() throws Exception {
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("")
                .purchasePrice(new BigDecimal("1000.00"))
                .purchaseDate(LocalDate.now())
                .depreciationRate(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPurchasePriceIsZero() throws Exception {
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("Asset")
                .purchasePrice(new BigDecimal("0.00"))
                .purchaseDate(LocalDate.now())
                .depreciationRate(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPurchaseDateIsInFuture() throws Exception {
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("Asset")
                .purchasePrice(new BigDecimal("1000.00"))
                .purchaseDate(LocalDate.now().plusDays(1))
                .depreciationRate(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403WhenCreatingAssetWithoutToken() throws Exception {
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("Asset")
                .purchasePrice(new BigDecimal("1000.00"))
                .purchaseDate(LocalDate.now())
                .depreciationRate(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForWorkerRoleOnCreate() throws Exception {
        // WORKER users have no company context; CompanyRoleAspect rejects with 403
        AssetCreateRequest request = AssetCreateRequest.builder()
                .name("Asset Name")
                .purchasePrice(new BigDecimal("1000.00"))
                .purchaseDate(LocalDate.now())
                .depreciationRate(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============= PUT /api/v1/assets/{id} =============

    @Test
    void shouldUpdateAssetSuccessfully() throws Exception {
        AssetUpdateRequest request = AssetUpdateRequest.builder()
                .name("Updated Laptop")
                .description("Updated description")
                .serialNumber("SN-UPDATED")
                .assetTag("AT-UPDATED")
                .salvageValue(new BigDecimal("150.00"))
                .build();

        mockMvc.perform(put("/api/v1/assets/" + existingAsset.getId())
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingAsset.getId()))
                .andExpect(jsonPath("$.name").value("Updated Laptop"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.serialNumber").value("SN-UPDATED"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentAsset() throws Exception {
        AssetUpdateRequest request = AssetUpdateRequest.builder().name("Asset").build();

        mockMvc.perform(put("/api/v1/assets/99999")
                        .header("Authorization", "Bearer " + companyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingAnotherCompanyAsset() throws Exception {
        AssetUpdateRequest request = AssetUpdateRequest.builder().name("Updated").build();

        mockMvc.perform(put("/api/v1/assets/" + existingAsset.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/assets/{id} =============

    @Test
    void shouldGetAssetByIdSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/assets/" + existingAsset.getId())
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingAsset.getId()))
                .andExpect(jsonPath("$.name").value("Test Laptop"))
                .andExpect(jsonPath("$.serialNumber").value("SN-001"))
                .andExpect(jsonPath("$.assetTag").value("AT-001"))
                .andExpect(jsonPath("$.purchasePrice").value(1200.00))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void shouldReturn404WhenAssetNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/assets/99999")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAccessingAnotherCompanyAsset() throws Exception {
        mockMvc.perform(get("/api/v1/assets/" + existingAsset.getId())
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/assets (paginated) =============

    @Test
    void shouldListAssetsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/assets")
                        .header("Authorization", "Bearer " + companyToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Test Laptop"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void shouldListAvailableAssetsOnly() throws Exception {
        mockMvc.perform(get("/api/v1/assets")
                        .header("Authorization", "Bearer " + companyToken)
                        .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void shouldReturnEmptyPageForAnotherCompany() throws Exception {
        mockMvc.perform(get("/api/v1/assets")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void shouldReturn403ForMissingAuthOnList() throws Exception {
        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isForbidden());
    }

    // ============= PATCH /api/v1/assets/{id}/archive =============

    @Test
    void shouldArchiveAssetSuccessfully() throws Exception {
        mockMvc.perform(patch("/api/v1/assets/" + existingAsset.getId() + "/archive")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNoContent());

        Asset archived = assetRepository.findById(existingAsset.getId()).orElseThrow();
        assert archived.isArchived();
    }

    @Test
    void shouldReturn404WhenArchivingNonExistentAsset() throws Exception {
        mockMvc.perform(patch("/api/v1/assets/99999/archive")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenArchivingAnotherCompanyAsset() throws Exception {
        mockMvc.perform(patch("/api/v1/assets/" + existingAsset.getId() + "/archive")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/assets/{id}/value =============

    @Test
    void shouldGetAssetValueSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/assets/" + existingAsset.getId() + "/value")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value(existingAsset.getId()))
                .andExpect(jsonPath("$.assetName").value("Test Laptop"))
                .andExpect(jsonPath("$.purchasePrice").value(1200.00))
                .andExpect(jsonPath("$.currentValue").exists())
                .andExpect(jsonPath("$.depreciationRate").exists());
    }

    @Test
    void shouldGetAssetValueWithAsOfDate() throws Exception {
        mockMvc.perform(get("/api/v1/assets/" + existingAsset.getId() + "/value")
                        .header("Authorization", "Bearer " + companyToken)
                        .param("asOf", "2024-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value(existingAsset.getId()));
    }

    @Test
    void shouldReturn400ForInvalidAsOfDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/assets/" + existingAsset.getId() + "/value")
                        .header("Authorization", "Bearer " + companyToken)
                        .param("asOf", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenGettingValueForNonExistentAsset() throws Exception {
        mockMvc.perform(get("/api/v1/assets/99999/value")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenGettingValueForAnotherCompanyAsset() throws Exception {
        mockMvc.perform(get("/api/v1/assets/" + existingAsset.getId() + "/value")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isNotFound());
    }

    // ============= GET /api/v1/assets/statistics =============

    @Test
    void shouldGetAssetStatisticsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/assets/statistics")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets").value(1))
                .andExpect(jsonPath("$.availableAssets").value(1))
                .andExpect(jsonPath("$.assetsInUse").value(0))
                .andExpect(jsonPath("$.totalPurchaseValue").exists());
    }

    @Test
    void shouldReturnZeroStatisticsForCompanyWithNoAssets() throws Exception {
        mockMvc.perform(get("/api/v1/assets/statistics")
                        .header("Authorization", "Bearer " + anotherCompanyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets").value(0))
                .andExpect(jsonPath("$.availableAssets").value(0));
    }

    @Test
    void shouldReturn401ForInvalidTokenOnStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/assets/statistics")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }
}
