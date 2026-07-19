package com.jpablodrexler.photomanager.application.usecase.search;

import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.domain.port.in.search.GetSearchPresetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.SearchPresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSearchPresetsUseCaseImpl implements GetSearchPresetsUseCase {

    private final SearchPresetRepository searchPresetRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SearchPreset> execute(UUID userId) {
        return searchPresetRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
