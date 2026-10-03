package com.studioos.server.servicecatalog;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.servicecatalog.dto.CreateCustomServiceRequest;
import com.studioos.server.servicecatalog.dto.ServiceCatalogResponse;
import com.studioos.server.servicecatalog.dto.ServiceProviderResponse;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/services/catalog")
@RequiredArgsConstructor
public class ServiceCatalogController {
    private final ServiceCatalogService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceCatalogResponse>>> getCatalog(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(service.getAvailable(user == null ? null : user.getRole())));
    }

    @PostMapping("/custom")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> createCustom(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateCustomServiceRequest request) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Service added to StudioOS catalog", service.createCustom(user, request)));
    }

    @GetMapping("/{slug}/providers")
    public ResponseEntity<ApiResponse<List<ServiceProviderResponse>>> getProviders(
            @org.springframework.web.bind.annotation.PathVariable String slug,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String providerType) {
        return ResponseEntity.ok(ApiResponse.success(service.getProviders(slug, providerType)));
    }
}
