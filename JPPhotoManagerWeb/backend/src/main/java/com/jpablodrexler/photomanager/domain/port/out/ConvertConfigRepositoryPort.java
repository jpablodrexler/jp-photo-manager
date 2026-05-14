package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;

import java.util.List;

public interface ConvertConfigRepositoryPort {
    List<ConvertDirectoriesDefinition> findAllOrderByOrder();
    void replaceAll(List<ConvertDirectoriesDefinition> definitions);
}
