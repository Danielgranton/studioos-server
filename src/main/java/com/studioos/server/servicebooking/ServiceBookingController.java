package com.studioos.server.servicebooking;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.booking.dto.InitiatePaymentRequest;
import com.studioos.server.booking.dto.PaymentInitiationResponse;
import com.studioos.server.servicebooking.dto.CreateServiceBookingRequest;
import com.studioos.server.servicebooking.dto.DecideServiceBookingRequest;
import com.studioos.server.servicebooking.dto.ServiceBookingResponse;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/service-bookings")
@RequiredArgsConstructor
public class ServiceBookingController {
    private final ServiceBookingService service;

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceBookingResponse>> create(
            @AuthenticationPrincipal User user, @Valid @RequestBody CreateServiceBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service request sent", service.create(user, request)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ServiceBookingResponse>>> mine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(service.getMine(user)));
    }

    @GetMapping("/provider")
    public ResponseEntity<ApiResponse<List<ServiceBookingResponse>>> provider(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(service.getForProvider(user)));
    }

    @PatchMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<ServiceBookingResponse>> decide(
            @AuthenticationPrincipal User user, @PathVariable String id,
            @Valid @RequestBody DecideServiceBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.decide(user, id, request)));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<PaymentInitiationResponse>> pay(
            @AuthenticationPrincipal User user, @PathVariable String id,
            @Valid @RequestBody InitiatePaymentRequest request) {
        ServiceBookingResponse booking = service.pay(user, id, request.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.success("M-Pesa prompt sent", PaymentInitiationResponse.builder()
                .transactionId(booking.getTransactionId()).status("PENDING").build()));
    }

    @PatchMapping("/{id}/delivered")
    public ResponseEntity<ApiResponse<ServiceBookingResponse>> delivered(
            @AuthenticationPrincipal User user, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.markDelivered(user, id)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ServiceBookingResponse>> cancel(
            @AuthenticationPrincipal User user, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.cancel(user, id)));
    }
}
