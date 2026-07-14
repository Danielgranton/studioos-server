package com.studioos.server.search.event;

import com.studioos.server.shared.enums.SearchEntityType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewCreatedEvent {
    private SearchEntityType entityType;
    private String entityId;
}
