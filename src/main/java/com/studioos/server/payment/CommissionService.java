package com.studioos.server.payment;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.studioos.server.shared.enums.CommissionContext;

@Service
public class CommissionService {

    // Rates expressed in basis points (1% = 100 bps) to avoid floating point math on money.
    private static final Map<CommissionContext, Integer> RATES_BPS = Map.of(
            CommissionContext.BOOKING, 1000, // 10%
            CommissionContext.BEAT, 500      // 5%
    );

    public int getRateBasisPoints(CommissionContext context) {
        Integer bps = RATES_BPS.get(context);
        if (bps == null) {
            throw new IllegalStateException("No commission rate configured for " + context);
        }
        return bps;
    }

    public int calculateCommission(int amount, CommissionContext context) {
        int bps = getRateBasisPoints(context);
        return (amount * bps) / 10000;
    }

    public int calculateNetAmount(int amount, CommissionContext context) {
        return amount - calculateCommission(amount, context);
    }
}