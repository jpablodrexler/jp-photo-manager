package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.domain.model.Asset;

import java.util.List;

public interface FindDuplicatedAssetsService {

    List<List<Asset>> getDuplicatedAssets();
}
