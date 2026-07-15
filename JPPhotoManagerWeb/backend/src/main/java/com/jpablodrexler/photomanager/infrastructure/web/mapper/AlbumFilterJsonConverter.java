package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlbumFilterJsonConverter {

    private final ObjectMapper objectMapper;

    @Named("deserializeFilterJson")
    public AlbumFilterJson deserializeFilterJson(String filterJson) {
        if (filterJson == null) return null;
        try {
            return objectMapper.readValue(filterJson, AlbumFilterJson.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize filterJson", e);
        }
    }

    public String serializeFilterJson(AlbumFilterJson filterJson) {
        if (filterJson == null) return null;
        try {
            return objectMapper.writeValueAsString(filterJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize filterJson", e);
        }
    }
}
