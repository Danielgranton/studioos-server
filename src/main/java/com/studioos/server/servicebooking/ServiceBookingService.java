package com.studioos.server.servicebooking;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.artist.ArtistServiceOffering;
import com.studioos.server.artist.ArtistServiceOfferingRepository;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.payment.PaymentService;
import com.studioos.server.payment.Transaction;
import com.studioos.server.servicebooking.dto.CreateServiceBookingRequest;
import com.studioos.server.servicebooking.dto.DecideServiceBookingRequest;
import com.studioos.server.servicebooking.dto.ServiceBookingResponse;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.events.TransactionResolvedEvent;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceBookingService {
    private final ServiceBookingRepository repository;
    private final ArtistServiceOfferingRepository artistOfferings;
    private final StudioRepository studioRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final NotificationServiceImpl notificationService;

    @Transactional
    public ServiceBookingResponse create(User requester, CreateServiceBookingRequest request) {
        if (requester == null || requester.getId() == null || requester.getRole() == Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Sign in with an artist account to request a service");
        }
        Integer amount;
        String currency;
        String providerType = request.getProviderType().trim().toUpperCase();
        if ("ARTIST".equals(providerType)) {
            ArtistServiceOffering offer = artistOfferings.findByIdAndArtistId(request.getListingId(), request.getProviderId())
                    .filter(ArtistServiceOffering::isActive)
                    .orElseThrow(() -> StudioosException.notFound("Artist service is no longer available"));
            amount = offer.getPrice();
            currency = offer.getCurrency();
        } else if ("PRODUCER".equals(providerType)) {
            Studio studio = studioRepository.findById(request.getStudioId() == null ? request.getListingId() : request.getStudioId())
                    .orElseThrow(() -> StudioosException.notFound("Studio service is no longer available"));
            var offer = studio.getServices().stream().filter(service ->
                    request.getCatalogServiceId() != null
                            ? request.getCatalogServiceId().equals(service.getCatalogServiceId())
                                    || service.getName().equalsIgnoreCase(request.getServiceName())
                            : service.getName().equalsIgnoreCase(request.getServiceName()))
                    .filter(com.studioos.server.studio.StudioService::isActive)
                    .findFirst().orElse(null);
            if (!studio.getOwnerId().equals(request.getProviderId()) || !studio.isAvailable() || offer == null) {
                throw StudioosException.notFound("Studio service is no longer available");
            }
            amount = offer.isIncludedInProductionPackage() ? studio.getProductionPackagePrice() : offer.getPrice();
            if (amount == null || amount < 1) {
                throw StudioosException.conflict("This provider has not set a price for the selected service");
            }
            currency = "KES";
        } else {
            throw StudioosException.badRequest("Provider type must be ARTIST or PRODUCER");
        }
        if (requester.getId().equals(request.getProviderId())) {
            throw StudioosException.badRequest("You cannot request a service from your own account");
        }
        ServiceBooking booking = repository.save(ServiceBooking.builder()
                .providerId(request.getProviderId())
                .providerType(providerType)
                .listingId(request.getListingId())
                .studioId(request.getStudioId())
                .serviceName(request.getServiceName().trim())
                .requesterId(requester.getId())
                .preferredDate(request.getPreferredDate())
                .requestDetails(request.getRequestDetails().trim())
                .amount(amount)
                .currency(currency == null || currency.isBlank() ? "KES" : currency)
                .build());
        notifyUser(booking.getProviderId(), NotificationType.SERVICE_BOOKING_REQUEST,
                "New service request", requester.getName() + " requested " + booking.getServiceName() + ".", booking.getId());
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<ServiceBookingResponse> getMine(User user) {
        return repository.findByRequesterIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceBookingResponse> getForProvider(User user) {
        if (user.getRole() != Role.ARTIST && user.getRole() != Role.PRODUCER && user.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Only service providers can view service requests");
        }
        return repository.findByProviderIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ServiceBookingResponse decide(User user, String id, DecideServiceBookingRequest request) {
        ServiceBooking booking = ownedByProvider(user, id);
        if (booking.getStatus() != ServiceBookingStatus.PENDING) {
            throw StudioosException.conflict("This request has already been answered");
        }
        if (!Boolean.TRUE.equals(request.getAccepted())) {
            booking.setStatus(ServiceBookingStatus.DECLINED);
            repository.save(booking);
            notifyUser(booking.getRequesterId(), NotificationType.SERVICE_BOOKING_UPDATE,
                    "Service request declined", "Your request for " + booking.getServiceName() + " was declined.", id);
        } else {
            if (request.getAmount() == null || request.getAmount() < 1) {
                throw StudioosException.badRequest("Enter the agreed service price");
            }
            booking.setAmount(request.getAmount());
            booking.setStatus(ServiceBookingStatus.ACCEPTED);
            repository.save(booking);
            notifyUser(booking.getRequesterId(), NotificationType.SERVICE_BOOKING_UPDATE,
                    "Service request accepted", "Your provider accepted " + booking.getServiceName()
                            + " at " + booking.getCurrency() + " " + request.getAmount() + ".", id);
        }
        return toResponse(booking);
    }

    @Transactional
    public ServiceBookingResponse pay(User user, String id, String phone) {
        ServiceBooking booking = repository.findByIdForUpdate(id)
                .orElseThrow(() -> StudioosException.notFound("Service booking not found"));
        if (!booking.getRequesterId().equals(user.getId())) throw StudioosException.forbidden("This request is not yours");
        if (booking.getStatus() != ServiceBookingStatus.ACCEPTED) {
            throw StudioosException.conflict("The provider must accept and price this request before payment");
        }
        booking.setStatus(ServiceBookingStatus.PAYMENT_PENDING);
        repository.save(booking);
        try {
            Transaction transaction = paymentService.initiateServiceBookingPayment(user.getId(), booking.getStudioId(),
                    booking.getAmount(), phone, booking.getId(), "Service booking: " + booking.getServiceName());
            booking.setTransactionId(transaction.getId());
        } catch (RuntimeException exception) {
            booking.setStatus(ServiceBookingStatus.ACCEPTED);
            repository.save(booking);
            throw exception;
        }
        return toResponse(repository.save(booking));
    }

    @Transactional
    public ServiceBookingResponse markDelivered(User user, String id) {
        ServiceBooking booking = ownedByProvider(user, id);
        if (booking.getStatus() != ServiceBookingStatus.PAID) {
            throw StudioosException.conflict("Only paid service bookings can be marked delivered");
        }
        booking.setStatus(ServiceBookingStatus.DELIVERED);
        repository.save(booking);
        notifyUser(booking.getRequesterId(), NotificationType.SERVICE_BOOKING_UPDATE,
                "Service delivered", booking.getServiceName() + " has been marked delivered.", id);
        return toResponse(booking);
    }

    @Transactional
    public ServiceBookingResponse cancel(User user, String id) {
        ServiceBooking booking = repository.findByIdForUpdate(id)
                .orElseThrow(() -> StudioosException.notFound("Service booking not found"));
        boolean requester = booking.getRequesterId().equals(user.getId());
        boolean provider = booking.getProviderId().equals(user.getId());
        if (!requester && !provider && user.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("This request is not yours to cancel");
        }
        if (booking.getStatus() != ServiceBookingStatus.PENDING && booking.getStatus() != ServiceBookingStatus.ACCEPTED) {
            throw StudioosException.conflict("This request can no longer be cancelled");
        }
        booking.setStatus(ServiceBookingStatus.CANCELLED);
        repository.save(booking);
        notifyUser(requester ? booking.getProviderId() : booking.getRequesterId(), NotificationType.SERVICE_BOOKING_UPDATE,
                "Service request cancelled", "The " + booking.getServiceName() + " request was cancelled.", id);
        return toResponse(booking);
    }

    @EventListener
    @Transactional
    public void onTransactionResolved(TransactionResolvedEvent event) {
        if (event.getType() != com.studioos.server.shared.enums.TransactionType.SERVICE_BOOKING_PAYMENT) return;
        ServiceBooking booking = repository.findByTransactionId(event.getTransactionId()).orElse(null);
        if (booking == null || booking.getStatus() != ServiceBookingStatus.PAYMENT_PENDING) return;
        booking.setStatus(event.isSuccess() ? ServiceBookingStatus.PAID : ServiceBookingStatus.ACCEPTED);
        repository.save(booking);
        if (event.isSuccess()) {
            notifyUser(booking.getProviderId(), NotificationType.SERVICE_BOOKING_UPDATE,
                    "Service booking paid", "Payment is confirmed for " + booking.getServiceName() + ".", booking.getId());
        } else {
            notifyUser(booking.getRequesterId(), NotificationType.SERVICE_BOOKING_UPDATE,
                    "Payment not completed", "You can retry payment for " + booking.getServiceName() + ".", booking.getId());
        }
    }

    private ServiceBooking ownedByProvider(User user, String id) {
        ServiceBooking booking = repository.findByIdForUpdate(id)
                .orElseThrow(() -> StudioosException.notFound("Service booking not found"));
        if (!booking.getProviderId().equals(user.getId()) && user.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("This request belongs to another provider");
        }
        return booking;
    }

    private ServiceBookingResponse toResponse(ServiceBooking booking) {
        User provider = userRepository.findById(booking.getProviderId()).orElse(null);
        User requester = userRepository.findById(booking.getRequesterId()).orElse(null);
        return ServiceBookingResponse.builder().id(booking.getId()).providerId(booking.getProviderId())
                .providerType(booking.getProviderType()).providerName(provider == null ? "Provider" : provider.getName())
                .listingId(booking.getListingId()).studioId(booking.getStudioId()).serviceName(booking.getServiceName())
                .requesterId(booking.getRequesterId()).requesterName(requester == null ? "StudioOS member" : requester.getName())
                .preferredDate(booking.getPreferredDate()).requestDetails(booking.getRequestDetails())
                .amount(booking.getAmount()).currency(booking.getCurrency()).transactionId(booking.getTransactionId())
                .status(booking.getStatus()).createdAt(booking.getCreatedAt()).updatedAt(booking.getUpdatedAt()).build();
    }

    private void notifyUser(Integer userId, NotificationType type, String title, String message, String bookingId) {
        try {
            CreateNotificationRequest request = new CreateNotificationRequest();
            request.setUserId(userId);
            request.setType(type);
            request.setTitle(title);
            request.setMessage(message);
            request.setRelatedEntityId(bookingId);
            notificationService.createNotification(request);
        } catch (Exception exception) {
            log.warn("Could not notify user {} about service booking {}: {}", userId, bookingId, exception.getMessage());
        }
    }
}
