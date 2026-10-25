package com.ktpm.parking.infrastructure.persistence;

import com.ktpm.parking.business.support.LicensePlateNormalizer;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class LicensePlateKeyMigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private String schema;

    @BeforeEach
    void migrateLegacySchema() {
        schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Properties properties = new Properties();
        properties.setProperty("currentSchema", schema);
        dataSource.setConnectionProperties(properties);
        flyway("2").migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void upgradesAnEmptyDatabaseAndPreservesAllSeededSpots() {
        Flyway latest = flyway("3");
        latest.migrate();
        latest.validate();
        assertThat(latest.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parking_spots", Integer.class)).isEqualTo(30);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes WHERE schemaname = ?
                  AND indexname IN ('uq_parking_sessions_active_license_plate_key', 'uq_parking_sessions_active_spot')
                """, Integer.class, schema)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes WHERE schemaname = ?
                  AND indexname = 'uq_parking_sessions_active_license_plate'
                """, Integer.class, schema)).isZero();
    }

    @Test
    void backfillsHistoricalSessionsUsingExactlyTheBusinessWhitespaceRules() {
        // Include every Java separator and ASCII whitespace, spread across valid legacy rows.
        String[] plates = {" 29a-12345 ", "29a\t12345", "29a\n12345", "29a\u000B12345", "29a\f12345",
                "29a\r12345", "29a\u00A012345", "29a\u168012345", "29a\u200012345", "29a\u200112345",
                "29a\u200212345", "29a\u200312345", "29a\u200412345", "29a\u200512345", "29a\u200612345",
                "29a\u200712345", "29a\u200812345", "29a\u200912345", "29a\u200A12345", "29a\u202812345",
                "29a\u202912345", "29a\u202F12345", "29a\u205F12345", "29a\u300012345"};
        for (String plate : plates) {
            insertLegacy(plate, 1L, false);
        }
        insertLegacy("29A-12345", 2L, true);
        flyway("3").migrate();

        var rows = jdbc.queryForList("SELECT license_plate, license_plate_key FROM parking_sessions");
        assertThat(rows).hasSize(plates.length + 1);
        rows.forEach(row -> assertThat(row.get("license_plate_key"))
                .isEqualTo(LicensePlateNormalizer.toKey((String) row.get("license_plate"))));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parking_sessions WHERE status = 'ACTIVE'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void conflictingLegacyActiveKeysFailAtomicallyWithoutDeletingOrCompletingSessions() {
        insertLegacy("29A-12345", 1L, true);
        insertLegacy("29a 12345", 2L, true);

        assertThatThrownBy(() -> flyway("3").migrate()).isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("uq_parking_sessions_active_license_plate_key");
        assertThat(flyway("2").info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(jdbc.queryForList("SELECT license_plate FROM parking_sessions ORDER BY id", String.class))
                .containsExactly("29A-12345", "29a 12345");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parking_sessions WHERE status = 'ACTIVE'", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = ? AND table_name = 'parking_sessions' AND column_name = 'license_plate_key'
                """, Integer.class, schema)).isZero();
    }

    @Test
    void invalidHistoricalKeysStopMigrationAndPreserveTheOriginalData() {
        insertLegacy("----", 1L, false);
        assertThatThrownBy(() -> flyway("3").migrate()).isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("ck_parking_sessions_license_plate_key");
        assertThat(jdbc.queryForObject("SELECT license_plate FROM parking_sessions", String.class)).isEqualTo("----");
        assertThat(flyway("2").info().current().getVersion().getVersion()).isEqualTo("2");
    }

    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").target(target).load();
    }

    private void insertLegacy(String plate, Long spotId, boolean active) {
        jdbc.update("""
                INSERT INTO parking_sessions
                    (license_plate, vehicle_type, parking_spot_id, check_in_time, check_out_time, status)
                VALUES (?, 'MOTORBIKE', ?, '2026-10-24T08:00:00Z',
                        CASE WHEN ? THEN NULL ELSE TIMESTAMPTZ '2026-10-24T09:00:00Z' END, ?)
                """, plate, spotId, active, active ? "ACTIVE" : "COMPLETED");
    }
}
