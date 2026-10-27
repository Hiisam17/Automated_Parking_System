package com.ktpm.parking.infrastructure.persistence.adapter;

import com.ktpm.parking.business.domain.ParkingSpot;
import com.ktpm.parking.business.domain.ParkingSpotStatus;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.repository.ParkingSpotWorkflowRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class ParkingSpotWorkflowRepositoryAdapter implements ParkingSpotWorkflowRepository {

    private final JdbcTemplate jdbc;

    public ParkingSpotWorkflowRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ParkingSpot> lockFirstAvailable(VehicleType vehicleType) {
        return jdbc.query("""
                SELECT id, code, vehicle_type, occupancy_status, in_maintenance
                FROM parking_spots
                WHERE vehicle_type = ? AND occupancy_status = 'AVAILABLE' AND NOT in_maintenance
                ORDER BY code ASC
                LIMIT 1 FOR UPDATE
                """, (rs, row) -> new ParkingSpot(
                rs.getLong("id"), rs.getString("code"),
                VehicleType.valueOf(rs.getString("vehicle_type")),
                ParkingSpotStatus.valueOf(rs.getString("occupancy_status")),
                rs.getBoolean("in_maintenance")
        ), vehicleType.name()).stream().findFirst();
    }

    @Override
    public boolean markOccupied(Long spotId) {
        return jdbc.update("""
                UPDATE parking_spots SET occupancy_status = 'OCCUPIED'
                WHERE id = ? AND occupancy_status = 'AVAILABLE' AND NOT in_maintenance
                """, spotId) == 1;
    }

    @Override
    public boolean markAvailable(Long spotId) {
        return jdbc.update("""
                UPDATE parking_spots SET occupancy_status = 'AVAILABLE'
                WHERE id = ? AND occupancy_status = 'OCCUPIED'
                """, spotId) == 1;
    }
}
