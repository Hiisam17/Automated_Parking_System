package com.ktpm.parking.business.support;

import com.ktpm.parking.business.domain.GateEventType;
import com.ktpm.parking.business.domain.VehicleType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class GateRequestHasher {

    private GateRequestHasher() {
    }

    public static String forEntry(String gateId, String licensePlateKey, VehicleType vehicleType) {
        return hash(GateEventType.ENTRY.name(), gateId, licensePlateKey, vehicleType.name());
    }

    public static String forExit(String gateId, String licensePlateKey) {
        return hash(GateEventType.EXIT.name(), gateId, licensePlateKey);
    }

    private static String hash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
