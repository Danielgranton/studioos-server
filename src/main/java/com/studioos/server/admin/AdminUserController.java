package com.studioos.server.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.RoleManagementService;
import com.studioos.server.user.User;
import com.studioos.server.user.dto.UpdateRoleRequest;
import com.studioos.server.user.dto.UserProfileResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final RoleManagementService roleManagementService;

    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserRole(
            @AuthenticationPrincipal User actor,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        User updated = roleManagementService.updateUserRole(actor, userId, request);
        UserProfileResponse response = UserProfileResponse.builder()
                .id(updated.getId())
                .name(updated.getName())
                .username(updated.getUsername())
                .email(updated.getEmail())
                .phone(updated.getPhone())
                .role(updated.getRole())
                .build();
        return ResponseEntity.ok(ApiResponse.success("User role updated", response));
    }
}
