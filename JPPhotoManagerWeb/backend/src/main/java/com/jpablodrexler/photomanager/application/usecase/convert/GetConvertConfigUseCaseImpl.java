package com.jpablodrexler.photomanager.application.usecase.convert;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.in.convert.GetConvertConfigUseCase;
import com.jpablodrexler.photomanager.domain.port.out.ConvertConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetConvertConfigUseCaseImpl implements GetConvertConfigUseCase {

    private final ConvertConfigRepository convertConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ConvertDirectoriesDefinition> execute() {
        return convertConfigRepository.findAllOrderByOrder();
    }
}
