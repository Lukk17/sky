package com.lukk.sky.booking.migration;

import com.lukk.sky.booking.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the production migrations plus the local-profile demo seed against a real PostgreSQL.
 */
@DisplayName("Demo seed migration tests")
@Testcontainers
class DemoSeedMigrationTest {

    private static final String DEMO_SEED_RESOURCE = "db/demo/R__demo_seed.sql";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE);

    @BeforeAll
    static void migrateWithDemoSeed() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/demo")
                .load()
                .migrate();
    }

    @Test
    @DisplayName("the demo seed inserts two bookings and one event for each of them")
    void migrate_whenDemoLocationIsIncluded_thenBookingsAndEventsAreSeeded() {
        assertThat(count("SELECT count(*) FROM booking")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM booking_event")).isEqualTo(2);
    }

    @Test
    @DisplayName("every seeded event joins to a booking the same file inserted")
    void migrate_whenDemoLocationIsIncluded_thenEveryEventJoinsToASeededBooking() {
        long joined = count("""
                SELECT count(*) FROM booking_event event
                JOIN booking seeded ON seeded.id = event.booking_id
                WHERE seeded.booking_user = 'user@sky.dev'
                """);

        assertThat(joined).isEqualTo(count("SELECT count(*) FROM booking_event"));
    }

    @Test
    @DisplayName("the seed payload carries the offer id of the booking it belongs to")
    void migrate_whenDemoLocationIsIncluded_thenEventPayloadMatchesItsBooking() {
        long matching = count("""
                SELECT count(*) FROM booking_event event
                JOIN booking seeded ON seeded.id = event.booking_id
                WHERE event.payload::json ->> 'offerId' = seeded.offer_id::text
                  AND event.payload::json ->> 'id' = seeded.id::text
                """);

        assertThat(matching).isEqualTo(count("SELECT count(*) FROM booking_event"));
    }

    @Test
    @DisplayName("re-running the seed inserts nothing because every insert is idempotent")
    void demoSeed_whenExecutedASecondTime_thenRowCountsAreUnchanged() {
        execute(readDemoSeed());

        assertThat(count("SELECT count(*) FROM booking")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM booking_event")).isEqualTo(2);
    }

    private static String readDemoSeed() {
        try (InputStream stream = DemoSeedMigrationTest.class.getClassLoader()
                .getResourceAsStream(DEMO_SEED_RESOURCE)) {

            if (stream == null) {
                throw new IllegalStateException(DEMO_SEED_RESOURCE + " is not on the classpath");
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + DEMO_SEED_RESOURCE, ex);
        }
    }

    private static long count(String sql) {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            resultSet.next();

            return resultSet.getLong(1);
        } catch (SQLException ex) {
            throw new IllegalStateException("Query failed: " + sql, ex);
        }
    }

    private static void execute(String sql) {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
        } catch (SQLException ex) {
            throw new IllegalStateException("Statement failed", ex);
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
