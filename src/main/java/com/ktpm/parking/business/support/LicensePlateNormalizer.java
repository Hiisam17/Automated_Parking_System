package com.ktpm.parking.business.support;

import java.util.Locale;

public final class LicensePlateNormalizer {

    private LicensePlateNormalizer() {
    }

    public static String normalize(String licensePlate) {
        return toKey(licensePlate);
    }

    public static String normalizeForDisplay(String licensePlate) {
        validateNotBlank(licensePlate);
        return licensePlate.strip()
                .replaceAll("[\\s\\p{Z}]+", " ")
                .toUpperCase(Locale.ROOT);
    }

    public static String toKey(String licensePlate) {
        validateNotBlank(licensePlate);
        return licensePlate.replaceAll("[\\s\\p{Z}-]+", "")
                .toUpperCase(Locale.ROOT);
    }

    private static void validateNotBlank(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("License plate must not be blank");
        }
    }
}
