package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.AlbumFilterJson;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AlbumFilterJsonDeserializer {

    private final ObjectMapper objectMapper;

    @Named("deserializeFilterJson")
    AlbumFilterJson deserializeFilterJson(String filterJson) {
        if (filterJson == null) return null;
        try {
            return objectMapper.readValue(filterJson, AlbumFilterJson.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize filterJson", e);
        }
    }
}
