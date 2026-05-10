package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.Asset;
import com.jpablodrexler.photomanager.domain.entity.Folder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AssetRepositoryCustom {

    Page<Asset> findByFolderWithFilters(Folder folder, String search, LocalDateTime dateFrom,
                                        LocalDateTime dateTo, Integer minRating, Pageable pageable);
}
