package com.designer.costume.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.designer.costume.dto.response.CategoryResponse;
import com.designer.costume.dto.response.CostumeResponse;
import com.designer.costume.dto.response.CostumeVariantResponse;
import com.designer.costume.entity.category.CategoryEntity;
import com.designer.costume.entity.costume.CostumeEntity;
import com.designer.costume.entity.costume.CostumeImageEntity;
import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.entity.user.RefreshTokenEntity;
import com.designer.costume.entity.variant.ColorEntity;
import com.designer.costume.entity.variant.CostumeVariantEntity;
import com.designer.costume.entity.variant.SizeEntity;
import com.designer.costume.enums.CostumeImageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.designer.costume.repository.category.CategoryRepository;
import com.designer.costume.repository.costume.CostumeImageRepository;
import com.designer.costume.repository.costume.CostumeRepository;
import com.designer.costume.repository.user.UserRepository;
import com.designer.costume.repository.user.RefreshTokenRepository;
import com.designer.costume.repository.user.UserRolePermissionGrantRepository;
import com.designer.costume.repository.variant.ColorRepository;
import com.designer.costume.repository.variant.CostumeVariantRepository;
import com.designer.costume.repository.variant.SizeRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.designer.costume.security.JwtService;
import com.designer.costume.security.JwtTokenType;
import com.designer.costume.security.TokenHashing;
import java.time.Instant;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DatabaseArchitectureIntegrationTest {

    private static EmbeddedPostgres postgres;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CostumeRepository costumeRepository;

    @Autowired
    private CostumeImageRepository costumeImageRepository;

    @Autowired
    private ColorRepository colorRepository;

    @Autowired
    private SizeRepository sizeRepository;

    @Autowired
    private CostumeVariantRepository costumeVariantRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRolePermissionGrantRepository permissionGrantRepository;

    @Autowired
    private JwtService jwtService;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        try {
            postgres = EmbeddedPostgres.builder().start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start isolated PostgreSQL test database", exception);
        }

        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("security.jwt.secret", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("security.jwt.issuer", () -> "designer-costume-api-test");
        registry.add("app.auth.password-reset-delivery", () -> "development");
        registry.add("app.auth.cookie-secure", () -> "false");
    }

    @AfterAll
    static void stopDatabase() throws IOException {
        if (postgres != null) {
            postgres.close();
        }
    }

    @Test
    void flywayCreatesAllExpectedTablesAndSeedsAccessControl() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                """, String.class)).containsExactlyInAnyOrder(
                "users", "roles", "user_roles", "permissions", "role_permissions",
                "categories", "costumes", "costume_images", "colors", "sizes",
                "costume_variants", "user_photos", "try_on_jobs", "try_on_results",
                "favorites", "inquiries", "refresh_tokens", "password_reset_tokens",
                "user_role_permission_grants", "admin_activity_events");
        assertThat(count("SELECT COUNT(*) FROM roles")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM permissions")).isEqualTo(15);
        assertThat(count("""
                SELECT COUNT(*)
                FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                WHERE r.name = 'SUPER_ADMIN'
                """)).isEqualTo(15);
        assertThat(count("""
                SELECT COUNT(*)
                FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                WHERE r.name = 'ADMIN'
                """)).isEqualTo(14);
        assertThat(count("""
                SELECT COUNT(*)
                FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                WHERE r.name = 'CUSTOMER'
                """)).isEqualTo(2);
    }

    @Test
    @Transactional
    void jpaPersistsUuidAndAuditFieldsAndFindsEmailIgnoringCase() {
        UserEntity user = new UserEntity();
        user.setFullName("Repository Test");
        user.setEmail("  Repository@Example.Test ");
        user.setPasswordHash("test-only-hash");

        UserEntity saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("repository@example.test");
        assertThat(userRepository.findByEmailIgnoreCase("REPOSITORY@EXAMPLE.TEST"))
                .contains(saved);
    }

    @Test
    @Transactional
    void emailIsUniqueWithoutCaseSensitivity() {
        insertUser(UUID.randomUUID(), "first@example.test");

        assertThrows(DataIntegrityViolationException.class,
                () -> insertUser(UUID.randomUUID(), "FIRST@example.test"));
    }

    @Test
    @Transactional
    void costumeRequiresAnExistingCategory() {
        UUID costumeId = UUID.randomUUID();

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO costumes (id, name, sku, category_id, base_price)
                VALUES (?, 'Test Costume', 'TEST-FK', ?, 100.00)
                """, costumeId, UUID.randomUUID()));
    }

    @Test
    @Transactional
    void aUserCannotFavoriteTheSameCostumeTwice() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = insertCategory();
        UUID costumeId = insertCostume(categoryId, "FAVORITE-TEST");
        insertUser(userId, "favorite@example.test");
        insertFavorite(userId, costumeId);

        assertThrows(DataIntegrityViolationException.class,
                () -> insertFavorite(userId, costumeId));
    }

    @Test
    @Transactional
    void aCostumeColorSizeCombinationIsUnique() {
        UUID categoryId = insertCategory();
        UUID costumeId = insertCostume(categoryId, "VARIANT-TEST");
        UUID colorId = UUID.randomUUID();
        UUID sizeId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO colors (id, name, hex_code) VALUES (?, 'Test Color', '#123456')
                """, colorId);
        jdbcTemplate.update("""
                INSERT INTO sizes (id, name, display_order) VALUES (?, 'TEST_SIZE', 99)
                """, sizeId);
        insertVariant(costumeId, colorId, sizeId, "VARIANT-ONE");

        assertThrows(DataIntegrityViolationException.class,
                () -> insertVariant(costumeId, colorId, sizeId, "VARIANT-TWO"));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = {"CATEGORY_VIEW", "COSTUME_VIEW"})
    void categoryProjectionAndReadApisSearchActiveCategoriesWithoutRecursiveRelations() throws Exception {
        CategoryEntity parent = new CategoryEntity();
        parent.setName("Wedding");
        categoryRepository.saveAndFlush(parent);

        CategoryEntity child = new CategoryEntity();
        child.setName("Wedding Lehenga");
        child.setParentCategory(parent);
        categoryRepository.saveAndFlush(child);

        CategoryEntity inactive = new CategoryEntity();
        inactive.setName("Inactive Wedding");
        inactive.setActive(false);
        categoryRepository.saveAndFlush(inactive);

        var result = categoryRepository.findActiveCategories(
                "lehenga", PageRequest.of(0, 10, Sort.by("name")));

        assertThat(result.getContent()).containsExactly(new CategoryResponse(
                child.getId(), "Wedding Lehenga", null, null, parent.getId(), "Wedding"));

        mockMvc.perform(get("/api/v1/categories").param("search", "lehenga"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "success": true,
                          "data": {
                            "content": [
                              {
                                "id": "%s",
                                "name": "Wedding Lehenga",
                                "parentCategoryId": "%s",
                                "parentCategoryName": "Wedding"
                              }
                            ],
                            "totalElements": 1
                          }
                        }
                        """.formatted(child.getId(), parent.getId()), false));

        mockMvc.perform(get("/api/v1/categories/{id}", child.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "success": true,
                          "data": {
                            "id": "%s",
                            "name": "Wedding Lehenga",
                            "parentCategoryId": "%s"
                          }
                        }
                        """.formatted(child.getId(), parent.getId()), false));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = {"CATEGORY_VIEW", "COSTUME_VIEW"})
    void costumeReadApisAndProjectionsIncludeCatalogDataAndFilterUnavailableVariants() throws Exception {
        CategoryEntity category = new CategoryEntity();
        category.setName("API Test Category");
        categoryRepository.saveAndFlush(category);

        CostumeEntity costume = new CostumeEntity();
        costume.setName("API Bridal Costume");
        costume.setSku("API-COSTUME-" + UUID.randomUUID());
        costume.setCategory(category);
        costume.setBasePrice(new BigDecimal("1250.00"));
        costumeRepository.saveAndFlush(costume);

        CostumeImageEntity image = new CostumeImageEntity();
        image.setCostume(costume);
        image.setImageUrl("https://images.example.test/api-bridal.jpg");
        image.setImageType(CostumeImageType.PRODUCT);
        image.setPrimary(true);
        costumeImageRepository.saveAndFlush(image);

        ColorEntity color = new ColorEntity();
        color.setName("API Test Red");
        color.setHexCode("#AABBCC");
        colorRepository.saveAndFlush(color);

        SizeEntity size = new SizeEntity();
        size.setName("API_TEST_SIZE");
        size.setDisplayOrder(99);
        sizeRepository.saveAndFlush(size);

        SizeEntity unavailableSize = new SizeEntity();
        unavailableSize.setName("API_UNAVAILABLE_SIZE");
        unavailableSize.setDisplayOrder(100);
        sizeRepository.saveAndFlush(unavailableSize);

        CostumeVariantEntity available = new CostumeVariantEntity();
        available.setCostume(costume);
        available.setColor(color);
        available.setSize(size);
        available.setPrice(new BigDecimal("1350.00"));
        available.setSku("API-VARIANT-" + UUID.randomUUID());
        costumeVariantRepository.saveAndFlush(available);

        CostumeVariantEntity unavailable = new CostumeVariantEntity();
        unavailable.setCostume(costume);
        unavailable.setColor(color);
        unavailable.setSize(unavailableSize);
        unavailable.setPrice(new BigDecimal("1350.00"));
        unavailable.setSku("API-VARIANT-" + UUID.randomUUID());
        unavailable.setAvailable(false);
        costumeVariantRepository.saveAndFlush(unavailable);

        var costumes = costumeRepository.findActiveCostumes(
                "bridal", category.getId(), PageRequest.of(0, 10, Sort.by("name")));
        List<CostumeVariantResponse> variants =
                costumeVariantRepository.findAvailableVariantsByCostumeId(costume.getId());

        assertThat(costumes.getContent()).containsExactly(new CostumeResponse(
                costume.getId(),
                "API Bridal Costume",
                null,
                costume.getSku(),
                new BigDecimal("1250.00"),
                category.getId(),
                "API Test Category",
                image.getImageUrl()));
        assertThat(variants).containsExactly(new CostumeVariantResponse(
                available.getId(),
                costume.getId(),
                "API Test Red",
                "#AABBCC",
                "API_TEST_SIZE",
                99,
                new BigDecimal("1350.00"),
                true));

        mockMvc.perform(get("/api/v1/costumes")
                        .param("search", "bridal")
                        .param("categoryId", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "success": true,
                          "data": {
                            "content": [
                              {
                                "id": "%s",
                                "name": "API Bridal Costume",
                                "categoryId": "%s",
                                "categoryName": "API Test Category",
                                "primaryImageUrl": "%s"
                              }
                            ],
                            "totalElements": 1
                          }
                        }
                        """.formatted(costume.getId(), category.getId(), image.getImageUrl()), false));

        mockMvc.perform(get("/api/v1/costumes/{id}", costume.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "success": true,
                          "data": {
                            "id": "%s",
                            "name": "API Bridal Costume",
                            "categoryName": "API Test Category"
                          }
                        }
                        """.formatted(costume.getId()), false));

        mockMvc.perform(get("/api/v1/costumes/{id}/variants", costume.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "success": true,
                          "data": [
                            {
                              "id": "%s",
                              "colorName": "API Test Red",
                              "sizeName": "API_TEST_SIZE",
                              "available": true
                            }
                          ]
                        }
                        """.formatted(available.getId()), false));
    }

    @Test
    @Transactional
    void adminDashboardReturnsLiveMetricsAndRequiresAdministratorRole() throws Exception {
        long activeCategoriesBefore = categoryRepository.countByActiveTrue();
        long activeCostumesBefore = costumeRepository.countByActiveTrue();
        long activeCustomersBefore = userRepository.countActiveCustomers();
        long inquiriesBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inquiries", Long.class);
        long newInquiriesBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inquiries WHERE status = 'NEW'", Long.class);
        UUID categoryId = insertCategory();
        String categoryName = categoryRepository.findById(categoryId).orElseThrow().getName();
        insertCostume(categoryId, "DASHBOARD-COSTUME-" + UUID.randomUUID());
        insertUserWithRole(
                UUID.randomUUID(), "Dashboard Customer", "dashboard-customer@example.test",
                "DashboardCustomer1!", "CUSTOMER");
        insertUserWithRole(
                UUID.randomUUID(), "Dashboard Admin", "dashboard-admin@example.test",
                "DashboardAdmin1!", "ADMIN");

        String accessToken = login("dashboard-admin@example.test", "DashboardAdmin1!");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeCategories").value(activeCategoriesBefore + 1))
                .andExpect(jsonPath("$.data.activeCostumes").value(activeCostumesBefore + 1))
                .andExpect(jsonPath("$.data.activeCustomers").value(activeCustomersBefore + 1))
                .andExpect(jsonPath("$.data.totalInquiries").value(inquiriesBefore))
                .andExpect(jsonPath("$.data.newInquiries").value(newInquiriesBefore))
                .andExpect(jsonPath("$.data.costumeDistribution[0].categoryName").value(categoryName))
                .andExpect(jsonPath("$.data.costumeDistribution[0].costumeCount").value(1));

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void adminCatalogCanManageCategoriesCostumesAndInventory() throws Exception {
        var admin = withAuthorities(
                "ROLE_ADMIN", "USER_VIEW", "CATEGORY_VIEW", "CATEGORY_CREATE", "CATEGORY_UPDATE", "CATEGORY_DELETE",
                "COSTUME_VIEW", "COSTUME_CREATE", "COSTUME_UPDATE", "COSTUME_DELETE");
        MvcResult categoryResult = mockMvc.perform(post("/api/v1/admin/categories")
                        .with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "name", "Managed Category " + UUID.randomUUID(),
                                "description", "Managed category",
                                "active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();
        UUID categoryId = UUID.fromString(objectMapper.readTree(
                categoryResult.getResponse().getContentAsString()).path("data").path("id").asText());
        mockMvc.perform(get("/api/v1/admin/activity").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("CREATED"))
                .andExpect(jsonPath("$.data[0].resourceType").value("CATEGORY"))
                .andExpect(jsonPath("$.data[0].resourceId").value(categoryId.toString()));

        MvcResult colorResult = mockMvc.perform(post("/api/v1/admin/colors")
                        .with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "name", "Managed Color " + UUID.randomUUID(),
                                "hexCode", "#12AB34",
                                "active", true))))
                .andExpect(status().isOk())
                .andReturn();
        UUID colorId = UUID.fromString(objectMapper.readTree(
                colorResult.getResponse().getContentAsString()).path("data").path("id").asText());

        MvcResult sizeResult = mockMvc.perform(post("/api/v1/admin/sizes")
                        .with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "name", "Size-" + UUID.randomUUID().toString().substring(0, 8),
                                "displayOrder", 5,
                                "active", true))))
                .andExpect(status().isOk())
                .andReturn();
        UUID sizeId = UUID.fromString(objectMapper.readTree(
                sizeResult.getResponse().getContentAsString()).path("data").path("id").asText());

        MvcResult costumeResult = mockMvc.perform(post("/api/v1/admin/costumes")
                        .with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "name", "Managed Costume",
                                "sku", "MANAGED-" + UUID.randomUUID(),
                                "categoryId", categoryId,
                                "basePrice", new BigDecimal("100.00"),
                                "primaryImageUrl", "https://images.example.test/managed.jpg",
                                "active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryImageUrl")
                        .value("https://images.example.test/managed.jpg"))
                .andReturn();
        UUID costumeId = UUID.fromString(objectMapper.readTree(
                costumeResult.getResponse().getContentAsString()).path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/admin/costumes/{costumeId}/variants", costumeId)
                        .with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "colorId", colorId,
                                "sizeId", sizeId,
                                "sku", "MANAGED-VARIANT-" + UUID.randomUUID(),
                                "price", new BigDecimal("105.00"),
                                "stockQuantity", 4,
                                "available", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(4))
                .andExpect(jsonPath("$.data.available").value(true));

        mockMvc.perform(get("/api/v1/admin/costumes/{costumeId}/variants", costumeId).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/admin/costumes/{id}", costumeId).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/admin/categories/{id}", categoryId).with(admin))
                .andExpect(status().isOk());
    }

    @Test
    void customerCatalogPermissionsDoNotGrantAccessToAdminCatalogRoutes() throws Exception {
        JsonNode registered = register(
                "admin-boundary-" + UUID.randomUUID() + "@example.test", "mobile");
        String customerToken = registered.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(roles = "SUPER_ADMIN")
    void rolePermissionRemovalRevokesSnapshotsAndReaddingDoesNotRestoreThem() throws Exception {
        UUID userId = UUID.randomUUID();
        insertUserWithRole(userId, "Snapshot User", "snapshot-user@example.test",
                "SnapshotUser1!", "ADMIN");
        assertThat(permissionGrantRepository.findEffectivePermissions(userId))
                .contains("CATEGORY_VIEW");
        UUID adminRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'ADMIN'", UUID.class);

        Set<String> remainingPermissions = Set.copyOf(jdbcTemplate.queryForList("""
                SELECT permission.name
                FROM role_permissions role_permission
                JOIN roles role ON role.id = role_permission.role_id
                JOIN permissions permission ON permission.id = role_permission.permission_id
                WHERE role.name = 'ADMIN' AND permission.name <> 'CATEGORY_VIEW'
                """, String.class));
        updateRolePermissions(adminRoleId, remainingPermissions);
        assertThat(permissionGrantRepository.findEffectivePermissions(userId))
                .doesNotContain("CATEGORY_VIEW");

        Set<String> restoredRolePermissions = new java.util.HashSet<>(remainingPermissions);
        restoredRolePermissions.add("CATEGORY_VIEW");
        updateRolePermissions(adminRoleId, restoredRolePermissions);
        assertThat(permissionGrantRepository.findEffectivePermissions(userId))
                .doesNotContain("CATEGORY_VIEW");
    }

    @Test
    @Transactional
    void adminCanSearchAndUpdateUsersThroughProtectedEndpoints() throws Exception {
        UUID adminId = UUID.randomUUID();
        String adminEmail = "user-management-admin-" + adminId + "@example.test";
        insertUserWithRole(adminId, "User Management Admin", adminEmail, "AdminPass1!", "ADMIN");
        UUID customerId = UUID.randomUUID();
        String customerEmail = "managed-customer-" + customerId + "@example.test";
        insertUserWithRole(customerId, "Before Update", customerEmail, "CustomerPass1!", "CUSTOMER");
        String accessToken = login(adminEmail, "AdminPass1!");

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("search", customerEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value(customerEmail));

        mockMvc.perform(put("/api/v1/admin/users/{id}", customerId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "fullName", "After Update",
                                "email", customerEmail,
                                "phoneNumber", "555-0101",
                                "roles", List.of("CUSTOMER"),
                                "active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("After Update"))
                .andExpect(jsonPath("$.data.phoneNumber").value("555-0101"));

        mockMvc.perform(get("/api/v1/admin/users/{id}", customerId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("After Update"));
    }

    @Test
    @Transactional
    void adminCanSearchInquiriesAndUpdateTheirStatus() throws Exception {
        var admin = withAuthorities("ROLE_ADMIN", "INQUIRY_VIEW", "INQUIRY_UPDATE");
        UUID inquiryId = UUID.randomUUID();
        String email = "inquiry-" + inquiryId + "@example.test";
        jdbcTemplate.update("""
                INSERT INTO inquiries (id, customer_name, email, message)
                VALUES (?, 'Inquiry Customer', ?, 'Please share more details.')
                """, inquiryId, email);

        mockMvc.perform(get("/api/v1/admin/inquiries")
                        .with(admin)
                        .param("search", email)
                        .param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("NEW"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/admin/inquiries/{id}/status", inquiryId).with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("status", "CONTACTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONTACTED"));
    }

    @Test
    void openApiDocumentsHealthAndCatalogEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/categories")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/admin/dashboard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/costumes/{costumeId}/variants")));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void registrationAuthenticationRefreshReuseDetectionAndCurrentUserWork() throws Exception {
        String email = "auth-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/costumes"))
                .andExpect(status().isOk());
        JsonNode registered = register(email, "mobile");
        String accessToken = registered.path("data").path("accessToken").asText();
        String firstRefreshToken = registered.path("data").path("refreshToken").asText();
        assertThat(firstRefreshToken).isNotBlank();
        assertThat(registered.path("data").path("user").path("roles").toString())
                .contains("CUSTOMER");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "fullName", "Unauthorized Admin",
                                "email", "unauthorized-" + UUID.randomUUID() + "@example.test",
                                "password", "StrongPass1!",
                                "roles", List.of("CUSTOMER")))))
                .andExpect(status().isForbidden());

        JsonNode rotated = mobileRefresh(firstRefreshToken);
        String nextRefreshToken = rotated.path("data").path("refreshToken").asText();
        assertThat(nextRefreshToken).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", nextRefreshToken))))
                .andExpect(status().isOk());
    }

    @Test
    void passwordResetInvalidatesRefreshSessionsAndWebRefreshRequiresCsrf() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.test";
        JsonNode registered = register(email, "mobile");
        String oldRefreshToken = registered.path("data").path("refreshToken").asText();

        MvcResult forgotResult = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of("email", email))))
                .andExpect(status().isOk())
                .andReturn();
        String resetToken = objectMapper.readTree(forgotResult.getResponse().getContentAsString())
                .path("data").path("developmentResetToken").asText();
        assertThat(resetToken).isNotBlank();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "token", resetToken,
                                "newPassword", "ChangedPass2!"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "token", resetToken,
                                "newPassword", "ChangedPass2!"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", oldRefreshToken))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "email", email, "password", "ChangedPass2!"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "https://attacker.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "email", email, "password", "ChangedPass2!"))))
                .andExpect(status().isForbidden());

        MvcResult browserLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "email", email, "password", "ChangedPass2!"))))
                .andExpect(status().isOk())
                .andReturn();
        String refreshCookie = responseCookieValue(browserLogin, "refresh_token");
        String csrfCookie = responseCookieValue(browserLogin, "csrf_token");
        assertThat(responseCookieHeader(browserLogin, "refresh_token"))
                .contains("HttpOnly", "SameSite=Strict");
        assertThat(responseCookieHeader(browserLogin, "csrf_token"))
                .doesNotContain("HttpOnly")
                .contains("Path=/");
        assertThat(responseCookieHeader(browserLogin, "refresh_token"))
                .contains("Path=/api/v1/auth");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshCookie),
                                new Cookie("csrf_token", csrfCookie)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshCookie),
                                new Cookie("csrf_token", csrfCookie))
                        .header("X-CSRF-TOKEN", csrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void adminCanCreateOnlyUsersWhoseRolesAreWithinTheirPermissions() throws Exception {
        UUID adminId = UUID.randomUUID();
        String adminEmail = "admin-" + adminId + "@example.test";
        insertUserWithRole(adminId, "API Test Admin", adminEmail, "AdminPass1!", "ADMIN");

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "email", adminEmail, "password", "AdminPass1!"))))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "fullName", "Created Customer",
                                "email", "created-" + UUID.randomUUID() + "@example.test",
                                "password", "StrongPass1!",
                                "roles", List.of("CUSTOMER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"));

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "fullName", "Escalated User",
                                "email", "escalated-" + UUID.randomUUID() + "@example.test",
                                "password", "StrongPass1!",
                                "roles", List.of("SUPER_ADMIN")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void futureRolePermissionChangesDoNotExceedThePermissionsCapturedAtAssignment()
            throws Exception {
        UUID adminId = UUID.randomUUID();
        String adminEmail = "role-admin-" + adminId + "@example.test";
        insertUserWithRole(adminId, "Role Test Admin", adminEmail, "AdminPass1!", "ADMIN");
        String customerEmail = "future-role-" + UUID.randomUUID() + "@example.test";

        String adminToken = login(adminEmail, "AdminPass1!");
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "fullName", "Bounded Customer",
                                "email", customerEmail,
                                "password", "StrongPass1!",
                                "roles", List.of("CUSTOMER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("USER_DELETE"))));

        jdbcTemplate.update("""
                INSERT INTO role_permissions (role_id, permission_id)
                SELECT role.id, permission.id
                FROM roles role CROSS JOIN permissions permission
                WHERE role.name = 'CUSTOMER' AND permission.name = 'USER_DELETE'
                ON CONFLICT DO NOTHING
                """);
        try {
            String customerToken = login(customerEmail, "StrongPass1!");
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.permissions").value(
                            org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("USER_DELETE"))));
        } finally {
            jdbcTemplate.update("""
                    DELETE FROM role_permissions
                    WHERE role_id = (SELECT id FROM roles WHERE name = 'CUSTOMER')
                      AND permission_id = (SELECT id FROM permissions WHERE name = 'USER_DELETE')
                    """);
        }
    }

    @Test
    void deactivatedUserCannotUsePreviouslyIssuedAccessOrRefreshTokens() throws Exception {
        JsonNode registered = register(
                "inactive-" + UUID.randomUUID() + "@example.test", "mobile");
        UUID userId = UUID.fromString(registered.path("data").path("user").path("id").asText());
        String accessToken = registered.path("data").path("accessToken").asText();
        String refreshToken = registered.path("data").path("refreshToken").asText();
        jdbcTemplate.update("UPDATE users SET is_active = FALSE WHERE id = ?", userId);

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void replayRevokesOnlyTheReplayedLoginFamily() throws Exception {
        String email = "family-" + UUID.randomUUID() + "@example.test";
        JsonNode firstLogin = register(email, "mobile");
        String firstFamilyToken = firstLogin.path("data").path("refreshToken").asText();
        String independentFamilyToken = objectMapper.readTree(mockMvc.perform(
                        post("/api/v1/auth/login")
                                .header("X-Client-Platform", "mobile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                        "email", email, "password", "StrongPass1!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        String rotatedFamilyToken = mobileRefresh(firstFamilyToken)
                .path("data").path("refreshToken").asText();
        jdbcTemplate.update(
                "UPDATE refresh_tokens SET revoked_at = ? WHERE token_hash = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(10)),
                TokenHashing.sha256(firstFamilyToken));
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", firstFamilyToken))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", rotatedFamilyToken))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", independentFamilyToken))))
                .andExpect(status().isOk());
    }

    @Test
    void simultaneousRefreshReuseRevokesOnlyThatSessionFamily() throws Exception {
        String email = "parallel-refresh-" + UUID.randomUUID() + "@example.test";
        JsonNode registered = register(email, "mobile");
        String sharedRefreshToken = registered.path("data").path("refreshToken").asText();
        String otherFamilyToken = objectMapper.readTree(mockMvc.perform(
                        post("/api/v1/auth/login")
                                .header("X-Client-Platform", "mobile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                        "email", email, "password", "StrongPass1!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> first = executor.submit(() -> refreshRequest(start, sharedRefreshToken));
            Future<MvcResult> second = executor.submit(() -> refreshRequest(start, sharedRefreshToken));
            start.countDown();
            MvcResult firstResult = first.get();
            MvcResult secondResult = second.get();
            assertThat(List.of(
                    firstResult.getResponse().getStatus(),
                    secondResult.getResponse().getStatus()))
                    .containsExactlyInAnyOrder(200, 401);
            MvcResult successfulResult = firstResult.getResponse().getStatus() == 200
                    ? firstResult
                    : secondResult;
            String replacementToken = objectMapper
                    .readTree(successfulResult.getResponse().getContentAsString())
                    .path("data").path("refreshToken").asText();
            assertThat(replacementToken).isNotBlank();
            mobileRefresh(replacementToken);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .header("X-Client-Platform", "mobile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(
                                    java.util.Map.of("refreshToken", otherFamilyToken))))
                    .andExpect(status().isOk());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentPasswordResetAndRefreshCannotLeaveAnActiveSession() throws Exception {
        String email = "race-" + UUID.randomUUID() + "@example.test";
        JsonNode registered = register(email, "mobile");
        String refreshToken = registered.path("data").path("refreshToken").asText();
        String resetToken = requestResetToken(email);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> reset = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/v1/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                        "token", resetToken,
                                        "newPassword", "ConcurrentReset2!"))))
                        .andReturn();
            });
            Future<MvcResult> refresh = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/v1/auth/refresh")
                                .header("X-Client-Platform", "mobile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(
                                        java.util.Map.of("refreshToken", refreshToken))))
                        .andReturn();
            });
            start.countDown();
            assertThat(reset.get().getResponse().getStatus()).isEqualTo(200);
            MvcResult refreshResult = refresh.get();
            assertThat(refreshResult.getResponse().getStatus()).isIn(200, 401);
            if (refreshResult.getResponse().getStatus() == 200) {
                String descendant = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                        .path("data").path("refreshToken").asText();
                mockMvc.perform(post("/api/v1/auth/refresh")
                                .header("X-Client-Platform", "mobile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(
                                        java.util.Map.of("refreshToken", descendant))))
                        .andExpect(status().isUnauthorized());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void passwordResetTokensAreHashedAndExpiredTokensAreRejected() throws Exception {
        String email = "expired-reset-" + UUID.randomUUID() + "@example.test";
        register(email, "mobile");
        String resetToken = requestResetToken(email);
        String tokenHash = TokenHashing.sha256(resetToken);
        assertThat(count("SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash = '"
                + tokenHash + "'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash = '"
                + resetToken + "'")).isZero();
        jdbcTemplate.update(
                "UPDATE password_reset_tokens SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE token_hash = ?",
                tokenHash);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "token", resetToken, "newPassword", "ExpiredReset2!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsDoesNotGrantCredentialsToUnconfiguredOrigins() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(
                        "/api/v1/auth/refresh")
                        .header("Origin", "https://attacker.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(result -> assertThat(result.getResponse()
                                .getHeader("Access-Control-Allow-Origin"))
                        .isNull())
                .andExpect(result -> assertThat(result.getResponse()
                                .getHeader("Access-Control-Allow-Credentials"))
                        .isNull());
    }

    @Test
    void expiredRevokedRefreshTokenReuseStillRevokesItsActiveFamily() throws Exception {
        JsonNode registered = register(
                "expired-reuse-" + UUID.randomUUID() + "@example.test", "mobile");
        UUID userId = UUID.fromString(registered.path("data").path("user").path("id").asText());
        UserEntity user = userRepository.findById(userId).orElseThrow();
        String expiredToken = jwtService.generate(
                userId, JwtTokenType.REFRESH, UUID.randomUUID(), -1_000);

        RefreshTokenEntity oldToken = refreshTokenRepository.findByTokenHash(
                TokenHashing.sha256(registered.path("data").path("refreshToken").asText()))
                .orElseThrow();
        oldToken.setTokenHash(TokenHashing.sha256(expiredToken));
        oldToken.setExpiresAt(Instant.now().minusSeconds(1));
        oldToken.setRevokedAt(Instant.now().minusSeconds(10));
        refreshTokenRepository.saveAndFlush(oldToken);

        String activeDescendant = jwtService.generate(
                userId,
                JwtTokenType.REFRESH,
                UUID.randomUUID(),
                jwtService.refreshExpirationMillis());
        RefreshTokenEntity descendant = new RefreshTokenEntity();
        descendant.setFamilyId(oldToken.getFamilyId());
        descendant.setUser(user);
        descendant.setTokenHash(TokenHashing.sha256(activeDescendant));
        descendant.setExpiresAt(Instant.now().plusMillis(jwtService.refreshExpirationMillis()));
        refreshTokenRepository.saveAndFlush(descendant);
        oldToken.setReplacedBy(descendant.getId());
        refreshTokenRepository.saveAndFlush(oldToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", expiredToken))))
                .andExpect(status().isUnauthorized());

        assertThat(refreshTokenRepository.findById(descendant.getId()).orElseThrow().getRevokedAt())
                .isNotNull();
    }

    @Test
    void weakPasswordIsRejectedAndLogoutRevokesRefreshFamily() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "fullName", "Invalid Password",
                                "email", "weak-" + UUID.randomUUID() + "@example.test",
                                "password", "Password1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        JsonNode registered = register(
                "logout-" + UUID.randomUUID() + "@example.test", "mobile");
        String refreshToken = registered.path("data").path("refreshToken").asText();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode register(String email, String platform) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Client-Platform", platform)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "fullName", "Auth Test",
                                "email", email,
                                "password", "StrongPass1!"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode mobileRefresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private MvcResult refreshRequest(CountDownLatch start, String refreshToken) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                java.util.Map.of("refreshToken", refreshToken))))
                .andReturn();
    }

    private String requestResetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of("email", email))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("developmentResetToken").asText();
    }

    private void insertUserWithRole(
            UUID userId, String name, String email, String password, String roleName) {
        jdbcTemplate.update("""
                INSERT INTO users (id, full_name, email, password_hash, is_active)
                VALUES (?, ?, ?, ?, TRUE)
                """, userId, name, email, passwordEncoder.encode(password));
        jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT ?, role.id FROM roles role WHERE role.name = ?
                """, userId, roleName);
        permissionGrantRepository.snapshotCurrentRolePermissions(userId);
    }

    private void updateRolePermissions(UUID roleId, Set<String> permissions) throws Exception {
        mockMvc.perform(put("/api/v1/admin/roles/{id}", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of(
                                "name", "ADMIN",
                                "description", "Administrator role",
                                "permissions", permissions))))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor withAuthorities(
            String... authorityNames) {
        GrantedAuthority[] authorities = Arrays.stream(authorityNames)
                .map(SimpleGrantedAuthority::new)
                .toArray(GrantedAuthority[]::new);
        return user("test-admin").authorities(authorities);
    }

    private String responseCookieValue(MvcResult result, String name) {
        return responseCookieHeader(result, name)
                .substring(name.length() + 1, responseCookieHeader(result, name).indexOf(';'));
    }

    private String responseCookieHeader(MvcResult result, String name) {
        return result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith(name + "="))
                .reduce((first, last) -> last)
                .orElseThrow(() -> new AssertionError("Missing " + name + " cookie"));
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private void insertUser(UUID userId, String email) {
        jdbcTemplate.update("""
                INSERT INTO users (id, full_name, email, password_hash)
                VALUES (?, 'Test User', ?, 'test-only-hash')
                """, userId, email);
    }

    private UUID insertCategory() {
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO categories (id, name) VALUES (?, ?)
                """, categoryId, "Test Category " + categoryId);
        return categoryId;
    }

    private UUID insertCostume(UUID categoryId, String sku) {
        UUID costumeId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO costumes (id, name, sku, category_id, base_price)
                VALUES (?, 'Test Costume', ?, ?, 100.00)
                """, costumeId, sku, categoryId);
        return costumeId;
    }

    private void insertFavorite(UUID userId, UUID costumeId) {
        jdbcTemplate.update("""
                INSERT INTO favorites (id, user_id, costume_id) VALUES (?, ?, ?)
                """, UUID.randomUUID(), userId, costumeId);
    }

    private void insertVariant(UUID costumeId, UUID colorId, UUID sizeId, String sku) {
        jdbcTemplate.update("""
                INSERT INTO costume_variants (id, costume_id, color_id, size_id, price, sku)
                VALUES (?, ?, ?, ?, 100.00, ?)
                """, UUID.randomUUID(), costumeId, colorId, sizeId, sku);
    }
}
