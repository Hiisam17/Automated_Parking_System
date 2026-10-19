package com.ktpm.parking.infrastructure.persistence.entity;

import com.ktpm.parking.business.domain.ParkingSpotStatus;
import com.ktpm.parking.business.domain.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parking_spots")
public class ParkingSpotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupancy_status", nullable = false, length = 20)
    private ParkingSpotStatus occupancyStatus;

    @Column(name = "in_maintenance", nullable = false)
    private boolean inMaintenance;

    protected ParkingSpotEntity() {
    }

    public ParkingSpotEntity(Long id, String code, VehicleType vehicleType,
                             ParkingSpotStatus occupancyStatus, boolean inMaintenance) {
        this.id = id;
        this.code = code;
        this.vehicleType = vehicleType;
        this.occupancyStatus = occupancyStatus;
        this.inMaintenance = inMaintenance;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public ParkingSpotStatus getOccupancyStatus() {
        return occupancyStatus;
    }

    public boolean isInMaintenance() {
        return inMaintenance;
    }
}
