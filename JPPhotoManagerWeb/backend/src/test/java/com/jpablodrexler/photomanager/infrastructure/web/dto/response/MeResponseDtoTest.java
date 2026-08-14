package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeResponseDtoTest {

    @Test
    void construct_exposesFieldsViaAccessors() {
        MeResponseDto dto = new MeResponseDto("alice", "ADMIN");

        assertThat(dto.username()).isEqualTo("alice");
        assertThat(dto.role()).isEqualTo("ADMIN");
    }
}
