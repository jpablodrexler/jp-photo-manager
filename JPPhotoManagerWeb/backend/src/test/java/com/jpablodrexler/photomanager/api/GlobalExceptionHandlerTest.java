package com.jpablodrexler.photomanager.api;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler sut = new GlobalExceptionHandler();

    @Test
    void handleEntityNotFound_returns404WithBody() {
        var ex = new EntityNotFoundException("asset not found");

        ResponseEntity<ErrorResponse> response = sut.handleEntityNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("asset not found");
        assertThat(body.timestamp()).isNotBlank();
    }

    @Test
    void handleIllegalArgument_returns400WithBody() {
        var ex = new IllegalArgumentException("invalid input");

        ResponseEntity<ErrorResponse> response = sut.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("invalid input");
        assertThat(body.timestamp()).isNotBlank();
    }

    @Test
    void handleGeneric_returns500WithBody() {
        var ex = new RuntimeException("unexpected failure");

        ResponseEntity<ErrorResponse> response = sut.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.message()).isEqualTo("An internal error occurred.");
        assertThat(body.timestamp()).isNotBlank();
    }
}
