package com.jpablodrexler.photomanager.domain.port.in.catalog;

import com.jpablodrexler.photomanager.domain.model.Asset;
import java.util.List;

public interface GetDuplicatedAssetsUseCase {
    List<List<Asset>> execute();
}
