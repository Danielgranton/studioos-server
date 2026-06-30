package com.studioos.server.booking;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.booking.dto.BookingResponse;
import com.studioos.server.booking.dto.ConfirmBookingRequest;
import com.studioos.server.booking.dto.CreateBookingRequest;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingServiceImpl bookingService;

    // ─── Create booking ───
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        BookingResponse response = bookingService.createBooking(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    // ─── Confirm booking ───
    @PatchMapping("/{bookingId}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String bookingId,
            @Valid @RequestBody ConfirmBookingRequest request
    ) {
        BookingResponse response = bookingService.confirmBooking(currentUser, bookingId, request);
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", response));
    }

    // ─── Cancel booking ───
    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String bookingId
    ) {
        bookingService.cancelBooking(currentUser, bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully"));
    }

    // ─── Get booking details ───
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String bookingId
    ) {
        BookingResponse response = bookingService.getBooking(currentUser, bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Get my bookings (as artist) ───
    @GetMapping("/my/bookings")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<BookingResponse> response = bookingService.getMyBookings(currentUser, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Get studio bookings ───
    @GetMapping("/studio/{studioId}")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getStudioBookings(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String studioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<BookingResponse> response = bookingService.getStudioBookings(currentUser, studioId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}