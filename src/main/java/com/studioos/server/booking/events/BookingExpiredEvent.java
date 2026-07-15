package com.studioos.server.booking.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BookingExpiredEvent {
    private String bookingId;
    private String studioId;
    private Integer artistId;
}
