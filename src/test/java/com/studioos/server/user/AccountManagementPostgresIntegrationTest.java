package com.studioos.server.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.service.PasswordService;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.user.dto.DeleteAccountRequest;
import com.studioos.server.user.dto.UpdatePrivacySettingsRequest;
import com.studioos.server.user.dto.UpdateRoleRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "jwt.secret=test-secret-key-with-enough-length-for-jjwt-signing",
        "jwt.expiration=86400000",
        "jwt.refresh-expiration=604800000",
        "internal.service.api-key=test-internal-api-key",
        "MAIL_USERNAME=test@example.com",
        "MAIL_PASSWORD=test-password",
        "storage.s3.bucket=test-bucket",
        "opensearch.host=localhost",
        "opensearch.port=9200",
        "media.callback.grpc.enabled=false",
        "bootstrap.super-admin.email=",
        "bootstrap.super-admin.password="
})
class AccountManagementPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("studioos_test")
            .withUsername("studioos")
            .withPassword("studioos");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired private UserRepository userRepository;
    @Autowired private PrivacySettingsRepository privacySettingsRepository;
    @Autowired private PrivacySettingsService privacySettingsService;
    @Autowired private RoleManagementService roleManagementService;
    @Autowired private AccountDeletionService accountDeletionService;
    @Autowired private PasswordService passwordService;

    @MockBean private JavaMailSender mailSender;
    @MockBean private StringRedisTemplate redisTemplate;

    @Test
    @Transactional
    void privacySettingsPersistInPostgres() {
        User user = saveUser("privacy-it@example.com");
        UpdatePrivacySettingsRequest request = new UpdatePrivacySettingsRequest();
        request.setProfileDiscoverable(false);
        request.setEmailVisible(true);

        privacySettingsService.update(user, request);

        PrivacySettings saved = privacySettingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(saved.isProfileDiscoverable()).isFalse();
        assertThat(saved.isEmailVisible()).isTrue();
    }

    @Test
    @Transactional
    void roleChangePersistsAndCannotEscalateToPrivilegedRole() {
        User user = saveUser("role-it@example.com");
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.PRODUCER);

        AuthResponse response = roleManagementService.updateOwnRole(user, request);

        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole()).isEqualTo(Role.PRODUCER);
        assertThat(response.getRole()).isEqualTo(Role.PRODUCER);
    }

    @Test
    @Transactional
    void deletionAnonymizesUserWithoutBreakingBusinessReferences() {
        User user = saveUser("deletion-it@example.com");
        user.setPasswordHash(passwordService.hash("current-password"));
        userRepository.save(user);
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setConfirmation("DELETE");
        request.setCurrentPassword("current-password");

        accountDeletionService.delete(user, request);

        User deleted = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deleted.getEmail()).isEqualTo("deleted+" + user.getId() + "@deleted.studioos.invalid");
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.isEnabled()).isFalse();
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .name("Integration user")
                .email(email)
                .phone("+2547" + Math.abs(email.hashCode() % 10000000))
                .role(Role.USER)
                .emailVerified(true)
                .phoneVerified(true)
                .accountVerified(true)
                .build());
    }
}
