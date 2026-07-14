package com.studioos.server.advertisement.targeting;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.studioos.server.advertisement.campaign.AdCampaign;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.advertisement.targeting.dto.TargetingResponse;
import com.studioos.server.advertisement.targeting.dto.UpdateTargetingRequest;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TargetingService {

    private final TargetingRepository targetingRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<TargetingResponse> getTargeting(Integer advertiserId, String campaignId) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
                .orElseThrow(() -> StudioosException.notFound("Campaign not found"));

        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw StudioosException.forbidden("You do not own this campaign");
        }

        return targetingRepository.findByCampaignId(campaignId).map(this::toResponse);
    }

    @Transactional
    public TargetingResponse upsertTargeting(Integer advertiserId, String campaignId, UpdateTargetingRequest request) {
        AdCampaign campaign = adCampaignRepository.findById(campaignId)
                .orElseThrow(() -> StudioosException.notFound("Campaign not found"));

        if (!campaign.getAdvertiserId().equals(advertiserId)) {
            throw StudioosException.forbidden("You do not own this campaign");
        }

        Targeting targeting = targetingRepository.findByCampaignId(campaignId)
                .orElseGet(() -> Targeting.builder().campaignId(campaignId).build());

        targeting.setCountries(normalizeCsv(request.getCountries()));
        targeting.setCities(normalizeCsv(request.getCities()));
        targeting.setGenres(normalizeCsv(request.getGenres()));
        targeting.setAgeMin(request.getAgeMin());
        targeting.setAgeMax(request.getAgeMax());
        targeting.setGender(normalize(request.getGender()));
        targeting.setInterests(normalizeCsv(request.getInterests()));
        targeting.setDevices(normalizeCsv(request.getDevices()));

        return toResponse(targetingRepository.save(targeting));
    }

    @Transactional(readOnly = true)
    public boolean matchesUser(String campaignId, Integer userId) {
        Optional<Targeting> targeting = targetingRepository.findByCampaignId(campaignId);
        if (targeting.isEmpty() || !hasRestrictions(targeting.get())) {
            return true;
        }

        if (userId == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        return matches(targeting.get(), user);
    }

    @Transactional(readOnly = true)
    public boolean matches(Targeting targeting, User user) {
        if (targeting == null || !hasRestrictions(targeting)) {
            return true;
        }

        if (user == null) {
            return false;
        }

        if (!matchesText(targeting.getCountries(), user.getLocation())) {
            return false;
        }
        if (!matchesText(targeting.getCities(), user.getLocation())) {
            return false;
        }
        if (!matchesText(targeting.getGenres(), user.getGenre())) {
            return false;
        }
        if (!matchesAge(targeting, user)) {
            return false;
        }
        if (!matchesText(targeting.getGender(), readSetting(user.getSettings(), "gender"))) {
            return false;
        }
        if (!matchesText(targeting.getInterests(), readSetting(user.getSettings(), "interests"))) {
            return false;
        }
        if (!matchesText(targeting.getDevices(), readSetting(user.getSettings(), "device"))) {
            return false;
        }

        return true;
    }

    public boolean hasRestrictions(Targeting targeting) {
        return Stream.of(
                        targeting.getCountries(),
                        targeting.getCities(),
                        targeting.getGenres(),
                        targeting.getGender(),
                        targeting.getInterests(),
                        targeting.getDevices(),
                        targeting.getAgeMin(),
                        targeting.getAgeMax())
                .anyMatch(value -> value != null && !value.toString().isBlank());
    }

    private TargetingResponse toResponse(Targeting targeting) {
        return TargetingResponse.builder()
                .campaignId(targeting.getCampaignId())
                .countries(targeting.getCountries())
                .cities(targeting.getCities())
                .genres(targeting.getGenres())
                .ageMin(targeting.getAgeMin())
                .ageMax(targeting.getAgeMax())
                .gender(targeting.getGender())
                .interests(targeting.getInterests())
                .devices(targeting.getDevices())
                .build();
    }

    private boolean matchesAge(Targeting targeting, User user) {
        Integer age = readInteger(user.getSettings(), "age");
        if (targeting.getAgeMin() != null && age != null && age < targeting.getAgeMin()) {
            return false;
        }
        if (targeting.getAgeMax() != null && age != null && age > targeting.getAgeMax()) {
            return false;
        }
        if ((targeting.getAgeMin() != null || targeting.getAgeMax() != null) && age == null) {
            return false;
        }
        return true;
    }

    private String readSetting(JsonNode settings, String key) {
        if (settings == null || !settings.hasNonNull(key)) {
            return null;
        }

        JsonNode value = settings.get(key);
        if (value.isTextual()) {
            return value.asText();
        }

        if (value.isArray()) {
            return StreamSupport.stream(value.spliterator(), false)
                    .map(j -> j.asText())
                    .collect(Collectors.joining(","));
        }

        return value.asText();
    }

    private Integer readInteger(JsonNode settings, String key) {
        if (settings == null || !settings.hasNonNull(key)) {
            return null;
        }
        JsonNode value = settings.get(key);
        if (value.isInt() || value.isLong()) {
            return value.intValue();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private boolean matchesText(String targets, String actual) {
        if (targets == null || targets.isBlank()) {
            return true;
        }

        if (actual == null || actual.isBlank()) {
            return false;
        }

        String normalizedActual = normalize(actual);
        Set<String> tokens = splitCsv(targets);
        for (String token : tokens) {
            String normalizedToken = normalize(token);
            if (normalizedActual.contains(normalizedToken) || normalizedToken.contains(normalizedActual)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> splitCsv(String value) {
        return Stream.of(value.split(","))
                .map(s -> s.trim())
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizeCsv(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return splitCsv(value).stream().collect(Collectors.joining(","));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
