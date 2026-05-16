package com.jpablodrexler.photomanager.domain.port.in.convert;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import java.util.List;

public interface GetConvertConfigUseCase {
    List<ConvertDirectoriesDefinition> execute();
}
