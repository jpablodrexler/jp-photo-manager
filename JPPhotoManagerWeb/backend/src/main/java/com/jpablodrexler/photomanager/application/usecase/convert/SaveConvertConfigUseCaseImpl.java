package com.jpablodrexler.photomanager.application.usecase.convert;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.convert.SaveConvertConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.out.ConvertConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SaveConvertConfigUseCaseImpl implements SaveConvertConfigUseCase {

    private final ConvertConfigRepository convertConfigRepository;

    @Override
    @Transactional
    public void execute(List<ConvertDirectoriesDefinition> definitions) {
        convertConfigRepository.deleteAll();
        for (int i = 0; i < definitions.size(); i++) {
            definitions.get(i).setId(null);
            definitions.get(i).setOrder(i);
        }
        convertConfigRepository.saveAll(definitions);
    }
}
