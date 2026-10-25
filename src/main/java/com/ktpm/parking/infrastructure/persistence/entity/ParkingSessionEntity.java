package com.ktpm.parking.infrastructure.persistence.entity;

import com.ktpm.parking.business.domain.ParkingSessionStatus;
import com.ktpm.parking.business.domain.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "parking_sessions")
public class ParkingSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false, length = 20)
    private String licensePlate;

    @Column(name = "license_plate_key", nullable = false, length = 20)
    private String licensePlateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_spot_id", nullable = false)
    private ParkingSpotEntity parkingSpot;

    @Column(name = "check_in_time", nullable = false)
    private OffsetDateTime checkInTime;

    @Column(name = "check_out_time")
    private OffsetDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParkingSessionStatus status;

    protected ParkingSessionEntity() {
    }

    public ParkingSessionEntity(Long id, String licensePlate, String licensePlateKey, VehicleType vehicleType,
                                ParkingSpotEntity parkingSpot, OffsetDateTime checkInTime,
                                OffsetDateTime checkOutTime, ParkingSessionStatus status) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.licensePlateKey = licensePlateKey;
        this.vehicleType = vehicleType;
        this.parkingSpot = parkingSpot;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getLicensePlateKey() {
        return licensePlateKey;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public ParkingSpotEntity getParkingSpot() {
        return parkingSpot;
    }

    public OffsetDateTime getCheckInTime() {
        return checkInTime;
    }

    public OffsetDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public ParkingSessionStatus getStatus() {
        return status;
    }
}
