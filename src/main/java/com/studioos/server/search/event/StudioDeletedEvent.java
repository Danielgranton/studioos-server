package com.studioos.server.search.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudioDeletedEvent {
    private String studioId;
}
