package com.studioos.server.shared.events;

import com.studioos.server.shared.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TransactionResolvedEvent {
    private String transactionId;
    private TransactionType type;
    private boolean success;
}