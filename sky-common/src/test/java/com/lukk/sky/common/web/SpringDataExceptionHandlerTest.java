package com.lukk.sky.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SpringDataExceptionHandler")
class SpringDataExceptionHandlerTest {

    private static final String NUL_BYTE_MESSAGE =
            "ERROR: invalid byte sequence for encoding \"UTF8\": 0x00\n  Where: unnamed portal parameter $4";

    private final SpringDataExceptionHandler handler = new SpringDataExceptionHandler();

    @Test
    @DisplayName("handleUnknownSortProperty_returns400NamingTheProperty_whenTheNameIsSafeToEcho")
    void handleUnknownSortProperty_returns400NamingTheProperty_whenTheNameIsSafeToEcho() {
        // given
        PropertyReferenceException exception = unknownProperty("bookedDatee");

        // when
        ErrorResponse response = handler.handleUnknownSortProperty(exception);

        // then
        ProblemDetail body = response.getBody();

        assertThat(body.getStatus())
                .as("an unknown sort property is a client error, not a server failure")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getDetail())
                .as("the caller has to be told which property was rejected")
                .contains("bookedDatee");
    }

    @Test
    @DisplayName("handleUnknownSortProperty_doesNotEchoTheProperty_whenItCarriesUnsafeCharacters")
    void handleUnknownSortProperty_doesNotEchoTheProperty_whenItCarriesUnsafeCharacters() {
        // given
        PropertyReferenceException exception = unknownProperty("<script>alert(1)</script>");

        // when
        ErrorResponse response = handler.handleUnknownSortProperty(exception);

        // then
        ProblemDetail body = response.getBody();

        assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getDetail())
                .as("attacker-supplied markup must never be reflected into the response body")
                .doesNotContain("<script>")
                .isEqualTo(SpringDataExceptionHandler.UNNAMEABLE_PROPERTY_DETAIL);
    }

    @Test
    @DisplayName("handleUnknownSortProperty_doesNotLeakTheEntityTypeName")
    void handleUnknownSortProperty_doesNotLeakTheEntityTypeName() {
        // given
        PropertyReferenceException exception = unknownProperty("nope");

        // when
        ErrorResponse response = handler.handleUnknownSortProperty(exception);

        // then
        assertThat(response.getBody().getDetail())
                .as("the persistence type name is internal detail and stays out of the response")
                .doesNotContain(SortableFixture.class.getSimpleName());
    }

    @Test
    @DisplayName("handleUnstorableValue_returns400NamingTheNulCharacter_whenTheDatabaseCannotEncodeIt")
    void handleUnstorableValue_returns400NamingTheNulCharacter_whenTheDatabaseCannotEncodeIt() {
        // given
        DataIntegrityViolationException exception = databaseRejected(NUL_BYTE_MESSAGE, "22021");

        // when
        ErrorResponse response = handler.handleUnstorableValue(exception);

        // then
        ProblemDetail body = response.getBody();

        assertThat(body.getStatus())
                .as("a character the caller sent is a client error, not a server failure")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getDetail())
                .as("the caller cannot fix the payload unless the response names the character")
                .isEqualTo(SpringDataExceptionHandler.UNENCODABLE_CHARACTER_DETAIL)
                .contains("NUL")
                .contains("U+0000");
    }

    @Test
    @DisplayName("handleUnstorableValue_echoesNeitherTheStatementNorTheOffendingValue")
    void handleUnstorableValue_echoesNeitherTheStatementNorTheOffendingValue() {
        // given
        DataIntegrityViolationException exception = databaseRejected(
                "could not execute statement [" + NUL_BYTE_MESSAGE + "] [insert into offer (city,comment) "
                        + "values (?,?)]; rejected value <script>alert(1)</script>",
                "22021");

        // when
        ErrorResponse response = handler.handleUnstorableValue(exception);

        // then
        assertThat(response.getBody().getDetail())
                .as("neither the statement nor attacker-supplied markup may reach the response body")
                .doesNotContain("insert into")
                .doesNotContain("<script>")
                .doesNotContain("0x00");
    }

    @Test
    @DisplayName("handleUnstorableValue_returns400_whenANumericValueOverflowsItsColumn")
    void handleUnstorableValue_returns400_whenANumericValueOverflowsItsColumn() {
        // given
        DataIntegrityViolationException exception = databaseRejected("ERROR: numeric field overflow", "22003");

        // when
        ErrorResponse response = handler.handleUnstorableValue(exception);

        // then
        ProblemDetail body = response.getBody();

        assertThat(body.getStatus())
                .as("a price carrying more digits than the column holds is a client error")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getDetail()).isEqualTo(SpringDataExceptionHandler.NUMERIC_OUT_OF_RANGE_DETAIL);
    }

    @Test
    @DisplayName("handleUnstorableValue_returns400_whenATextValueIsLongerThanItsColumn")
    void handleUnstorableValue_returns400_whenATextValueIsLongerThanItsColumn() {
        // given
        DataIntegrityViolationException exception = databaseRejected(
                "ERROR: value too long for type character varying(255)", "22001");

        // when
        ErrorResponse response = handler.handleUnstorableValue(exception);

        // then
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getDetail()).isEqualTo(SpringDataExceptionHandler.VALUE_TOO_LONG_DETAIL);
    }

    @Test
    @DisplayName("handleUnstorableValue_rethrowsUnchanged_whenAUniqueConstraintIsViolated")
    void handleUnstorableValue_rethrowsUnchanged_whenAUniqueConstraintIsViolated() {
        // given
        DataIntegrityViolationException exception = databaseRejected(
                "ERROR: duplicate key value violates unique constraint \"offer_pkey\"", "23505");

        // when
        // then
        assertThatThrownBy(() -> handler.handleUnstorableValue(exception))
                .as("a duplicate key is not a bad character, so it keeps its status instead of becoming a 400")
                .isSameAs(exception);
    }

    @Test
    @DisplayName("handleUnstorableValue_rethrowsUnchanged_whenANotNullConstraintIsViolated")
    void handleUnstorableValue_rethrowsUnchanged_whenANotNullConstraintIsViolated() {
        // given
        DataIntegrityViolationException exception = databaseRejected(
                "ERROR: null value in column \"owner_email\" violates not-null constraint", "23502");

        // when
        // then
        assertThatThrownBy(() -> handler.handleUnstorableValue(exception))
                .as("a null the service itself failed to supply is a server fault and must not hide behind a 400")
                .isSameAs(exception);
    }

    @Test
    @DisplayName("handleUnstorableValue_rethrowsUnchanged_whenAForeignKeyConstraintIsViolated")
    void handleUnstorableValue_rethrowsUnchanged_whenAForeignKeyConstraintIsViolated() {
        // given
        DataIntegrityViolationException exception = databaseRejected(
                "ERROR: insert or update on table \"booking_event\" violates foreign key constraint", "23503");

        // when
        // then
        assertThatThrownBy(() -> handler.handleUnstorableValue(exception))
                .as("a broken reference is not a bad character and keeps its current status")
                .isSameAs(exception);
    }

    @Test
    @DisplayName("handleUnstorableValue_rethrowsUnchanged_whenNoSqlExceptionIsInTheCauseChain")
    void handleUnstorableValue_rethrowsUnchanged_whenNoSqlExceptionIsInTheCauseChain() {
        // given
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("translated without a SQL cause");

        // when
        // then
        assertThatThrownBy(() -> handler.handleUnstorableValue(exception))
                .as("with no SQL state there is nothing to classify, so the failure stays a server failure")
                .isSameAs(exception);
    }

    @Test
    @DisplayName("handleUnstorableValue_findsTheSqlState_whenItIsNestedBelowAnotherCause")
    void handleUnstorableValue_findsTheSqlState_whenItIsNestedBelowAnotherCause() {
        // given
        SQLException driverFailure = new SQLException(NUL_BYTE_MESSAGE, "22021");
        IllegalStateException persistenceLayer =
                new IllegalStateException("could not execute statement", driverFailure);
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("could not execute statement", persistenceLayer);

        // when
        ErrorResponse response = handler.handleUnstorableValue(exception);

        // then
        assertThat(response.getBody().getStatus())
                .as("the driver exception sits two levels down in the real Hibernate chain")
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @Timeout(value = 10, threadMode = ThreadMode.SEPARATE_THREAD)
    @DisplayName("handleUnstorableValue_rethrowsUnchanged_whenTheCauseChainPointsAtItself")
    void handleUnstorableValue_rethrowsUnchanged_whenTheCauseChainPointsAtItself() {
        // given
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("could not execute statement", new SelfCausingFailure());

        // when
        // then
        assertThatThrownBy(() -> handler.handleUnstorableValue(exception))
                .as("a cause chain that points at itself must end the walk instead of looping forever")
                .isSameAs(exception);
    }

    private static DataIntegrityViolationException databaseRejected(String message, String sqlState) {
        SQLException driverFailure = new SQLException(message, sqlState);

        return new DataIntegrityViolationException(message, driverFailure);
    }

    private static PropertyReferenceException unknownProperty(String propertyName) {
        return new PropertyReferenceException(
                propertyName, TypeInformation.of(SortableFixture.class), List.of());
    }

    private static final class SelfCausingFailure extends RuntimeException {

        private SelfCausingFailure() {
            super("driver failure whose cause is itself");
        }

        @Override
        public synchronized Throwable getCause() {
            return this;
        }
    }

    @SuppressWarnings("unused")
    private static final class SortableFixture {

        private String hotelName;

        public String getHotelName() {
            return hotelName;
        }
    }
}
