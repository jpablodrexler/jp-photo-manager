package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;

import java.util.List;

public interface ConvertConfigRepository {

    List<ConvertDirectoriesDefinition> findAllOrderByOrder();

    void saveAll(List<ConvertDirectoriesDefinition> definitions);

    void deleteAll();
}
