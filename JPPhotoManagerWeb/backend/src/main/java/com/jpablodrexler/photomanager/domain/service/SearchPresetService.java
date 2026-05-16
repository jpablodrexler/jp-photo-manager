package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.application.dto.FilterPreset;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;

import java.util.List;
import java.util.UUID;

public interface SearchPresetService {

    List<SearchPreset> listPresets(UUID userId);

    SearchPreset createPreset(UUID userId, String name, FilterPreset filter);

    void deletePreset(UUID userId, Long presetId);
}
