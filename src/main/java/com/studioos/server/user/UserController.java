package com.studioos.server.user;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.dto.PublicUserResponse;
import com.studioos.server.user.dto.UpdateProfileRequest;
import com.studioos.server.user.dto.UpdateUsernameRequest;
import com.studioos.server.user.dto.UserProfileResponse;
import com.studioos.server.user.dto.PrivacySettingsResponse;
import com.studioos.server.user.dto.UpdatePrivacySettingsRequest;
import com.studioos.server.user.dto.UpdateRoleRequest;
import com.studioos.server.auth.AuthCookieService;
import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.user.dto.DeleteAccountRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PrivacySettingsService privacySettingsService;
    private final RoleManagementService roleManagementService;
    private final AuthCookieService authCookieService;
    private final AccountDeletionService accountDeletionService;
    private final AccountDataExportService accountDataExportService;

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountDataExportResponse> exportAccountData(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"studioos-account-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(accountDataExportService.export(currentUser));
    }

    // ─── Get own profile ───
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser
    ) {
        UserProfileResponse profile = userService.getMyProfile(currentUser);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // ─── Update own profile ───
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse updated = userService.updateProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PutMapping("/username")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUsername(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UpdateUsernameRequest request
    ) {
        UserProfileResponse updated = userService.updateUsername(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Username updated successfully", updated));
    }

    @PostMapping(value = "/profile/image", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfileImage(
            @AuthenticationPrincipal User currentUser,
            @RequestPart("file") MultipartFile file
    ) {
        UserProfileResponse updated = userService.updateProfileImage(currentUser, file);
        return ResponseEntity.ok(ApiResponse.success("Profile image updated successfully", updated));
    }

    @GetMapping("/privacy")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> getPrivacySettings(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(privacySettingsService.get(currentUser)));
    }

    @PutMapping("/privacy")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> updatePrivacySettings(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UpdatePrivacySettingsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Privacy settings updated", privacySettingsService.update(currentUser, request)));
    }

    @PutMapping("/role")
    public ResponseEntity<ApiResponse<AuthResponse>> updateOwnRole(
            @AuthenticationPrincipal User currentUser,
            @jakarta.validation.Valid @RequestBody UpdateRoleRequest request,
            jakarta.servlet.http.HttpServletResponse httpResponse
    ) {
        AuthResponse response = roleManagementService.updateOwnRole(currentUser, request);
        authCookieService.addAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success("Role updated", response));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal User currentUser,
            @jakarta.validation.Valid @RequestBody DeleteAccountRequest request,
            jakarta.servlet.http.HttpServletResponse httpResponse
    ) {
        accountDeletionService.delete(currentUser, request);
        authCookieService.clearAuthCookies(httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Account deleted"));
    }

    // ─── Get any user's public profile ───
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getUserById(@PathVariable Integer id) {
        PublicUserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
