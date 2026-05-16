package com.jpablodrexler.photomanager.domain.port.in.convert;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import java.util.List;

public interface SaveConvertConfigUseCase {
    void execute(List<ConvertDirectoriesDefinition> definitions);
}
