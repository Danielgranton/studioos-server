package com.studioos.server.search.mapper;

import com.studioos.server.search.document.ProducerDocument;
import com.studioos.server.search.dto.ProducerSearchResult;
import com.studioos.server.user.User;

public final class ProducerMapper {
    private ProducerMapper() {
    }

    public static ProducerDocument toDocument(User producer, Double averageRating, Integer reviewCount) {
        return ProducerDocument.builder()
                .id(producer.getId())
                .name(producer.getName())
                .location(producer.getLocation())
                .genre(producer.getGenre())
                .bio(producer.getBio())
                .profileImage(producer.getProfileImage())
                .profileImageThumbnail(producer.getProfileImageThumbnail())
                .verified(producer.isAccountVerified())
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .createdAt(producer.getCreatedAt() != null ? producer.getCreatedAt().toString() : null)
                .build();
    }

    public static ProducerSearchResult toResult(ProducerDocument doc, Double score) {
        return ProducerSearchResult.builder()
                .id(doc.getId())
                .name(doc.getName())
                .location(doc.getLocation())
                .genre(doc.getGenre())
                .bio(doc.getBio())
                .profileImage(doc.getProfileImage())
                .profileImageThumbnail(doc.getProfileImageThumbnail())
                .verified(doc.getVerified())
                .studioNames(doc.getStudioNames())
                .studioCount(doc.getStudioCount())
                .available(doc.getAvailable())
                .startingPrice(doc.getStartingPrice())
                .responseTime(doc.getResponseTime())
                .services(doc.getServices())
                .averageRating(doc.getAverageRating())
                .reviewCount(doc.getReviewCount())
                .score(score)
                .build();
    }
}
