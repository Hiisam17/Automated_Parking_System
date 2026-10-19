package com.ktpm.parking.business.support;

import java.util.Locale;

public final class LicensePlateNormalizer {

    private LicensePlateNormalizer() {
    }

    public static String normalize(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("License plate must not be blank");
        }
        return licensePlate.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
