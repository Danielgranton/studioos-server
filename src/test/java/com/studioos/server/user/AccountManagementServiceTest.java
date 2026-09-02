package com.studioos.server.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.service.PasswordService;
import com.studioos.server.auth.service.SessionService;
import com.studioos.server.auth.service.TokenService;
import com.studioos.server.shared.audit.AccountAuditService;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.dto.DeleteAccountRequest;
import com.studioos.server.user.dto.UpdatePrivacySettingsRequest;
import com.studioos.server.user.dto.UpdateRoleRequest;

@ExtendWith(MockitoExtension.class)
class AccountManagementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PrivacySettingsRepository privacySettingsRepository;
    @Mock private PasswordService passwordService;
    @Mock private SessionService sessionService;
    @Mock private TokenService tokenService;
    @Mock private AccountAuditService accountAuditService;

    @InjectMocks private PrivacySettingsService privacySettingsService;
    @InjectMocks private RoleManagementService roleManagementService;
    @InjectMocks private AccountDeletionService accountDeletionService;

    @Test
    void privacyUpdatePersistsValuesAndWritesAuditEvent() {
        User user = user(7, Role.USER);
        UpdatePrivacySettingsRequest request = new UpdatePrivacySettingsRequest();
        request.setProfileDiscoverable(false);
        request.setEmailVisible(true);
        when(privacySettingsRepository.findByUserId(7)).thenReturn(Optional.empty());
        when(privacySettingsRepository.save(any(PrivacySettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = privacySettingsService.update(user, request);

        assertThat(response.isProfileDiscoverable()).isFalse();
        assertThat(response.isEmailVisible()).isTrue();
        verify(accountAuditService).record(AuditEventType.PRIVACY_UPDATED, user, "Privacy settings updated");
    }

    @Test
    void selfRoleChangeRotatesSessionsAndIssuesNewRole() {
        User user = user(7, Role.USER);
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.PRODUCER);
        AuthResponse auth = AuthResponse.builder().refreshToken("refresh").role(Role.PRODUCER).build();
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(tokenService.issue(user)).thenReturn(auth);

        AuthResponse response = roleManagementService.updateOwnRole(user, request);

        assertThat(user.getRole()).isEqualTo(Role.PRODUCER);
        assertThat(response).isSameAs(auth);
        verify(sessionService).logoutAllDevices(user);
        verify(sessionService).recordSession(user, "refresh");
        verify(accountAuditService).record(AuditEventType.ROLE_CHANGED, user, "Role changed from USER to PRODUCER");
    }

    @Test
    void selfRoleChangeRejectsPrivilegedRoles() {
        User user = user(7, Role.USER);
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole(Role.SUPER_ADMIN);

        assertThatThrownBy(() -> roleManagementService.updateOwnRole(user, request))
                .isInstanceOf(StudioosException.class)
                .hasMessage("Privileged roles can only be assigned by an authorized administrator");
        verify(userRepository, never()).save(any(User.class));
        verify(sessionService, never()).logoutAllDevices(any(User.class));
    }

    @Test
    void deletionRequiresExplicitConfirmation() {
        User user = user(7, Role.USER);
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setConfirmation("delete");

        assertThatThrownBy(() -> accountDeletionService.delete(user, request))
                .isInstanceOf(StudioosException.class)
                .hasMessage("Type DELETE to confirm account deletion");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deletionAnonymizesUserAndRevokesSessions() {
        User user = user(7, Role.USER);
        user.setEmail("user@example.com");
        user.setPhone("+254700000000");
        user.setUsername("creator");
        user.setPasswordHash("hash");
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setConfirmation("DELETE");
        request.setCurrentPassword("current");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(passwordService.hasPassword(user)).thenReturn(true);
        when(passwordService.matches("current", "hash")).thenReturn(true);

        accountDeletionService.delete(user, request);

        assertThat(user.getEmail()).isEqualTo("deleted+7@deleted.studioos.invalid");
        assertThat(user.getName()).isEqualTo("Deleted user");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.isAccountVerified()).isFalse();
        assertThat(user.getDeletedAt()).isNotNull();
        verify(sessionService).logoutAllDevices(user);
        verify(accountAuditService).record(AuditEventType.ACCOUNT_DELETED, user, "Account deleted and personal data anonymized");
        verify(userRepository).save(user);
    }

    private User user(Integer id, Role role) {
        return User.builder().id(id).name("Test user").email("test@example.com").role(role).build();
    }
}
