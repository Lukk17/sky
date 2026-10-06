package com.lukk.sky.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SpringDataExceptionHandler over MVC")
class SpringDataExceptionHandlerWebTest {

    private final FailingController controller = new FailingController();

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new SpringDataExceptionHandler())
            .build();

    @Test
    @DisplayName("unstorableValue_answers400AsProblemJson")
    void unstorableValue_answers400AsProblemJson() throws Exception {
        controller.failWith(databaseRejected(
                "ERROR: invalid byte sequence for encoding \"UTF8\": 0x00", "22021"));

        mvc.perform(post("/offers"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value(SpringDataExceptionHandler.UNENCODABLE_CHARACTER_DETAIL));
    }

    @Test
    @DisplayName("constraintViolation_staysUnhandled_soItKeepsItsServerErrorStatus")
    void constraintViolation_staysUnhandled_soItKeepsItsServerErrorStatus() {
        DataIntegrityViolationException duplicateKey = databaseRejected(
                "ERROR: duplicate key value violates unique constraint \"offer_pkey\"", "23505");
        controller.failWith(duplicateKey);

        assertThatThrownBy(() -> mvc.perform(post("/offers")))
                .as("declining the exception has to leave it unresolved rather than answer an empty 200")
                .hasRootCauseInstanceOf(SQLException.class)
                .rootCause()
                .hasMessageContaining("unique constraint");
    }

    private static DataIntegrityViolationException databaseRejected(String message, String sqlState) {
        return new DataIntegrityViolationException(message, new SQLException(message, sqlState));
    }

    @RestController
    static final class FailingController {

        private RuntimeException failure = new IllegalStateException("not configured");

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        @PostMapping("/offers")
        String create() {
            throw failure;
        }
    }
}
