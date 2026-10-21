package com.ktpm.parking.business.support;

import com.ktpm.parking.business.domain.VehicleType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GateRequestHasherTest {

    @Test
    void shouldProduceStableSha256Hash() {
        String first = GateRequestHasher.forEntry("IN-01", "29A12345", VehicleType.MOTORBIKE);
        String second = GateRequestHasher.forEntry("IN-01", "29A12345", VehicleType.MOTORBIKE);

        assertThat(first).hasSize(64).isEqualTo(second);
    }

    @Test
    void shouldDistinguishEventTypeAndPayload() {
        String entry = GateRequestHasher.forEntry("IN-01", "29A12345", VehicleType.MOTORBIKE);
        String otherVehicleType = GateRequestHasher.forEntry("IN-01", "29A12345", VehicleType.CAR);
        String exit = GateRequestHasher.forExit("IN-01", "29A12345");

        assertThat(entry).isNotEqualTo(otherVehicleType).isNotEqualTo(exit);
    }

    @Test
    void shouldHashEquivalentLicensePlateFormatsIdentically() {
        String withHyphen = GateRequestHasher.forEntry(
                "IN-01",
                LicensePlateNormalizer.toKey("29A-12345"),
                VehicleType.MOTORBIKE
        );
        String withSpace = GateRequestHasher.forEntry(
                "IN-01",
                LicensePlateNormalizer.toKey("29a 12345"),
                VehicleType.MOTORBIKE
        );

        assertThat(withHyphen).isEqualTo(withSpace);
    }
}
