package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumFilterJsonConverterTest {

    private final AlbumFilterJsonConverter sut = new AlbumFilterJsonConverter(new ObjectMapper());

    @Mock
    ObjectMapper failingObjectMapper;

    @Test
    void deserializeFilterJson_nullInput_returnsNull() {
        assertThat(sut.deserializeFilterJson(null)).isNull();
    }

    @Test
    void deserializeFilterJson_validJson_returnsAlbumFilterJson() {
        AlbumFilterJson result = sut.deserializeFilterJson("{\"search\":\"sunset\",\"minRating\":4}");

        assertThat(result.search()).isEqualTo("sunset");
        assertThat(result.minRating()).isEqualTo(4);
    }

    @Test
    void deserializeFilterJson_invalidJson_throwsRuntimeException() {
        assertThatThrownBy(() -> sut.deserializeFilterJson("{not valid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize filterJson");
    }

    @Test
    void serializeFilterJson_nullInput_returnsNull() {
        assertThat(sut.serializeFilterJson(null)).isNull();
    }

    @Test
    void serializeFilterJson_validObject_returnsJsonString() {
        AlbumFilterJson filterJson = new AlbumFilterJson("sunset", "2024-01-01", "2024-12-31", 4);

        String result = sut.serializeFilterJson(filterJson);

        assertThat(result).contains("\"search\":\"sunset\"").contains("\"minRating\":4");
    }

    @Test
    void serializeFilterJson_jsonProcessingException_throwsRuntimeException() throws JsonProcessingException {
        AlbumFilterJsonConverter throwingSut = new AlbumFilterJsonConverter(failingObjectMapper);
        AlbumFilterJson filterJson = new AlbumFilterJson("sunset", null, null, null);
        JsonProcessingException cause = new JsonProcessingException("boom") {
        };
        when(failingObjectMapper.writeValueAsString(any())).thenThrow(cause);

        assertThatThrownBy(() -> throwingSut.serializeFilterJson(filterJson))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to serialize filterJson")
                .hasCause(cause);
    }
}
