package com.jpablodrexler.photomanager.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginatedResultTest {

    @Test
    void totalPages_evenlyDivisibleTotal_returnsExactQuotient() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of(), 100L, 0, 50);

        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void totalPages_remainderPresent_roundsUp() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of(), 101L, 0, 50);

        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void totalPages_zeroPageSize_returnsZero() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of(), 10L, 0, 0);

        assertThat(result.totalPages()).isZero();
    }

    @Test
    void totalPages_zeroTotal_returnsZero() {
        PaginatedResult<String> result = new PaginatedResult<>(List.of(), 0L, 0, 50);

        assertThat(result.totalPages()).isZero();
    }
}
