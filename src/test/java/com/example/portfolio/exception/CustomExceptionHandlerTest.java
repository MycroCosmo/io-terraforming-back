package com.example.portfolio.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CustomExceptionHandlerTest {

    private final CustomExceptionHandler handler = new CustomExceptionHandler();

    @Test
    void hidesInternalExceptionDetailsFromTheResponse() {
        var response = handler.handleAllException(new RuntimeException("database-host:3306"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Request could not be completed");
        assertThat(response.getBody().getDetail()).doesNotContain("database-host");
    }

    @Test
    void keepsClientSafeDetailsForNotFoundErrors() {
        var exception = new CustomException(
                HttpStatus.NOT_FOUND, ErrorCode.NOT_FIND_PROJECT, "Project not found with id: 10"
        );

        var response = handler.handleCustomException(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Project not found with id: 10");
    }
}
