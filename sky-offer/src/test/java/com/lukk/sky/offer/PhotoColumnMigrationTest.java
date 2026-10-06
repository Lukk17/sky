package com.lukk.sky.offer;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("V3 and V4: the photo column split and what it does to rows written before it")
class PhotoColumnMigrationTest {

    private static final UUID ADOPTABLE_KEY_OFFER = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    private static final UUID ARBITRARY_STRING_OFFER = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000002");
    private static final UUID EXTERNAL_URL_OFFER = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000003");
    private static final UUID FOREIGN_KEY_OFFER = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000004");
    private static final UUID LEGACY_KEY_OFFER = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000005");

    private static final String ADOPTABLE_KEY =
            "offers/" + ADOPTABLE_KEY_OFFER + "/0f1e2d3c-4b5a-4968-8776-554433221100-hotel.png";
    private static final String FOREIGN_KEY =
            "offers/" + ADOPTABLE_KEY_OFFER + "/0f1e2d3c-4b5a-4968-8776-554433221100-victim.png";
    private static final String LEGACY_KEY = "offers/0f1e2d3c-4b5a-4968-8776-554433221100-hotel.png";
    private static final String ARBITRARY_STRING = "/images/grand-hotel-paris-new.jpg";
    private static final String EXTERNAL_URL = "https://images.example.com/test-hotel.jpeg";

    private static PostgreSQLContainer postgres;

    @BeforeAll
    static void migrateOverRowsWrittenBeforeTheSplit() throws SQLException {
        postgres = new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE);
        postgres.start();

        flyway("2").migrate();
        insertRowsAsV2WroteThem();
        flyway("4").migrate();
    }

    @AfterAll
    static void stopContainer() {
        postgres.stop();
    }

    @Test
    @DisplayName("A key inside the offer's own prefix is adopted as the server-owned object key")
    void migration_whenPhotoPathIsAKeyInTheOffersOwnPrefix_thenItBecomesThePhotoObjectKey() throws SQLException {
        assertEquals(ADOPTABLE_KEY, photoObjectKeyOf(ADOPTABLE_KEY_OFFER));
        assertNull(externalPhotoUrlOf(ADOPTABLE_KEY_OFFER));
    }

    @Test
    @DisplayName("An arbitrary string a client typed is dropped rather than carried into either column")
    void migration_whenPhotoPathIsAnArbitraryString_thenBothPhotoColumnsEndUpNull() throws SQLException {
        assertNull(photoObjectKeyOf(ARBITRARY_STRING_OFFER));
        assertNull(externalPhotoUrlOf(ARBITRARY_STRING_OFFER));
    }

    @Test
    @DisplayName("An absolute URL survives as the external photo address")
    void migration_whenPhotoPathIsAnAbsoluteUrl_thenItSurvivesAsTheExternalAddress() throws SQLException {
        assertNull(photoObjectKeyOf(EXTERNAL_URL_OFFER));
        assertEquals(EXTERNAL_URL, externalPhotoUrlOf(EXTERNAL_URL_OFFER));
    }

    @Test
    @DisplayName("A key under another offer's prefix is never adopted, because the storage guard would refuse it")
    void migration_whenPhotoPathIsAnotherOffersKey_thenItIsNotAdopted() throws SQLException {
        assertNull(photoObjectKeyOf(FOREIGN_KEY_OFFER));
        assertNull(externalPhotoUrlOf(FOREIGN_KEY_OFFER));
    }

    @Test
    @DisplayName("A key from before the per-offer prefix is never adopted, because the storage guard would refuse it")
    void migration_whenPhotoPathIsAKeyWithoutTheOfferSegment_thenItIsNotAdopted() throws SQLException {
        assertNull(photoObjectKeyOf(LEGACY_KEY_OFFER));
        assertNull(externalPhotoUrlOf(LEGACY_KEY_OFFER));
    }

    @Test
    @DisplayName("The photo_path column is gone, so nothing can read or write it again")
    void migration_whenApplied_thenThePhotoPathColumnNoLongerExists() throws SQLException {
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery(
                     "select column_name from information_schema.columns where table_name = 'offer'")) {

            StringBuilder names = new StringBuilder();
            while (columns.next()) {
                names.append(columns.getString(1)).append(' ');
            }

            assertTrue(names.indexOf("photo_object_key") >= 0, names.toString());
            assertTrue(names.indexOf("external_photo_url") >= 0, names.toString());
            assertEquals(-1, names.indexOf("photo_path"), names.toString());
        }
    }

    private static Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .table("flyway_schema_history_offer")
                .target(target)
                .load();
    }

    private static void insertRowsAsV2WroteThem() throws SQLException {
        try (Connection connection = connect()) {
            insertOffer(connection, ADOPTABLE_KEY_OFFER, ADOPTABLE_KEY);
            insertOffer(connection, ARBITRARY_STRING_OFFER, ARBITRARY_STRING);
            insertOffer(connection, EXTERNAL_URL_OFFER, EXTERNAL_URL);
            insertOffer(connection, FOREIGN_KEY_OFFER, FOREIGN_KEY);
            insertOffer(connection, LEGACY_KEY_OFFER, LEGACY_KEY);
        }
    }

    private static void insertOffer(Connection connection, UUID id, String photoPath) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                insert into offer (id, hotel_name, price, owner_email, room_capacity, city, country, photo_path)
                values (?, 'testHotelName', 20.00, 'test@owner.com', 2, 'testCity', 'testCountry', ?)
                """)) {

            insert.setObject(1, id);
            insert.setString(2, photoPath);
            insert.executeUpdate();
        }
    }

    private static String photoObjectKeyOf(UUID offerId) throws SQLException {
        return columnOf("photo_object_key", offerId);
    }

    private static String externalPhotoUrlOf(UUID offerId) throws SQLException {
        return columnOf("external_photo_url", offerId);
    }

    private static String columnOf(String column, UUID offerId) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement select = connection.prepareStatement(
                     "select " + column + " from offer where id = ?")) {

            select.setObject(1, offerId);

            try (ResultSet row = select.executeQuery()) {
                assertTrue(row.next(), "no offer row for " + offerId);

                return row.getString(1);
            }
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
