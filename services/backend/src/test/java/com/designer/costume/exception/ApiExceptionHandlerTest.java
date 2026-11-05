package com.designer.costume.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

    @Test
    void databaseConstraintFailureDoesNotExposeDatabaseDetails() {
        var response = exceptionHandler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("secret password; SELECT * FROM users"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Request conflicts with existing data");
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void malformedRequestBodyUsesCommonBadRequestEnvelope() {
        var response = exceptionHandler.handleUnreadableBody(
                new HttpMessageNotReadableException("parser internals", (org.springframework.http.HttpInputMessage) null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Request body is invalid");
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void unexpectedErrorUsesGenericMessage() {
        var response = exceptionHandler.handleUnexpected(new IllegalStateException("private implementation detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
