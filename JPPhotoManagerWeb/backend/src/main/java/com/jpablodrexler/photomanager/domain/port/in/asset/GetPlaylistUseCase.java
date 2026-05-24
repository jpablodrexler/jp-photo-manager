package com.jpablodrexler.photomanager.domain.port.in.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;

import java.util.List;

public interface GetPlaylistUseCase {

    List<Asset> execute(Long assetId);
}
