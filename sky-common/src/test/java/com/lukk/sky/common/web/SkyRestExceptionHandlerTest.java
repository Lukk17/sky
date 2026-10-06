package com.lukk.sky.common.web;

import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SkyRestExceptionHandler")
class SkyRestExceptionHandlerTest {

    private final SkyRestExceptionHandler handler = new SkyRestExceptionHandler();

    @Test
    @DisplayName("handleMethodArgumentNotValid_returnsProblemDetailWithFieldErrors_whenValidationFails")
    @SuppressWarnings("unchecked")
    void handleMethodArgumentNotValid_returnsProblemDetailWithFieldErrors_whenValidationFails() throws Exception {
        // given
        MethodArgumentNotValidException exception = validationFailureOn("receiverEmail", "must not be blank");

        // when
        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                exception,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                new ServletWebRequest(new MockHttpServletRequest()));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);

        ProblemDetail body = (ProblemDetail) response.getBody();

        assertThat(body.getDetail())
                .as("the problem detail must state that validation failed")
                .isEqualTo("Validation failed");
        assertThat(body.getProperties())
                .as("the field-errors property is part of the published error contract")
                .containsKey(SkyRestExceptionHandler.FIELD_ERRORS_PROPERTY);
        assertThat((Map<String, String>) body.getProperties().get(SkyRestExceptionHandler.FIELD_ERRORS_PROPERTY))
                .as("each rejected field must be reported with its constraint message")
                .containsEntry("receiverEmail", "must not be blank");
    }

    private MethodArgumentNotValidException validationFailureOn(String field, String message) throws Exception {
        Payload payload = new Payload();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(payload, "payload");
        bindingResult.rejectValue(field, "NotBlank", message);

        Method method = Controller.class.getDeclaredMethod("handle", Payload.class);

        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    @SuppressWarnings("unused")
    private static final class Payload {

        @NotBlank
        private String receiverEmail;

        public String getReceiverEmail() {
            return receiverEmail;
        }

        public void setReceiverEmail(String receiverEmail) {
            this.receiverEmail = receiverEmail;
        }
    }

    @SuppressWarnings("unused")
    private static final class Controller {

        void handle(Payload payload) {
        }
    }
}
