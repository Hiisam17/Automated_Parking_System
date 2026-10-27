package com.ktpm.parking.infrastructure.persistence.adapter;

import com.ktpm.parking.business.domain.ParkingSession;
import com.ktpm.parking.business.domain.ParkingSessionStatus;
import com.ktpm.parking.business.domain.VehicleType;
import com.ktpm.parking.business.repository.ParkingSessionWorkflowRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class ParkingSessionWorkflowRepositoryAdapter implements ParkingSessionWorkflowRepository {

    private static final RowMapper<ParkingSession> SESSION_MAPPER = (rs, row) -> {
        OffsetDateTime checkOutTime = rs.getObject("check_out_time", OffsetDateTime.class);
        return new ParkingSession(
                rs.getLong("id"), rs.getString("license_plate"),
                VehicleType.valueOf(rs.getString("vehicle_type")), rs.getLong("parking_spot_id"),
                rs.getString("spot_code"),
                rs.getObject("check_in_time", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                checkOutTime == null ? null : checkOutTime.withOffsetSameInstant(ZoneOffset.UTC),
                ParkingSessionStatus.valueOf(rs.getString("status"))
        );
    };

    private final JdbcTemplate jdbc;

    public ParkingSessionWorkflowRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existsActiveByLicensePlate(String licensePlateKey) {
        lockLicensePlate(licensePlateKey);
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM parking_sessions
                WHERE license_plate_key = ? AND status = 'ACTIVE')
                """, Boolean.class, licensePlateKey));
    }

    @Override
    public Optional<ParkingSession> lockActiveByLicensePlate(String licensePlateKey) {
        lockLicensePlate(licensePlateKey);
        return jdbc.query("""
                SELECT s.*, p.code AS spot_code
                FROM parking_sessions s JOIN parking_spots p ON p.id = s.parking_spot_id
                WHERE s.license_plate_key = ? AND s.status = 'ACTIVE'
                FOR UPDATE OF s
                """, SESSION_MAPPER, licensePlateKey).stream().findFirst();
    }

    @Override
    public ParkingSession createActive(String licensePlate, String licensePlateKey,
                                       VehicleType vehicleType, Long spotId, OffsetDateTime checkInTime) {
        // Also protect direct callers; the business workflow already owns this lock.
        lockLicensePlate(licensePlateKey);
        return jdbc.queryForObject("""
                INSERT INTO parking_sessions
                    (license_plate, license_plate_key, vehicle_type, parking_spot_id, check_in_time, status)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                RETURNING *, (SELECT code FROM parking_spots WHERE id = parking_spot_id) AS spot_code
                """, SESSION_MAPPER, licensePlate, licensePlateKey, vehicleType.name(), spotId, checkInTime);
    }

    @Override
    public boolean complete(Long sessionId, OffsetDateTime checkOutTime) {
        return jdbc.update("""
                UPDATE parking_sessions SET status = 'COMPLETED', check_out_time = ?
                WHERE id = ? AND status = 'ACTIVE' AND check_out_time IS NULL
                """, checkOutTime, sessionId) == 1;
    }

    private void lockLicensePlate(String key) {
        // A row lock cannot protect a plate that has no session yet. The transaction
        // advisory lock serializes ENTRY/EXIT for that key, including across JVMs.
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                (rs, row) -> Boolean.TRUE, "parking:plate:" + key);
    }
}
