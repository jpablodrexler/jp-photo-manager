package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.RecentTargetPath;

import java.util.List;

public interface RecentTargetPathRepository {

    List<RecentTargetPath> findAllOrderByIdDesc();

    boolean existsByPath(String path);

    RecentTargetPath save(RecentTargetPath recentTargetPath);

    void deleteById(Long id);

    void deleteAll(List<RecentTargetPath> paths);
}
