package com.studioos.server.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.dto.PublicUserResponse;
import com.studioos.server.user.dto.UpdateProfileRequest;
import com.studioos.server.user.dto.UserProfileResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    // ─── Get any user's public profile ───
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getUserById(@PathVariable Integer id) {
        PublicUserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}