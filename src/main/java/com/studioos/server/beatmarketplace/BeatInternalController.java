package com.studioos.server.beatmarketplace;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.beatmarketplace.dto.MediaJobCallbackRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/beats")
@RequiredArgsConstructor
public class BeatInternalController {

    private final BeatService beatService;

    @PostMapping("/processing-callback")
    public ResponseEntity<Void> processingCallback(@RequestBody MediaJobCallbackRequest callback) {
        beatService.handleJobCallback(callback);
        return ResponseEntity.ok().build();
    }
}