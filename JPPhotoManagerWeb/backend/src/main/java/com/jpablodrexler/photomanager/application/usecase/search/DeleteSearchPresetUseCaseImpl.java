package com.jpablodrexler.photomanager.application.usecase.search;

import com.jpablodrexler.photomanager.application.exception.SearchPresetNotFoundException;
import com.jpablodrexler.photomanager.domain.port.in.search.DeleteSearchPresetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.SearchPresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSearchPresetUseCaseImpl implements DeleteSearchPresetUseCase {

    private final SearchPresetRepository searchPresetRepository;

    @Override
    @Transactional
    public void execute(Long presetId, UUID userId) {
        searchPresetRepository.findByIdAndUserId(presetId, userId)
                .orElseThrow(() -> new SearchPresetNotFoundException(presetId));
        searchPresetRepository.deleteById(presetId);
    }
}
