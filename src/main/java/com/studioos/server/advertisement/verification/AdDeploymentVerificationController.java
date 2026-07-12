package com.studioos.server.advertisement.verification;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/ads/verification")
@RequiredArgsConstructor
public class AdDeploymentVerificationController {

    private final AdDeploymentVerificationService verificationService;

    @PostMapping("/media")
    public MediaProbeResponse probeMedia(@RequestBody MediaProbeRequest request) {
        return verificationService.probeMedia(request);
    }

    @PostMapping("/storage")
    public StorageProbeResponse probeStorage(@RequestBody StorageProbeRequest request) {
        return verificationService.probeStorage(request);
    }

    @PostMapping("/payment")
    public PaymentSmokeResponse smokePayment(@Valid @RequestBody PaymentSmokeRequest request) {
        return verificationService.smokePayment(request);
    }
}
