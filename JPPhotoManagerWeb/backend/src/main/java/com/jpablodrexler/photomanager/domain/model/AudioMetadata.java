package com.jpablodrexler.photomanager.domain.model;

public record AudioMetadata(
        String title,
        String artist,
        String album,
        Integer durationSeconds,
        Integer bitrateKbps,
        Integer sampleRateHz
) {}
