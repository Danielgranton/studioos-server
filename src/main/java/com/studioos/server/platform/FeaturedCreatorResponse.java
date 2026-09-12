package com.studioos.server.platform;

import com.studioos.server.shared.enums.Role;

public record FeaturedCreatorResponse(
        Integer id,
        String name,
        Role role,
        String profileImageThumbnail,
        boolean verified
) {
}
